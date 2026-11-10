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
import static org.assertj.core.api.Assertions.within;

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
        Measured measured = measuredAt(7.8, TextAlign.LEFT, SHORT);

        assertThat(measured.writtenSize()).as("the premise: Word is given a larger size").isGreaterThan(7.8);
        assertThat(measured.measureTwips())
                .as("one line: as many ems wide in Word as on the page, to the half twip the indent is rounded to")
                .isCloseTo(ROOM * 20 * measured.writtenSize() / 7.8, within(0.5));
    }

    @Test
    void aSizeWordSetsSmallerNarrowsAParagraphOfSeveralLines() throws Exception {
        // Long words, three to a line, leave each line well short of the room: the measure is the
        // page's ems a point short, not held out by the widest line.
        Measured measured = measuredAt(9.2, TextAlign.LEFT, SPARSE);
        double inEms = ROOM * 20 * measured.writtenSize() / 9.2;
        double widestInWord = laidOut(ROOM, p -> p.text(SPARSE).textStyle(DocumentTextStyle.DEFAULT.withSize(9.2)))
                .stream().mapToDouble(ParagraphLine::width).max().orElseThrow() * 20 * measured.writtenSize() / 9.2;

        assertThat(measured.writtenSize()).as("the premise: Word is given a smaller size").isLessThan(9.2);
        assertThat(widestInWord + 20).as("the premise: the widest line and a point stand clear of that measure")
                .isLessThan(inEms - 20 - 1);
        // The indent is written to the twip, so the measure is held to half of one.
        assertThat(measured.measureTwips()).as("the page's ems, a point short of them")
                .isCloseTo(inEms - 20, within(0.5));
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
    void aCentredOrRightAlignedParagraphOfSeveralLinesIsSpacedByTheLineFurthestOff() throws Exception {
        // ClassicSerif's 9.8pt profile, three centred lines set at 10pt, stood 3.7 to 4.9pt left of
        // the page's in Word. One spacing for the paragraph: the line Word sets furthest off, its
        // difference over its letters, to the nearest twentieth.
        for (double size : new double[] {7.8, 7.7}) {
            List<ParagraphLine> lines = laidOut(ROOM, p -> p.text(LONG).textStyle(DocumentTextStyle.DEFAULT.withSize(size)));
            int expected = furthestSpacing(lines);
            assertThat(lines).as("the premise: several lines").hasSizeGreaterThan(1);
            assertThat(expected).as("the premise: spaced at %spt", size).isNotZero();
            assertThat(breaksAsThePage(lines, expected, ROOM, size)).as("the premise: Word still breaks where the page does")
                    .isTrue();
            for (TextAlign align : new TextAlign[] {TextAlign.CENTER, TextAlign.RIGHT}) {
                try (XWPFDocument document = DocxExports.withLayout(400, 400, 20, page -> page
                        .addParagraph(p -> p.text(LONG).textStyle(DocumentTextStyle.DEFAULT.withSize(size)).align(align)))) {
                    XWPFParagraph paragraph = text(document, "Platform engineer");
                    assertThat(paragraph.getRuns()).as("every run spaced alike, %s at %spt", align, size)
                            .allSatisfy(run -> assertThat(run.getCharacterSpacing()).isEqualTo(expected));
                    assertThat(leftIndent(paragraph)).as("its measure stays the page's").isZero();
                    assertThat(rightIndent(paragraph)).isZero();
                }
            }
        }
    }

    @Test
    void theSpacingIsHeldBackWhereWordWouldBreakALineElsewhereThanThePage() throws Exception {
        // Spaced to the furthest line, a line of other letters, or a line rounded over a hundred of
        // them, comes out up to a couple of points past its own: narrowed, it could take up the
        // next line's first word, widened, run past the measure. Over a run of measures some line
        // ends near enough its measure or its next word for that; the spacing is held back there.
        for (double size : new double[] {7.8, 7.7}) {
            int heldBack = 0;
            int spaced = 0;
            for (double room = 300; room < 360; room += 0.5) {
                double measure = room;
                List<ParagraphLine> lines = laidOut(measure, p -> p.text(LONG).textStyle(DocumentTextStyle.DEFAULT.withSize(size)));
                int furthest = furthestSpacing(lines);
                try (XWPFDocument document = DocxExports.withLayout(measure + 40, 400, 20, page -> page
                        .addParagraph(p -> p.text(LONG).textStyle(DocumentTextStyle.DEFAULT.withSize(size)).align(TextAlign.CENTER)))) {
                    List<org.apache.poi.xwpf.usermodel.XWPFRun> runs = text(document, "Platform engineer").getRuns();
                    int spacing = runs.get(0).getCharacterSpacing();
                    assertThat(runs).allSatisfy(run -> assertThat(run.getCharacterSpacing()).isEqualTo(spacing));
                    assertThat(Math.abs(spacing)).as("never past the furthest line's, at %s", measure)
                            .isLessThanOrEqualTo(Math.abs(furthest));
                    assertThat(spacing * furthest).as("the same way, at %s", measure).isGreaterThanOrEqualTo(0);
                    // Unspaced, the paragraph is as Word sets it, past the page's or not.
                    assertThat(spacing == 0 || breaksAsThePage(lines, spacing, measure, size))
                            .as("Word breaks the lines where the page does, at %s, %spt", measure, size).isTrue();
                    spaced += spacing != 0 ? 1 : 0;
                    heldBack += spacing != furthest ? 1 : 0;
                }
            }
            assertThat(spaced).as("the premise: spaced over most measures at %spt", size).isGreaterThan(60);
            assertThat(heldBack).as("held back over some at %spt", size).isPositive();
        }
    }

    @Test
    void theSpacingIsTheFurthestLinesNotTheWidestsOrTheLasts() throws Exception {
        // A full line at 7.9pt, set at 8, grows 1.3%; a shorter one at 7.75pt, set at 8, 3.2%, and
        // further; a last line of narrow letters grows least a letter.
        // The first line's words are taken from LONG until the page sets the three apart.
        String[] words = LONG.split(" ");
        for (int count = 8; count < words.length; count++) {
            String first = String.join(" ", java.util.Arrays.copyOf(words, count));
            java.util.function.Consumer<com.demcha.compose.document.dsl.ParagraphBuilder> paragraph = p -> p
                    .align(TextAlign.CENTER)
                    .rich(rich -> rich.size(first, 7.9)
                            .size(" " + String.join(" ", java.util.Collections.nCopies(5, "WMWMWMW")), 7.75)
                            .size(" Documentationpipelinesandtemplates ii il li", 7.9));
            List<ParagraphLine> lines = laidOut(ROOM, paragraph);
            if (lines.size() != 3 || !lines.get(0).text().strip().equals(first)) {
                continue;
            }
            ParagraphLine widest = lines.stream().max(java.util.Comparator.comparingDouble(DocxWordSizeMeasureTest::asked)).orElseThrow();
            ParagraphLine last = lines.get(2);
            int expected = furthestSpacing(lines);
            if (expected == spacingOf(widest) || expected == spacingOf(last) || expected == 0
                || !breaksAsThePage(lines, expected, ROOM, 8)) {
                continue;
            }
            try (XWPFDocument document = DocxExports.withLayout(400, 400, 20, page -> page.addParagraph(paragraph))) {
                assertThat(text(document, "Platform engineer").getRuns())
                        .filteredOn(run -> run.text() != null && !run.text().isEmpty())
                        .isNotEmpty().allSatisfy(run -> assertThat(run.getCharacterSpacing()).isEqualTo(expected));
            }
            return;
        }
        throw new AssertionError("no first line sets the furthest line apart from the widest and the last");
    }

    @Test
    void aParagraphOfALineOfTextAndALineOfAPictureIsWeighedAsOneOfSeveralLines() throws Exception {
        // Its text line grows past a point, but the line under it opens with a picture, which the
        // guard has no word of to weigh: the text is not narrowed.
        java.util.function.Consumer<com.demcha.compose.document.dsl.ParagraphBuilder> paragraph = p -> p
                .align(TextAlign.CENTER)
                .rich(rich -> rich.size(SHORT, 7.8)
                        .image(com.demcha.compose.document.image.DocumentImageData.fromBytes(pngBytes()), 300, 8));
        List<ParagraphLine> lines = laidOut(ROOM, paragraph);
        assertThat(lines).as("the premise: a line of text and a line of a picture").hasSize(2);
        assertThat(lettersOf(lines.get(1))).isZero();
        assertThat(spacingOf(lines.get(0))).as("the premise: its text alone would be spaced").isNotZero();
        try (XWPFDocument document = DocxExports.withLayout(400, 400, 20, page -> page.addParagraph(paragraph))) {
            assertThat(text(document, "Platform engineer").getRuns())
                    .allSatisfy(run -> assertThat(run.getCharacterSpacing()).isZero());
        }
    }

    @Test
    void aParagraphOfSeveralLinesIndentedForItsPrefixIsNotSpaced() throws Exception {
        // The prefix's indent narrows Word's measure past the room the paragraph is written in.
        try (XWPFDocument plain = DocxExports.withLayout(400, 400, 20, page -> page
                     .addParagraph(p -> p.text(LONG).textStyle(DocumentTextStyle.DEFAULT.withSize(7.8)).align(TextAlign.CENTER)));
             XWPFDocument prefixed = DocxExports.withLayout(400, 400, 20, page -> page
                     .addParagraph(p -> p.text(LONG).textStyle(DocumentTextStyle.DEFAULT.withSize(7.8)).align(TextAlign.CENTER)
                             .bulletOffset("  ").indentStrategy(com.demcha.compose.document.style.DocumentTextIndent.ALL_LINES)))) {
            assertThat(text(plain, "Platform engineer").getRuns()).as("the premise: spaced without it")
                    .anySatisfy(run -> assertThat(run.getCharacterSpacing()).isNotZero());
            assertThat(text(prefixed, "Platform engineer").getRuns())
                    .allSatisfy(run -> assertThat(run.getCharacterSpacing()).isZero());
        }
    }

    /** A line's own difference over its letters, to the nearest twentieth of a point. */
    private static int spacingOf(ParagraphLine line) {
        return (int) Math.round((asked(line) - wordsWidth(line)) / lettersOf(line) * 20);
    }

    /** The furthest-off line's difference over its letters, to the nearest twentieth of a point. */
    private static int furthestSpacing(List<ParagraphLine> lines) {
        ParagraphLine furthest = lines.stream()
                .max(java.util.Comparator.comparingDouble(line -> Math.abs(asked(line) - wordsWidth(line)))).orElseThrow();
        return (int) Math.round((asked(furthest) - wordsWidth(furthest)) / lettersOf(furthest) * 20);
    }

    /**
     * Whether Word, at its size and with the letters spaced so, keeps a point clear of breaking
     * the page's lines elsewhere: widened, every line a point inside the measure; narrowed, no
     * line but the last within a point of taking up a space and the next line's first word.
     * Widths are the layout's, the next word's at the size given, set at Word's.
     */
    private static boolean breaksAsThePage(List<ParagraphLine> lines, int twentieths, double room, double size) {
        double grows = Math.round(size * 2) / 2.0 / size;
        double space = widthAlone("a a", size) - 2 * widthAlone("a", size);
        for (int index = 0; index < lines.size(); index++) {
            ParagraphLine line = lines.get(index);
            double set = wordsWidth(line) + lettersOf(line) * twentieths / 20.0;
            if (twentieths > 0 && set > room - 1) {
                return false;
            }
            if (twentieths < 0 && index + 1 < lines.size()) {
                String word = lines.get(index + 1).text().strip().split("\\s+")[0];
                double next = (space + widthAlone(word, size)) * grows
                              + (word.codePointCount(0, word.length()) + 1) * twentieths / 20.0;
                if (!(set + next > room + 1)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Test
    void aParagraphOfSeveralLinesSetFromItsLeftOrNearThePageIsNotSpaced() throws Exception {
        // From its left, a line's first letter is the page's; at 8.48pt, set at 8.5, the widest line
        // grows by less than a point.
        try (XWPFDocument left = DocxExports.withLayout(400, 400, 20, page -> page
                     .addParagraph(p -> p.text(LONG).textStyle(DocumentTextStyle.DEFAULT.withSize(7.8)).align(TextAlign.LEFT)));
             XWPFDocument near = DocxExports.withLayout(400, 400, 20, page -> page
                     .addParagraph(p -> p.text(LONG).textStyle(DocumentTextStyle.DEFAULT.withSize(8.48))
                             .align(TextAlign.CENTER)))) {
            assertThat(laidOut(ROOM, p -> p.text(LONG).textStyle(DocumentTextStyle.DEFAULT.withSize(8.48))))
                    .as("the premise: several lines, the widest growing less than a point")
                    .hasSizeGreaterThan(1)
                    .allSatisfy(line -> assertThat(wordsWidth(line) - asked(line)).isLessThan(1));
            assertThat(text(left, "Platform engineer").getRuns()).allSatisfy(run -> assertThat(run.getCharacterSpacing()).isZero());
            assertThat(text(near, "Platform engineer").getRuns()).allSatisfy(run -> assertThat(run.getCharacterSpacing()).isZero());
        }
    }

    @Test
    void aLineBesideAPictureHasItsTextAloneSpacedToThePagesWidth() throws Exception {
        // TimelineMinimal's 7.8pt e-mail, right-aligned beside its icon and set at 8pt, stood 2.7pt
        // left of the page's in Word. The picture is written at its own size; the text's letters
        // take the text's difference.
        java.util.function.Consumer<com.demcha.compose.document.dsl.ParagraphBuilder> line = p -> p
                .textStyle(DocumentTextStyle.DEFAULT.withSize(7.8))
                .rich(rich -> rich.size(SHORT + " ", 7.8)
                        .image(com.demcha.compose.document.image.DocumentImageData.fromBytes(pngBytes()), 24, 8));
        ParagraphLine laid = laidOut(ROOM, line).get(0);
        assertThat(laidOut(ROOM, line)).as("the premise: one line").hasSize(1);
        assertThat(laid.spans()).as("the premise: a picture in the line")
                .anyMatch(span -> !(span instanceof com.demcha.compose.document.layout.payloads.ParagraphTextSpan)
                                  && span.width() > 0);
        int expected = (int) Math.round((asked(laid) - wordsWidth(laid)) / lettersOf(laid) * 20);
        assertThat(expected).as("the premise: spaced").isNotZero();
        for (TextAlign align : new TextAlign[] {TextAlign.CENTER, TextAlign.RIGHT}) {
            try (XWPFDocument document = DocxExports.withLayout(400, 400, 20, page -> page
                    .addParagraph(p -> {
                        line.accept(p);
                        p.align(align);
                    }))) {
                XWPFParagraph paragraph = text(document, "Platform engineer");
                assertThat(paragraph.getRuns()).as("the text's runs, %s", align)
                        .filteredOn(run -> run.text() != null && !run.text().isEmpty()).isNotEmpty()
                        .allSatisfy(run -> assertThat(run.getCharacterSpacing()).isEqualTo(expected));
                assertThat(paragraph.getRuns()).as("the picture's, left as it is")
                        .filteredOn(run -> !run.getEmbeddedPictures().isEmpty()).isNotEmpty()
                        .allSatisfy(run -> assertThat(run.getCharacterSpacing()).isZero());
            }
        }
    }

    /** How wide the page set a line: its spans', text and pictures alike. */
    private static double asked(ParagraphLine line) {
        return line.spans().stream().mapToDouble(span -> Math.max(0, span.width())).sum();
    }

    /** The letters of a line's text, as the export counts them. */
    private static int lettersOf(ParagraphLine line) {
        int letters = 0;
        for (var span : line.spans()) {
            if (span instanceof com.demcha.compose.document.layout.payloads.ParagraphTextSpan text && text.text() != null) {
                letters += text.text().codePointCount(0, text.text().length());
            }
        }
        return letters;
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
            assertThat(ROOM * 20 - rightIndent(small)).as("the measure taken for the size the run states")
                    .isCloseTo(ROOM * 20 * writtenSize(small) / 7.8, within(0.5));
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
                    .isLessThanOrEqualTo(Math.round(200 * 20 * (writtenSize(cell) / 7.8 - 1)));
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
                double grows = writtenSize(text(document, "Built backend")) / 7.95;

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
    /** Nine long words, three to a line, each line well short of the room. */
    private static final String SPARSE = String.join(" ", java.util.Collections.nCopies(9, "Documentationpipelines"));

    /** A paragraph's measure in Word, in twips, and the size its run is written at, in points. */
    private record Measured(double measureTwips, double writtenSize) {
    }

    /** The measure Word sets a paragraph of the text given in, at a size and an alignment. */
    private static Measured measuredAt(double size, TextAlign align, String words) throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 400, 20, page -> page
                .addParagraph(p -> p.text(words).textStyle(DocumentTextStyle.DEFAULT.withSize(size)).align(align)))) {
            XWPFParagraph paragraph = text(document, words.split(" ")[0]);
            return new Measured(ROOM * 20 - rightIndent(paragraph), writtenSize(paragraph));
        }
    }

    /**
     * The size a paragraph's first run is written at, in points: the run's own, else its
     * paragraph style's, else the document's default, which the export writes to Normal as well.
     * Styles based on others are not followed: the export writes none for these paragraphs.
     */
    private static double writtenSize(XWPFParagraph paragraph) {
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr run =
                paragraph.getRuns().get(0).getCTR().getRPr();
        if (run != null && run.sizeOfSzArray() > 0) {
            return DocxTwips.of(run.getSzArray(0).getVal()) / 2.0;
        }
        org.apache.poi.xwpf.usermodel.XWPFStyles styles = paragraph.getDocument().getStyles();
        org.apache.poi.xwpf.usermodel.XWPFStyle style = paragraph.getStyleID() == null ? null
                : styles.getStyle(paragraph.getStyleID());
        if (style != null && style.getCTStyle().isSetRPr() && style.getCTStyle().getRPr().sizeOfSzArray() > 0) {
            return DocxTwips.of(style.getCTStyle().getRPr().getSzArray(0).getVal()) / 2.0;
        }
        return DocxTwips.of(styles.getCtStyles().getDocDefaults().getRPrDefault().getRPr().getSzArray(0).getVal()) / 2.0;
    }

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
