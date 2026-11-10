package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.fixed.pdf.PdfFontLibraryFactory;
import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Layers a container stacks over one another, written one after the other in Word.
 *
 * <p>A title set as lines a pitch apart, tighter than the face's own line, took each line's
 * own height in Word, and an icon laid beside its label took a line of its own above it:
 * {@code NorthlineProposal}'s cover ran onto a third page, its drawings a hundred points off
 * the text they belong to.</p>
 */
class DocxStackedLayersTest {

    private static final double PITCH = 32;
    private static final DocumentTextStyle LARGE = DocumentTextStyle.builder().fontName(FontName.LATO).size(30).build();
    /** Lato's own line at 30pt: its ascent and descent, 2400 of 2000 units. */
    private static final double LARGE_LINE = 36;

    @Test
    void eachLineOfAStackHoldsItsLettersWholeAndStandsOnThePagesBaseline() throws Exception {
        // Word draws an exact line's text on screen only inside the line.
        for (Stacked line : stacked(title(2 * PITCH + 40), "One", "Two", "Three")) {
            assertThat(line.wordBaseline()).as("%s on the page's baseline", line.text())
                    .isCloseTo(line.pageBaseline(), within(0.3));
            assertThat(line.wordBaseline() - line.inkAbove()).as("%s: its letters' tops inside its line", line.text())
                    .isGreaterThanOrEqualTo(line.top());
            assertThat(line.wordBaseline() + line.inkBelow()).as("%s: its letters' feet inside its line", line.text())
                    .isLessThanOrEqualTo(line.top() + line.height());
        }
    }

    @Test
    void aTitleSetTighterThanItsFaceHoldsItsLettersWhole() throws Exception {
        // NorthlineProposal's title: 46pt Spectral lines 48pt apart, whose letters a 48pt step
        // cuts — "Proposal" loses its descenders, "Brand" the tops of its capitals.
        DocumentTextStyle display = DocumentTextStyle.builder().fontName(FontName.SPECTRAL).size(46).build();
        DocumentNode title = new ShapeContainerBuilder().name("Display").rectangle(520, 140)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(new ParagraphBuilder().name("A").text("Proposal —").textStyle(display).build(),
                        0, 0, LayerAlign.TOP_LEFT)
                .position(new ParagraphBuilder().name("B").text("Brand Refresh &").textStyle(display).build(),
                        0, 48, LayerAlign.TOP_LEFT)
                .position(new ParagraphBuilder().name("C").text("Website Redesign").textStyle(display).build(),
                        0, 96, LayerAlign.TOP_LEFT)
                .build();
        for (Stacked line : stacked(title, "Proposal —", "Brand Refresh &", "Website Redesign")) {
            assertThat(line.wordBaseline()).as("%s on the page's baseline", line.text())
                    .isCloseTo(line.pageBaseline(), within(0.3));
            assertThat(line.wordBaseline() - line.inkAbove()).as("%s: tops inside", line.text())
                    .isGreaterThanOrEqualTo(line.top());
            assertThat(line.wordBaseline() + line.inkBelow()).as("%s: feet inside", line.text())
                    .isLessThanOrEqualTo(line.top() + line.height());
        }
    }

    @Test
    void theLastLineOfAStackEndsAtTheContainersFootAndHangsPastNothing() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addSection("Cover", cover -> cover.spacing(30)
                        .add(title(2 * PITCH + 34))
                        .addParagraph("After")))) {
            double stack = line(paragraph(document, "One")) + line(paragraph(document, "Two"))
                           + line(paragraph(document, "Three"));
            assertThat(stack).as("as tall as the box, its last line's own 36pt running past it")
                    .isCloseTo(2 * PITCH + 34, within(0.1));
            assertThat(before(paragraph(document, "After"))).as("the whole gap under the box, less its line's raise")
                    .isCloseTo(30 - DocxExports.DEFAULT_LINE_RAISE / 20.0, within(0.05));
        }
    }

    @Test
    void theLettersOfAStacksLastLineHangingPastTheFootComeOutOfTheGapUnderIt() throws Exception {
        // The box ends 4pt above the last line's baseline: its letters stand past the foot.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addSection("Cover", cover -> cover.spacing(30)
                        .add(title(2 * PITCH + 26))
                        .addParagraph("After")))) {
            double stack = line(paragraph(document, "One")) + line(paragraph(document, "Two"))
                           + line(paragraph(document, "Three"));
            double hang = stack - (2 * PITCH + 26);
            Stacked last = stacked(title(2 * PITCH + 26), "One", "Two", "Three").get(2);

            assertThat(hang).as("just below its letters, not where its own line ends")
                    .isCloseTo(last.pageBaseline() + last.inkBelow() + DocxStackedLines.INK_MARGIN - (2 * PITCH + 26),
                            within(0.1));
            assertThat(before(paragraph(document, "After"))).as("by as much less gap under it, and its line's raise")
                    .isCloseTo(30 - hang - DocxExports.DEFAULT_LINE_RAISE / 20.0, within(0.1));
        }
    }

    @Test
    void aStackedLineSeatedAtItsTopHoldsItsRaisedLettersWhole() throws Exception {
        // "Two" is set against the top of its line, 8pt higher than on its baseline: its letters
        // come that much closer to the line above, and the edge between them moves up with them.
        DocumentNode title = new ShapeContainerBuilder().name("Seated").rectangle(300, 2 * PITCH + 40)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(new ParagraphBuilder().name("One").text("One").textStyle(LARGE).build(),
                        0, 0, LayerAlign.TOP_LEFT)
                .position(new ParagraphBuilder().name("Two").text("Two").textStyle(LARGE)
                        .verticalAlign(com.demcha.compose.document.node.TextVerticalAlign.TOP).build(),
                        0, PITCH, LayerAlign.TOP_LEFT)
                .position(new ParagraphBuilder().name("Three").text("Three").textStyle(LARGE).build(),
                        0, 2 * PITCH, LayerAlign.TOP_LEFT)
                .build();
        for (Stacked line : stacked(title, "One", "Two", "Three")) {
            assertThat(line.wordBaseline()).as("%s where the page seats it", line.text())
                    .isCloseTo(line.pageBaseline(), within(0.3));
            assertThat(line.wordBaseline() - line.inkAbove()).as("%s: tops inside", line.text())
                    .isGreaterThanOrEqualTo(line.top());
            assertThat(line.wordBaseline() + line.inkBelow()).as("%s: feet inside", line.text())
                    .isLessThanOrEqualTo(line.top() + line.height());
        }
    }

    @Test
    void linesWhoseLettersOverlapAreStillSplitHalfwaySoThePageDoesNotMove() throws Exception {
        // 18pt apart in a 30pt face, the letters of one line reach into the next's: no edge
        // leaves both whole, and each line at its own 36pt would push the page 18pt down.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .add(new ShapeContainerBuilder().name("Tight").rectangle(300, 60)
                        .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                        .position(new ParagraphBuilder().name("One").text("One").textStyle(LARGE).build(),
                                0, 0, LayerAlign.TOP_LEFT)
                        .position(new ParagraphBuilder().name("Two").text("Two").textStyle(LARGE).build(),
                                0, 18, LayerAlign.TOP_LEFT)
                        .build()))) {
            double one = line(paragraph(document, "One"));
            double two = line(paragraph(document, "Two"));

            assertThat(one).as("cut halfway into the letters both lines share, short of its own 36pt")
                    .isLessThan(LARGE_LINE - 5);
            assertThat(one + two).as("ending where the second line's own box does").isCloseTo(18 + LARGE_LINE, within(0.1));
        }
    }

    @Test
    void aLineHoldingAPictureKeepsItsOwnHeight() throws Exception {
        // A picture's height is not its line's letters': squeezed to the 28pt left in the box,
        // Word would cut its top off.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .add(new ShapeContainerBuilder().name("Mixed").rectangle(300, PITCH + 28)
                        .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                        .position(new ParagraphBuilder().name("One").text("One").textStyle(LARGE).build(),
                                0, 0, LayerAlign.TOP_LEFT)
                        .position(new ParagraphBuilder().name("Two").inlineText("Two", LARGE)
                                .inlineImage(com.demcha.compose.document.image.DocumentImageData.fromBytes(pngBytes()),
                                        30, 30).build(),
                                0, PITCH, LayerAlign.TOP_LEFT)
                        .build()))) {
            XWPFParagraph two = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getText().startsWith("Two")).findFirst().orElseThrow();
            assertThat(line(two)).as("its own line, not the rest of the box")
                    .isCloseTo(LARGE_LINE, within(0.05));
        }
    }

    @Test
    void anIconBesideItsLabelInAPaintedPanelStandsInFrontOfItsShading() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addSection("Card", card -> card.fillColor(com.demcha.compose.document.style.DocumentColor.rgb(240, 240, 240))
                        .add(fact(30))))) {
            String body = document.getDocument().xmlText();

            assertThat(body).doesNotContain("<wp:inline");
            assertThat(body).contains("<pic:pic").contains("behindDoc=\"0\"");
        }
    }

    @Test
    void aLineBesideTheOneAboveKeepsItsOwnHeight() throws Exception {
        // A value set right of its label a few points lower is not laid over it: squeezed to
        // those few points, Word would cut its letters off.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .add(new ShapeContainerBuilder().name("Pair").rectangle(300, 40)
                        .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                        .position(new ParagraphBuilder().name("Label").text("Label").build(),
                                0, 0, LayerAlign.TOP_LEFT)
                        .position(new ParagraphBuilder().name("Value").text("Value").textStyle(LARGE).build(),
                                0, 8, LayerAlign.TOP_RIGHT)
                        .position(new ParagraphBuilder().name("Unit").text("Unit").build(),
                                0, 0, LayerAlign.BOTTOM_LEFT)
                        .build()))) {
            assertThat(line(paragraph(document, "Value"))).as("its own height, not the step down from the label").isGreaterThan(33);
        }
    }

    @Test
    void theLastLinesOverhangBelowItsBoxComesOutOfTheGapUnderIt() throws Exception {
        // One line, no stack, set 18pt down: its own line's foot is 54pt down. A box 4pt shorter
        // leaves it hanging 4pt further below, and the paragraph after it that much less space
        // above.
        double shorter = spaceAboveTheParagraphAfter(46);
        double taller = spaceAboveTheParagraphAfter(50);

        assertThat(taller - shorter).isCloseTo(4, within(0.1));
    }

    @Test
    void anIconBesideItsLabelIsDrawnWhereThePagePutsItNotWrittenAboveIt() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .add(fact(30)))) {
            String body = document.getDocument().xmlText();

            assertThat(body).as("no line of its own in the flow").doesNotContain("<wp:inline");
            assertThat(body).as("drawn where the page puts it").contains("<wp:anchor").contains("<pic:pic");
            assertThat(document.getParagraphs()).extracting(XWPFParagraph::getText).contains("Project duration");
        }
    }

    @Test
    void anIconOverItsLabelIsWrittenAsBefore() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .add(fact(0)))) {
            assertThat(document.getDocument().xmlText()).contains("<wp:inline");
        }
    }

    private static double spaceAboveTheParagraphAfter(double boxHeight) throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addSection("Cover", cover -> cover.spacing(30)
                        .add(new ShapeContainerBuilder().name("Low").rectangle(300, boxHeight)
                                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                                .position(new ParagraphBuilder().name("Two").text("Two").textStyle(LARGE).build(),
                                        0, 18, LayerAlign.TOP_LEFT)
                                .build())
                        .addParagraph("After")))) {
            return before(paragraph(document, "After"));
        }
    }

    private static double before(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().getPPr();
        return properties != null && properties.isSetSpacing() && properties.getSpacing().isSetBefore()
                ? DocxTwips.of(properties.getSpacing().getBefore()) / 20.0
                : 0;
    }

    /**
     * One line of a stack as Word sets it, measured down from the stack's first line's top.
     *
     * @param top          where its Word line starts
     * @param height       its Word line's height
     * @param wordBaseline where Word stands its baseline: four fifths down, less its raise
     * @param pageBaseline where the page sets it
     * @param inkAbove     how far its letters reach above the baseline
     * @param inkBelow     and below it
     */
    private record Stacked(String text, double top, double height, double wordBaseline, double pageBaseline,
                           double inkAbove, double inkBelow) {
    }

    /** Lays a container of stacked lines out, exports it, and reads each line's both ways. */
    private static List<Stacked> stacked(DocumentNode container, String... texts) throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(595, 600)
                .margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.add(container));
            List<PlacedFragment> fragments = session.layoutGraph().fragments();
            byte[] docx = session.export(new DocxSemanticBackend());
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
                List<Stacked> lines = new ArrayList<>();
                double firstTop = Double.NaN;
                double wordTop = 0;
                for (String text : texts) {
                    PlacedFragment fragment = fragments.stream()
                            .filter(f -> f.payload() instanceof ParagraphFragmentPayload p
                                         && p.lines().get(0).text().startsWith(text.split(" ")[0]))
                            .findFirst().orElseThrow();
                    ParagraphFragmentPayload payload = (ParagraphFragmentPayload) fragment.payload();
                    ParagraphLine line = payload.lines().get(0);
                    double pageTop = fragment.y() + fragment.height() - payload.padding().top();
                    if (Double.isNaN(firstTop)) {
                        firstTop = pageTop;
                    }
                    com.demcha.compose.font.FontLibrary fonts = PdfFontLibraryFactory.measurementLibrary(List.of());
                    // Measured down from the first line's top; text the page seats up stands higher.
                    double seat = payload.verticalAlign() == com.demcha.compose.document.node.TextVerticalAlign.DEFAULT
                            ? 0 : com.demcha.compose.document.backend.fixed.pdf.handlers.ParagraphSeating
                                    .shift(line, fonts, payload.verticalAlign());
                    double pageBaseline = firstTop - pageTop + line.lineHeight() - line.baselineOffsetFromBottom() - seat;
                    double[] ink = DocxInk.of(line, fonts);
                    XWPFParagraph written = document.getParagraphs().stream()
                            .filter(paragraph -> paragraph.getText().startsWith(text.split(" ")[0]))
                            .findFirst().orElseThrow();
                    double height = line(written);
                    double raise = positionOf(written) / 2.0;
                    assertThat(payload.lines()).as("%s on one line", text).hasSize(1);
                    lines.add(new Stacked(text, wordTop, height, wordTop + 0.8 * height - raise, pageBaseline,
                            ink[0], ink[1]));
                    wordTop += height;
                }
                // Each edge between two lines stands halfway between the one's letters and the next's.
                for (int k = 0; k + 1 < lines.size(); k++) {
                    Stacked above = lines.get(k);
                    Stacked below = lines.get(k + 1);
                    assertThat(below.top()).as("the edge under %s", above.text()).isCloseTo(
                            (above.pageBaseline() + above.inkBelow() + below.pageBaseline() - below.inkAbove()) / 2,
                            within(0.05));
                }
                return lines;
            }
        }
    }

    private static int positionOf(XWPFParagraph paragraph) {
        var properties = paragraph.getRuns().get(0).getCTR().getRPr();
        return properties == null || properties.sizeOfPositionArray() == 0 ? 0
                : ((Number) properties.getPositionArray(0).getVal()).intValue();
    }

    private static DocumentNode title(double boxHeight) {
        return new ShapeContainerBuilder().name("Title").rectangle(300, boxHeight)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(new ParagraphBuilder().name("One").text("One").textStyle(LARGE).build(),
                        0, 0, LayerAlign.TOP_LEFT)
                .position(new ParagraphBuilder().name("Two").text("Two").textStyle(LARGE).build(),
                        0, PITCH, LayerAlign.TOP_LEFT)
                .position(new ParagraphBuilder().name("Three").text("Three").textStyle(LARGE).build(),
                        0, 2 * PITCH, LayerAlign.TOP_LEFT)
                .build();
    }

    private static DocumentNode fact(double labelOffset) {
        return new ShapeContainerBuilder().name("Fact").rectangle(200, 30)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(new ImageBuilder().name("Icon").source(pngBytes()).size(20, 20).build(),
                        0, 0, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder().name("Label").text("Project duration").build(),
                        labelOffset, 0, LayerAlign.CENTER_LEFT)
                .build();
    }

    private static XWPFParagraph paragraph(XWPFDocument document, String text) {
        return document.getParagraphs().stream()
                .filter(paragraph -> paragraph.getText().equals(text)).findFirst().orElseThrow();
    }

    private static double line(XWPFParagraph paragraph) {
        return DocxTwips.of(paragraph.getCTP().getPPr().getSpacing().getLine()) / 20.0;
    }

    private static byte[] pngBytes() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }
}
