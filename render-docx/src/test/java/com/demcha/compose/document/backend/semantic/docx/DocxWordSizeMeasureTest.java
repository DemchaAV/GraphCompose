package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTInd;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A paragraph's lines are set in a measure as much wider or narrower than the page's as Word
 * sets its text.
 *
 * <p>Word states a type size in half points. {@code EngineeringResume}'s 7.8pt profile, set at
 * 8pt, took a line more than on the page, and its 6.9pt skills broke a word onto a line of its
 * own: each column stood 8 to 9pt low under them.</p>
 */
class DocxWordSizeMeasureTest {

    /** The body's width on a 400pt page with 20pt margins. */
    private static final double ROOM = 360;

    @Test
    void aSizeWordSetsLargerWidensTheMeasureByAsMuch() throws Exception {
        assertThat(indentOf(7.8, TextAlign.LEFT, SHORT))
                .as("one line: the measure 8/7.8 of the page's, in full")
                .isEqualTo(-Math.round(ROOM * (8 / 7.8 - 1) * 20));
    }

    @Test
    void aSizeWordSetsSmallerNarrowsAParagraphOfSeveralLines() throws Exception {
        assertThat(indentOf(9.2, TextAlign.LEFT, LONG)).as("9/9.2 of the page's, a point short of it")
                .isEqualTo(Math.round((ROOM * (1 - 9 / 9.2) + 1) * 20));
    }

    @Test
    void aParagraphOfOneLineIsNeverNarrowed() throws Exception {
        // It broke no word to keep out, and narrowed it could break onto two.
        assertThat(indentOf(9.2, TextAlign.LEFT, SHORT)).isZero();
    }

    @Test
    void aSizeWordStatesAsItIsLeavesTheMeasureAlone() throws Exception {
        assertThat(indentOf(9.5, TextAlign.LEFT, LONG)).isZero();
    }

    @Test
    void aLineOfOneAsWideAsItsBoxIsGivenRoomPastIt() throws Exception {
        // OrangeOps' headings fill their boxes; Word set them a little wider and broke them onto
        // a second line. A line of one breaks no word, so it is given a few hundredths more room.
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 30, page -> page
                .addSection(s -> s.fillColor(com.demcha.compose.document.style.DocumentColor.rgb(230, 240, 255))
                        .padding(com.demcha.compose.document.style.DocumentInsets.of(14))
                        .addParagraph(p -> p.text("Card title"))))) {
            XWPFParagraph heading = document.getTables().get(0).getRow(0).getCell(0).getParagraphs().get(0);

            assertThat(rightIndent(heading)).as("a little past the box, at the right").isNegative()
                    .isGreaterThan(-40);
        }
        assertThat(indentOf(9.5, TextAlign.LEFT, SHORT)).as("a line of one with room to spare").isZero();
    }

    @Test
    void aCentredLineIsLeftWhereThePageSetsIt() throws Exception {
        assertThat(indentOf(7.8, TextAlign.CENTER, LONG)).isZero();
    }

    @Test
    void aCentredOrRightAlignedLineOfItsOwnIsSpacedToThePagesWidth() throws Exception {
        // CenteredHeadline's 8.3pt contact line, set at 8.5, stood 10pt wider in Word, its first
        // letter 4pt left of the page's. The difference is spread over its letters.
        // In twentieths of a point, to the nearest: narrowed at 8.3pt (set at 8.5), widened at
        // 6.2pt (set at 6).
        int letters = SHORT.codePointCount(0, SHORT.length());
        for (double size : new double[] {8.3, 6.2}) {
            double asked = widthAlone(SHORT, size);
            double wordSize = Math.round(size * 2) / 2.0;
            int expected = (int) Math.round((asked - asked * wordSize / size) / letters * 20);
            assertThat(expected).as("spaced at %spt", size).isNotZero();
            for (TextAlign align : new TextAlign[] {TextAlign.CENTER, TextAlign.RIGHT}) {
                try (XWPFDocument document = DocxExports.withLayout(400, 400, 20, page -> page
                        .addParagraph(p -> p.text(SHORT).textStyle(DocumentTextStyle.DEFAULT.withSize(size)).align(align)))) {
                    assertThat(text(document, "Platform engineer").getRuns())
                            .as("each run spaced the page's width less Word's, a letter at a time, %s at %spt", align, size)
                            .allSatisfy(run -> assertThat(run.getCharacterSpacing()).isEqualTo(expected));
                }
            }
        }
    }

    @Test
    void aLineSetFromItsLeftATrackedLineAndANearOneAreNotSpaced() throws Exception {
        // From its left, the line's first letter is the page's. Word sets a tracked line's
        // tracking its own way and spaces after its last letter too, which the size alone does
        // not tell; under a point, the size alone mispredicted what Word set on the corpus.
        DocumentTextStyle tracked = DocumentTextStyle.DEFAULT.withSize(8.3)
                .withLetterSpacing(com.demcha.compose.document.style.DocumentLetterSpacing.points(1));
        try (XWPFDocument left = DocxExports.withLayout(400, 400, 20, page -> page
                     .addParagraph(p -> p.text(SHORT).textStyle(DocumentTextStyle.DEFAULT.withSize(8.3)).align(TextAlign.LEFT)));
             XWPFDocument spaced = DocxExports.withLayout(400, 400, 20, page -> page
                     .addParagraph(p -> p.text(SHORT).textStyle(tracked).align(TextAlign.CENTER)));
             XWPFDocument near = DocxExports.withLayout(400, 400, 20, page -> page
                     .addParagraph(p -> p.text("Platform engineer").textStyle(DocumentTextStyle.DEFAULT.withSize(8.4))
                             .align(TextAlign.CENTER)))) {
            assertThat(text(left, "Platform engineer").getRuns()).allSatisfy(run -> assertThat(run.getCharacterSpacing()).isZero());
            assertThat(text(spaced, "Platform engineer").getRuns()).allSatisfy(run -> assertThat(run.getCharacterSpacing()).isEqualTo(20));
            assertThat(text(near, "Platform engineer").getRuns()).allSatisfy(run -> assertThat(run.getCharacterSpacing()).isZero());
        }
    }

    @Test
    void aShortCentredLineIsLeftWhereThePageSetsIt() throws Exception {
        // Its box has room for it as Word sets it, so neither edge moves.
        try (XWPFDocument document = DocxExports.withLayout(400, 400, 20, page -> page
                .addParagraph(p -> p.text(SHORT).textStyle(DocumentTextStyle.DEFAULT.withSize(7.8))
                        .align(TextAlign.CENTER)))) {
            XWPFParagraph paragraph = text(document, "Platform engineer");

            assertThat(leftIndent(paragraph)).isZero();
            assertThat(rightIndent(paragraph)).isZero();
        }
    }

    @Test
    void aLineSpacedBackFromWiderIsGivenRoomForThePagesWidthOnly() throws Exception {
        // Word sets 8.3pt at 8.5, wider; spaced back to the page's width, the line needs room for
        // that and no more: in a cell, room the left edge cannot give comes off the right and
        // would move a right-aligned line right.
        double room = fillingRoom(HEADING, 8.3);
        try (XWPFDocument document = filledBy(HEADING, 8.3, TextAlign.RIGHT, room)) {
            XWPFParagraph heading = text(document, "INFORMATION");
            double measure = room - (leftIndent(heading) + rightIndent(heading)) / 20.0;

            double asked = widthAlone(HEADING, 8.3);
            assertThat(measure).as("the page's line and a few hundredths, not Word's wider one")
                    .isCloseTo(asked * 1.03, org.assertj.core.data.Offset.offset(0.2))
                    .isLessThan(asked * 8.5 / 8.3 * 1.03 - 1);
        }
    }

    @Test
    void aCentredLineWordSetsNarrowerIsGivenRoomForThePagesWidth() throws Exception {
        // Its letters are spaced back to the page's width, which needs the few hundredths of
        // room past it as much as a line Word sets wider: without them, 7.2pt set at 7 and spaced
        // back to its box broke a word onto a second line.
        double room = fillingRoom(HEADING, 7.2);
        try (XWPFDocument document = filledBy(HEADING, 7.2, TextAlign.CENTER, room)) {
            XWPFParagraph heading = text(document, "INFORMATION");
            double measure = room - (leftIndent(heading) + rightIndent(heading)) / 20.0;

            assertThat(measure).as("the page's line and a few hundredths more")
                    .isGreaterThan(widthAlone(HEADING, 7.2) * 1.02);
        }
    }

    @Test
    void aCentredLineThatFillsItsBoxIsGivenRoomAtBothEdgesAlike() throws Exception {
        // VioletGrid's 6.8pt "INFORMATION ARCHITECTURE" fills its tile; set at 7pt it broke in two.
        double room = fillingRoom(HEADING, 6.8);
        try (XWPFDocument document = filledBy(HEADING, 6.8, TextAlign.CENTER, room)) {
            XWPFParagraph heading = text(document, "INFORMATION");
            double measure = room - (leftIndent(heading) + rightIndent(heading)) / 20.0;

            assertThat(leftIndent(heading)).as("both edges move").isNegative();
            assertThat(Math.abs(leftIndent(heading) - rightIndent(heading))).as("alike, so its centre stays")
                    .isLessThanOrEqualTo(1);
            assertThat(measure).as("the line fits at Word's 7pt").isGreaterThan(widthAlone(HEADING, 6.8) * 7 / 6.8);
        }
    }

    @Test
    void aRightAlignedLineThatFillsItsBoxIsGivenRoomAtItsLeft() throws Exception {
        double room = fillingRoom(HEADING, 6.8);
        try (XWPFDocument document = filledBy(HEADING, 6.8, TextAlign.RIGHT, room)) {
            XWPFParagraph heading = text(document, "INFORMATION");

            assertThat(rightIndent(heading)).as("its edge stays").isZero();
            assertThat(room - leftIndent(heading) / 20.0).as("the line fits at Word's 7pt")
                    .isGreaterThan(widthAlone(HEADING, 6.8) * 7 / 6.8);
        }
    }

    @Test
    void aRightAlignedLineFillingACellTakesNoTextPastTheCellsLeftEdge() throws Exception {
        // Word draws no text left of a cell's edge: the room the left indent cannot give comes
        // from the right.
        double column = fillingRoom(HEADING, 6.8);
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 20, page -> page
                .addRow("Tile", row -> row
                        .columns(DocumentRowColumn.fixed(column), DocumentRowColumn.weight(1))
                        .addParagraph(p -> p.text(HEADING).textStyle(DocumentTextStyle.DEFAULT.withSize(6.8))
                                .align(TextAlign.RIGHT))
                        .addParagraph("Main")))) {
            var cell = document.getTables().get(0).getRow(0).getCell(0);
            XWPFParagraph heading = cell.getParagraphs().stream()
                    .filter(p -> p.getText().startsWith("INFORMATION")).findFirst().orElseThrow();
            long cellWidth = DocxTwips.of(cell.getCTTc().getTcPr().getTcW().getW());

            assertThat(leftIndent(heading)).as("nothing past the cell's left edge").isGreaterThanOrEqualTo(0);
            assertThat((cellWidth - leftIndent(heading) - rightIndent(heading)) / 20.0)
                    .as("the line still fits at Word's 7pt").isGreaterThan(widthAlone(HEADING, 6.8) * 7 / 6.8);
        }
    }

    @Test
    void aRightAlignedParagraphOfSeveralLinesIsLeftWhereThePageSetsIt() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 400, 20, page -> page
                .addParagraph(p -> p.text(LONG).textStyle(DocumentTextStyle.DEFAULT.withSize(7.8))
                        .align(TextAlign.RIGHT)))) {
            XWPFParagraph paragraph = text(document, "Platform engineer");

            assertThat(leftIndent(paragraph)).isZero();
            assertThat(rightIndent(paragraph)).isZero();
        }
    }

    @Test
    void aCellsLeftIndentGivesWhatItHasAndTheRightTheRest() throws Exception {
        // Padded 2.5pt, the paragraph keeps half a point of indent past the editor's slack.
        double column = fillingRoom(HEADING, 6.8) + 2.5;
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 20, page -> page
                .addRow("Tile", row -> row
                        .columns(DocumentRowColumn.fixed(column), DocumentRowColumn.weight(1))
                        .addParagraph(p -> p.text(HEADING).textStyle(DocumentTextStyle.DEFAULT.withSize(6.8))
                                .padding(new DocumentInsets(0, 0, 0, 2.5)).align(TextAlign.RIGHT))
                        .addParagraph("Main")))) {
            var cell = document.getTables().get(0).getRow(0).getCell(0);
            XWPFParagraph heading = cell.getParagraphs().stream()
                    .filter(p -> p.getText().startsWith("INFORMATION")).findFirst().orElseThrow();
            long cellWidth = DocxTwips.of(cell.getCTTc().getTcPr().getTcW().getW());

            assertThat(leftIndent(heading)).as("all the half point it had, and no more").isZero();
            assertThat(rightIndent(heading)).as("the rest from the right").isNegative();
            assertThat((cellWidth - leftIndent(heading) - rightIndent(heading)) / 20.0)
                    .as("the line still fits at Word's 7pt").isGreaterThan(widthAlone(HEADING, 6.8) * 7 / 6.8);
        }
    }

    @Test
    void aCentredLineOfAPictureAloneIsLeftAsItIs() throws Exception {
        // A picture is written at its size and takes the same room in Word.
        try (XWPFDocument document = DocxExports.withLayout(150.5 + 40, 300, 20, page -> page
                .addParagraph(p -> p.rich(rich -> rich.image(
                                com.demcha.compose.document.image.DocumentImageData.fromBytes(pngBytes()), 150, 20))
                        .align(TextAlign.CENTER)))) {
            XWPFParagraph picture = document.getParagraphs().get(0);

            assertThat(leftIndent(picture)).isZero();
            assertThat(rightIndent(picture)).isZero();
        }
    }

    @Test
    void theMeasureIsTheOneTheLineThatGrowsMostNeeds() throws Exception {
        // EngineeringResume's projects: a 7.35pt title, set at 7.5, fills most of the first
        // line, and 7.1pt prose, set at 7, the lines under it. Averaged over the paragraph's
        // letters the measure would narrow, and the title's line would no longer fit.
        String title = "GraphCompose (Java 21, PDFBox, Maven, JMH) - Declarative Java PDF layout";
        String prose = " engine. Semantic templates, snapshot testing, and the pipelines that run on them ".repeat(4);
        java.util.function.Consumer<com.demcha.compose.document.dsl.ParagraphBuilder> paragraph =
                p -> p.rich(rich -> rich.size(title, 7.35).size(prose, 7.1));
        double letters = (title.length() * 7.5 + prose.length() * 7.0) / (title.length() * 7.35 + prose.length() * 7.1);
        assertThat(letters).as("the letters' average narrows").isLessThan(1);
        for (double room = 300; room < 360; room += 0.25) {
            double first = wordsWidth(laidOut(room, paragraph).get(0));
            if (!(first > room * letters)) {
                continue;
            }
            // A measure where the title's line, as Word sets it, is wider than the averaged one.
            double width = room;
            try (XWPFDocument document = DocxExports.withLayout(room + 40, 400, 20, page -> page.addParagraph(paragraph))) {
                double measure = width - rightIndent(text(document, "GraphCompose")) / 20.0;

                assertThat(measure).as("the title's line fits, a point to spare").isGreaterThanOrEqualTo(first + 0.99);
            }
            return;
        }
        throw new AssertionError("no measure where the averaged share would cut the title's line");
    }

    @Test
    void aRunOfItsOwnSizeIsWrittenAtTheSizeTheMeasureIsTakenFor() throws Exception {
        // The body sets Normal at 12pt; the small print states its own size on its run.
        try (XWPFDocument document = DocxExports.withLayout(400, 400, 20, page -> page
                .addParagraph(p -> p.text("A body paragraph long enough to set the document's own size.")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(12)))
                .addParagraph(p -> p.text("Platform engineer with ten years of document pipelines.")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(7.8))))) {
            XWPFParagraph small = text(document, "Platform engineer");

            assertThat(DocxTwips.of(small.getRuns().get(0).getCTR().getRPr().getSzArray(0).getVal()))
                    .as("8pt, in half points").isEqualTo(16L);
            assertThat(rightIndent(small)).isEqualTo(-Math.round(ROOM * (8 / 7.8 - 1) * 20));
        }
    }

    @Test
    void aParagraphInACellIsGivenItsShareOfTheCellsMeasure() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 400, 20, page -> page
                .addRow("Page", row -> row
                        .columns(DocumentRowColumn.fixed(200), DocumentRowColumn.weight(1))
                        .addParagraph(p -> p.text("Platform engineer with ten years of document pipelines.")
                                .textStyle(DocumentTextStyle.DEFAULT.withSize(7.8)))
                        .addParagraph("Main")))) {
            XWPFParagraph cell = document.getTables().get(0).getRow(0).getCell(0).getParagraphs().stream()
                    .filter(p -> p.getText().startsWith("Platform engineer")).findFirst().orElseThrow();

            assertThat(-rightIndent(cell)).as("wider, by its share of a column no wider than 200pt")
                    .isPositive()
                    .isLessThanOrEqualTo(Math.round(200 * (8 / 7.8 - 1) * 20));
        }
    }

    @Test
    void aLineThePageBrokeJustShortOfItsNextWordStaysBroken() throws Exception {
        // CompactMono's "and" was 0.1pt from fitting on the page; given the measure in
        // proportion, Word fitted it. Find a measure as close, then hold Word's clear of it.
        String text = "Built backend services and production document rendering pipelines processing two "
                      + "million documents per month and cutting render latency from seconds to milliseconds.";
        for (double width = 300; width < 360; width += 0.05) {
            double room = width;
            List<ParagraphLine> lines = laidOut(room, text);
            double[] tie = lineAndNextWord(lines);
            if (tie == null || !(tie[1] - room > 0) || !(tie[1] - room < 0.2)) {
                continue;
            }
            try (XWPFDocument document = DocxExports.withLayout(room + 40, 400, 20, page -> page
                    .addParagraph(p -> p.text(text).textStyle(DocumentTextStyle.DEFAULT.withSize(7.95))))) {
                double measure = room - rightIndent(text(document, "Built backend")) / 20.0;
                double grows = 8 / 7.95;

                assertThat(rightIndent(text(document, "Built backend"))).as("wider than the page's").isNegative();
                assertThat(measure).as("every line fits").isGreaterThanOrEqualTo(tie[0] * grows);
                assertThat(measure).as("the next word does not, by a point")
                        .isLessThanOrEqualTo(tie[1] * grows - 0.99);
            }
            return;
        }
        throw new AssertionError("no measure within 0.2pt of a line and its next word");
    }

    @Test
    void everyLineHoldingAPictureStillFits() throws Exception {
        // A picture is written at its own size: it takes the room it takes on the page. Over a
        // run of measures one finds the picture's line all but full.
        String prose = "Built backend services and production document rendering pipelines that process "
                       + "two million documents a month for hiring, billing and reporting teams.";
        java.util.function.Consumer<com.demcha.compose.document.dsl.ParagraphBuilder> paragraph = p -> p.rich(rich -> rich
                .image(com.demcha.compose.document.image.DocumentImageData.fromBytes(pngBytes()), 24, 8)
                .size(" " + prose, 7.1));
        double tightest = Double.POSITIVE_INFINITY;
        for (double room = 300; room < 330; room += 0.25) {
            double widest = laidOut(room, paragraph).stream().mapToDouble(DocxWordSizeMeasureTest::wordsWidth).max().orElseThrow();
            double width = room;
            try (XWPFDocument document = DocxExports.withLayout(room + 40, 400, 20, page -> page.addParagraph(paragraph))) {
                double measure = width - rightIndent(text(document, "Built backend")) / 20.0;
                tightest = Math.min(tightest, measure - widest);
            }
        }
        assertThat(tightest).as("the least room any line, picture and all, is left in Word's measure")
                .isGreaterThanOrEqualTo(0.99);
    }

    /** A laid-out line's width as Word sets it: text at its half-point size, pictures as they are. */
    private static double wordsWidth(ParagraphLine line) {
        double width = 0;
        for (var span : line.spans()) {
            if (span instanceof com.demcha.compose.document.layout.payloads.ParagraphTextSpan text) {
                double size = text.textStyle().size();
                width += text.width() * Math.round(size * 2) / 2.0 / size;
            } else {
                width += span.width();
            }
        }
        return width;
    }

    private static byte[] pngBytes() {
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(24, 8, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    /**
     * The widest line, and the narrowest line with a space and its next line's first word, as
     * the page lays them out; a word's width is its own line's, laid out alone.
     */
    private static double[] lineAndNextWord(List<ParagraphLine> lines) {
        double widest = 0;
        double nearest = Double.POSITIVE_INFINITY;
        double space = widthAlone("a a") - 2 * widthAlone("a");
        for (int index = 0; index < lines.size(); index++) {
            widest = Math.max(widest, lines.get(index).width());
            if (index + 1 < lines.size()) {
                String next = lines.get(index + 1).text().strip().split("\\s+")[0];
                nearest = Math.min(nearest, lines.get(index).width() + space + widthAlone(next));
            }
        }
        return lines.size() < 2 ? null : new double[]{widest, nearest};
    }

    private static double widthAlone(String text) {
        return laidOut(500, text).get(0).width();
    }

    private static List<ParagraphLine> laidOut(double room, String text) {
        return laidOut(room, p -> p.text(text).textStyle(DocumentTextStyle.DEFAULT.withSize(7.95)));
    }

    private static List<ParagraphLine> laidOut(double room,
                                               java.util.function.Consumer<com.demcha.compose.document.dsl.ParagraphBuilder> paragraph) {
        try (DocumentSession session = GraphCompose.document().pageSize(room + 40, 400)
                .margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.addParagraph(paragraph));
            return session.layoutGraph().fragments().stream()
                    .filter(fragment -> fragment.payload() instanceof ParagraphFragmentPayload)
                    .flatMap(fragment -> ((ParagraphFragmentPayload) fragment.payload()).lines().stream())
                    .toList();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static long rightIndent(XWPFParagraph paragraph) {
        CTInd indent = paragraph.getCTP().getPPr() == null ? null : paragraph.getCTP().getPPr().getInd();
        return indent == null || !indent.isSetRight() ? 0 : DocxTwips.of(indent.getRight());
    }

    private static final String HEADING = "INFORMATION ARCHITECTURE";

    /** A measure the line fills at the page's size, with less than Word's size needs to spare. */
    private static double fillingRoom(String line, double size) {
        return widthAlone(line, size) + 0.5;
    }

    private static XWPFDocument filledBy(String line, double size, TextAlign align, double room) throws Exception {
        return DocxExports.withLayout(room + 40, 300, 20, page -> page
                .addParagraph(p -> p.text(line).textStyle(DocumentTextStyle.DEFAULT.withSize(size)).align(align)));
    }

    private static double widthAlone(String text, double size) {
        return laidOut(500, p -> p.text(text).textStyle(DocumentTextStyle.DEFAULT.withSize(size))).get(0).width();
    }

    private static long leftIndent(XWPFParagraph paragraph) {
        CTInd indent = paragraph.getCTP().getPPr() == null ? null : paragraph.getCTP().getPPr().getInd();
        return indent == null || !indent.isSetLeft() ? 0 : DocxTwips.of(indent.getLeft());
    }

    private static final String SHORT = "Platform engineer with ten years of document pipelines.";
    private static final String LONG = SHORT + " Built resilient layout engines, template systems and the "
            + "snapshot-tested libraries that replace brittle production scripts across many teams.";

    /** The right indent of a paragraph of the text given, at a size and an alignment. */
    private static long indentOf(double size, TextAlign align, String words) throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 400, 20, page -> page
                .addParagraph(p -> p.text(words).textStyle(DocumentTextStyle.DEFAULT.withSize(size)).align(align)))) {
            return rightIndent(text(document, "Platform engineer"));
        }
    }

    private static XWPFParagraph text(XWPFDocument document, String start) {
        return document.getParagraphs().stream()
                .filter(p -> p.getText().strip().startsWith(start))
                .findFirst().orElseThrow();
    }
}
