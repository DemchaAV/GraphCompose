package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Text stands on the page's baseline in Word's exact lines.
 *
 * <p>Word stands the baseline of an exact line four fifths of the way down it, whatever the
 * face; the page sets it the face's ascent below the line's top. {@code NorthlineProposal}'s
 * 46pt Spectral title, whose ascent is under seven tenths of its line, stood 7pt low in Word.</p>
 */
class DocxBaselineSeatTest {

    /** Spectral's ascent, in ems: 1059 of 1000 units. */
    private static final double SPECTRAL_ASCENT = 1.059;
    /** Spectral's own line, in ems: its ascent and 463 units of descent. */
    private static final double SPECTRAL_LINE = 1.522;
    /** Lato's ascent, in ems: 1974 of 2000 units. */
    private static final double LATO_ASCENT = 0.987;
    /** Poppins' ascent, in ems: 1050 of 1000 units. */
    private static final double POPPINS_ASCENT = 1.05;
    /** Gothic A1's ascent, in ems: 798 of 1000 units, its own line one em. */
    private static final double GOTHIC_A1_ASCENT = 0.798;

    private static DocumentTextStyle spectral(double size) {
        return DocumentTextStyle.builder().fontName(FontName.SPECTRAL).size(size).build();
    }

    @Test
    void aDeepFaceIsRaisedFromWordsBaselineToThePages() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Title").textStyle(spectral(30))))) {
            XWPFParagraph title = paragraph(document, "Title");
            double wordBelowThePage = 0.8 * line(title) - SPECTRAL_ASCENT * 30;

            assertThat(wordBelowThePage).as("the premise: Word's baseline is the lower").isGreaterThan(4);
            assertThat(position(title)).isCloseTo((int) Math.round(wordBelowThePage * 2), within(1));
        }
    }

    @Test
    void aFaceWhoseBaselineWordAlreadyNearlyMatchesIsLeftAlone() throws Exception {
        // Lato's ascent is 0.82 of its line: a quarter point from Word's four fifths at 10pt.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Body").textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.LATO).size(10).build())))) {
            assertThat(position(paragraph(document, "Body"))).isZero();
        }
    }

    @Test
    void aPictureOnTheBaselineMovesWithItsText() throws Exception {
        // An 8pt picture standing on the baseline of a Spectral line: Word stands both on its own
        // baseline, so the picture takes the text's move and nothing else.
        byte[] png = png();
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.inlineText("Title ", spectral(30))
                        .inlineImage(com.demcha.compose.document.image.DocumentImageData.fromBytes(png), 8, 8,
                                com.demcha.compose.document.node.InlineImageAlignment.BASELINE, 0, null)))) {
            XWPFParagraph title = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getText().startsWith("Title")).findFirst().orElseThrow();
            int text = positionOf(title.getRuns().get(0).getCTR());

            assertThat(text).as("the premise: the line moved").isPositive();
            assertThat(positionOf(title.getRuns().get(1).getCTR())).isEqualTo(text);
        }
    }

    private static byte[] png() {
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(8, 8,
                    java.awt.image.BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    @Test
    void theHalfOfALinePairSetLowerIsSeatedFromTheLinesTop() throws Exception {
        // A 12pt label centred beside a 30pt value: the line is the value's, and the label's own
        // line starts half the difference below its top.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .add(new ShapeContainerBuilder().name("Row").rectangle(360, 60)
                        .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                        .position(new ParagraphBuilder().name("Label").text("Label").textStyle(spectral(12)).build(),
                                0, 0, LayerAlign.CENTER_LEFT)
                        .position(new ParagraphBuilder().name("Value").text("Value").textStyle(spectral(30)).build(),
                                0, 0, LayerAlign.CENTER_RIGHT)
                        .build()))) {
            XWPFParagraph pair = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getText().contains("Value")).findFirst().orElseThrow();
            double line = line(pair);
            double labelLine = line * 12 / 30;
            double labelBelow = (line - labelLine) / 2;
            CTR label = pair.getRuns().stream().filter(run -> run.text().contains("Label")).findFirst()
                    .orElseThrow().getCTR();

            assertThat(pair.getText()).as("one line").contains("Label");
            assertThat(positionOf(label))
                    .isCloseTo((int) Math.round((0.8 * line - labelBelow - SPECTRAL_ASCENT * 12) * 2), within(1));
        }
    }

    @Test
    void linesThatTookTheirGapFromTheSpaceAboveAreSeatedFromTheHigherTop() throws Exception {
        // Two lines 10pt apart under 20pt of space: Word's lines take their gaps from above and
        // start 10pt above the page's first line, and are raised into the 10pt left above them.
        try (XWPFDocument document = DocxExports.withLayout(240, 600, 20, page -> page
                .addParagraph(p -> p.text("First line of it and second line of it").textStyle(spectral(20))
                        .lineSpacing(10).margin(new DocumentInsets(20, 0, 0, 0))))) {
            XWPFParagraph text = paragraph(document, "First line of it and second line of it");
            double line = line(text);
            double raise = 0.8 * line - 10 - SPECTRAL_ASCENT * 20;

            assertThat(line).as("the premise: two lines, each its gap taller").isGreaterThan(40);
            assertThat(raise).as("the premise: Word's baseline is the lower, by less than the space left")
                    .isBetween(0.5, 10.0);
            assertThat(before(text)).isCloseTo(Math.round((10 - raise) * 20), within(1L));
            assertThat(position(text)).isZero();
        }
    }

    @Test
    void aLineWordSetsLowIsRaisedIntoTheSpaceAboveItAndOwesItBelow() throws Exception {
        // 20pt above a 30pt Spectral title: Word's baseline stands nearly 5pt below the page's.
        // The line moves up that much of the space, to the twip, where its position would move
        // it in half points, and LibreOffice that half again; Lato under it, which Word sets high
        // and so is never moved down, stands where the page does all the same.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Lead").textStyle(lato(10)))
                .addParagraph(p -> p.text("Title").textStyle(spectral(30)).margin(DocumentInsets.top(20)))
                .addParagraph(p -> p.text("Body").textStyle(lato(10))))) {
            XWPFParagraph title = paragraph(document, "Title");
            double raise = 0.8 * line(title) - SPECTRAL_ASCENT * 30;

            assertThat(raise).as("the premise: Word's baseline is the lower, by less than the space")
                    .isBetween(1.0, 20.0);
            assertThat(before(title)).isCloseTo(Math.round((20 - raise) * 20), within(1L));
            assertThat(position(title)).as("nothing left to its position").isZero();
            assertThat(before(paragraph(document, "Body"))).as("what the title owes below it")
                    .isCloseTo(Math.round(raise * 20), within(1L));
            assertThat(position(paragraph(document, "Body"))).isZero();
        }
    }

    @Test
    void whatTheSpaceAboveCannotGiveIsLeftToThePosition() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Lead").textStyle(lato(10)))
                .addParagraph(p -> p.text("Title").textStyle(spectral(30)).margin(DocumentInsets.top(2)))
                .addParagraph(p -> p.text("Body").textStyle(lato(10))))) {
            XWPFParagraph title = paragraph(document, "Title");
            double raise = 0.8 * line(title) - SPECTRAL_ASCENT * 30;

            assertThat(before(title)).as("all of the 2pt").isZero();
            assertThat(position(title)).isCloseTo((int) Math.round((raise - 2) * 2), within(1));
            assertThat(before(paragraph(document, "Body"))).as("the 2pt the line took").isEqualTo(40);
        }
    }

    @Test
    void whatTheSpaceAboveLeavesOfAShiftOverHalfAPointKeepsItsPosition() throws Exception {
        // Poppins at 10pt stands 0.7pt low in Word: under 0.4pt of space the line takes the 0.4pt,
        // and the 0.3pt left is still moved by its position, a half point, not left 0.3pt low.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Lead").textStyle(lato(10)))
                .addParagraph(p -> p.text("Low").textStyle(DocumentTextStyle.builder().fontName(FontName.POPPINS)
                        .size(10).build()).margin(DocumentInsets.top(0.4))))) {
            XWPFParagraph low = paragraph(document, "Low");
            double raise = 0.8 * line(low) - POPPINS_ASCENT * 10;

            assertThat(raise).as("the premise: over half a point, and the space under it").isBetween(0.5, 0.9);
            assertThat(before(low)).isZero();
            assertThat(position(low)).isEqualTo((int) Math.round((raise - 0.4) * 2)).isPositive();
        }
    }

    @Test
    void aParagraphTheLayoutBreaksOverAPageKeepsTheSpaceAboveIt() throws Exception {
        // Its lines on the next page start at that page's top: the space above moves none of them.
        String text = "A title long enough to run onto a second line and a third one";
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.spacer(1, 500)
                    .addParagraph(p -> p.name("Broken").text(text).textStyle(spectral(30)).margin(DocumentInsets.top(10))));
            var placed = session.layoutGraph().nodes().stream()
                    .filter(node -> "Broken".equals(node.semanticName())).findFirst().orElseThrow();
            try (XWPFDocument document = new XWPFDocument(
                    new java.io.ByteArrayInputStream(session.export(new DocxSemanticBackend())))) {
                XWPFParagraph broken = paragraph(document, text);

                assertThat(placed.endPage()).as("the premise: broken over a page").isGreaterThan(placed.startPage());
                assertThat(before(broken)).as("the spacer's height past its hairline, and its own 10pt, whole")
                        .isEqualTo(Math.round((500 - 0.1 + 10) * 20));
                assertThat(position(broken)).as("raised by its position, on both pages").isPositive();
            }
        }
    }

    @Test
    void aRaiseUnderATenthOfAPointLeavesTheSpaceAboveAsWritten() throws Exception {
        // Gothic A1's page seat is within a few hundredths of Word's four fifths: Word sets lines
        // on a grid 0.12pt apart, and a twip less of space would move the line a step or nothing.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Lead").textStyle(lato(10)))
                .addParagraph(p -> p.text("Even").textStyle(DocumentTextStyle.builder().fontName(FontName.GOTHIC_A1)
                        .size(14).build()).margin(DocumentInsets.top(10))))) {
            XWPFParagraph even = paragraph(document, "Even");
            double raise = 0.8 * line(even) - GOTHIC_A1_ASCENT * 14;

            assertThat(raise).as("the premise: Word's baseline lower by a twip and more, under a tenth")
                    .isBetween(0.025, 0.1);
            assertThat(before(even)).isEqualTo(200);
        }
    }

    @Test
    void aLineWordSetsHighKeepsTheSpaceAboveIt() throws Exception {
        // Lato's ascent is 0.82 of its line: at 24pt Word's baseline stands 0.65pt above the
        // page's. Moved down into the space above, the line would take that from the space
        // below it, not known yet; it is lowered by its position, the space as written.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Lead").textStyle(lato(10)))
                .addParagraph(p -> p.text("Heading").textStyle(lato(24)).margin(DocumentInsets.top(10))))) {
            XWPFParagraph heading = paragraph(document, "Heading");
            double raise = 0.8 * line(heading) - LATO_ASCENT * 24;

            assertThat(raise).as("the premise: Word's baseline is the higher, by more than half a point")
                    .isLessThan(-0.5);
            assertThat(before(heading)).isEqualTo(200);
            assertThat(position(heading)).isEqualTo((int) Math.round(raise * 2));
        }
    }

    @Test
    void aLinePairKeepsTheSpaceAboveTheLineItsHalvesShare() throws Exception {
        // A 30pt value, the line's first and tallest text, beside a 12pt label share one Word
        // line: the value's raise moved into the space above would move the label with it.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Above").textStyle(lato(10)).margin(DocumentInsets.bottom(20)))
                .add(new ShapeContainerBuilder().name("Row").rectangle(360, 60)
                        .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                        .position(new ParagraphBuilder().name("Value").text("Value").textStyle(spectral(30)).build(),
                                0, 0, LayerAlign.CENTER_LEFT)
                        .position(new ParagraphBuilder().name("Label").text("Label").textStyle(spectral(12)).build(),
                                0, 0, LayerAlign.CENTER_RIGHT)
                        .build()))) {
            XWPFParagraph pair = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getText().contains("Value")).findFirst().orElseThrow();
            CTR value = pair.getRuns().stream().filter(run -> run.text().contains("Value")).findFirst()
                    .orElseThrow().getCTR();

            double raise = 0.8 * line(pair) - SPECTRAL_ASCENT * 30;

            assertThat(pair.getText()).as("the premise: one line").contains("Label");
            assertThat(before(pair)).as("the premise: room above the line to move it into").isGreaterThan(400);
            assertThat(positionOf(value)).as("the value raised by its own position, the line where it stood")
                    .isCloseTo((int) Math.round(raise * 2), within(1));
        }
    }

    @Test
    void aLineInALayerStackKeepsTheSpaceAboveIt() throws Exception {
        // A stack measures what follows it from the page: a raise owed below a line inside it
        // would not reach what follows.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Above").textStyle(lato(10)))
                .addLayerStack(stack -> stack.name("Stack")
                        .back(new com.demcha.compose.document.dsl.ShapeBuilder().name("Backdrop").size(200, 80).build())
                        .layer(new ParagraphBuilder().name("Title").text("Title").textStyle(spectral(30))
                                .margin(DocumentInsets.top(20)).build(), LayerAlign.TOP_LEFT)))) {
            XWPFParagraph title = paragraph(document, "Title");
            double raise = 0.8 * line(title) - SPECTRAL_ASCENT * 30;

            assertThat(raise).as("the premise: Word's baseline is the lower").isGreaterThan(1);
            assertThat(position(title)).as("raised by its position").isCloseTo((int) Math.round(raise * 2), within(1));
        }
    }

    @Test
    void theLastLineOfACellOwesItsRaiseInsideTheCell() throws Exception {
        // A column's last paragraph raised into the space above it owes that space below it, in
        // its cell: the cell is as tall as before, and nothing below the row moves.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("Lead").textStyle(lato(10)))
                .addRow(row -> row
                        .addParagraph(p -> p.text("Left").textStyle(spectral(30)).margin(DocumentInsets.top(20)))
                        .addParagraph(p -> p.text("Right").textStyle(lato(10))))
                .addParagraph(p -> p.text("Below").textStyle(lato(10))))) {
            List<XWPFParagraph> cell = document.getTables().get(0).getRow(0).getCell(0).getParagraphs();
            XWPFParagraph left = cell.get(cell.size() - 1);
            double raise = 0.8 * line(left) - SPECTRAL_ASCENT * 30;

            assertThat(left.getText()).as("the premise: the column's last paragraph").isEqualTo("Left");

            assertThat(before(left)).isCloseTo(Math.round((20 - raise) * 20), within(1L));
            assertThat(after(left)).isCloseTo(Math.round(raise * 20), within(1L));
            assertThat(position(left)).isZero();
            assertThat(before(paragraph(document, "Below"))).as("nothing of the raise below the row").isZero();
        }
    }

    @Test
    void aLineOfTheDefaultTextIsRaisedByWhatTheSpacingTestsAllowFor() throws Exception {
        assertThat(raiseOf(DocumentTextStyle.DEFAULT)).isEqualTo(DocxExports.DEFAULT_LINE_RAISE);
        assertThat(raiseOf(DocumentTextStyle.DEFAULT.withSize(7))).isEqualTo(DocxExports.SMALL_LINE_RAISE);
    }

    /**
     * How far Word stands a line of {@code style} below the page, in twips: four fifths of the
     * line it is written at, against the page's baseline in its laid-out line.
     */
    private static long raiseOf(DocumentTextStyle style) throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.addParagraph(p -> p.text("Line").textStyle(style)));
            ParagraphLine line = session.layoutGraph().fragments().stream()
                    .map(fragment -> fragment.payload())
                    .filter(ParagraphFragmentPayload.class::isInstance)
                    .map(payload -> ((ParagraphFragmentPayload) payload).lines().get(0))
                    .findFirst()
                    .orElseThrow();
            try (XWPFDocument document = new XWPFDocument(
                    new java.io.ByteArrayInputStream(session.export(new DocxSemanticBackend())))) {
                double written = line(paragraph(document, "Line"));
                double pageSeat = line.lineHeight() - line.baselineOffsetFromBottom();
                return Math.round((0.8 * written - pageSeat) * 20);
            }
        }
    }

    private static DocumentTextStyle lato(double size) {
        return DocumentTextStyle.builder().fontName(FontName.LATO).size(size).build();
    }

    private static long before(XWPFParagraph paragraph) {
        var properties = paragraph.getCTP().getPPr();
        return properties == null || !properties.isSetSpacing() || !properties.getSpacing().isSetBefore() ? 0
                : DocxTwips.of(properties.getSpacing().getBefore());
    }

    private static long after(XWPFParagraph paragraph) {
        var properties = paragraph.getCTP().getPPr();
        return properties == null || !properties.isSetSpacing() || !properties.getSpacing().isSetAfter() ? 0
                : DocxTwips.of(properties.getSpacing().getAfter());
    }

    @Test
    void linesWhoseGapIsSharedOutAreSeatedAtTheMiddleOne() throws Exception {
        // Two lines 10pt apart with no space above to give: Word's lines are each 5pt taller
        // than the face, stepping 5pt closer than the page's. Seated at the first line, the
        // second would stand 5pt high; at the middle, each is 2.5pt off.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addParagraph(p -> p.text("First\nSecond").textStyle(spectral(20)).lineSpacing(10)))) {
            XWPFParagraph text = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getText().contains("First")).findFirst().orElseThrow();
            double face = SPECTRAL_LINE * 20;
            double line = line(text);
            double middle = 0.5;

            assertThat(line).as("the premise: the one gap shared by two lines").isCloseTo(face + 5, within(0.05));
            assertThat(position(text)).isCloseTo((int) Math.round(
                    (middle * line + 0.8 * line - middle * (face + 10) - SPECTRAL_ASCENT * 20) * 2), within(1));
        }
    }

    private static XWPFParagraph paragraph(XWPFDocument document, String text) {
        return document.getParagraphs().stream()
                .filter(paragraph -> paragraph.getText().equals(text)).findFirst().orElseThrow();
    }

    private static double line(XWPFParagraph paragraph) {
        return DocxTwips.of(paragraph.getCTP().getPPr().getSpacing().getLine()) / 20.0;
    }

    private static int position(XWPFParagraph paragraph) {
        return positionOf(paragraph.getRuns().get(0).getCTR());
    }

    private static int positionOf(CTR run) {
        var properties = run.getRPr();
        return properties == null || properties.sizeOfPositionArray() == 0 ? 0
                : ((Number) properties.getPositionArray(0).getVal()).intValue();
    }
}
