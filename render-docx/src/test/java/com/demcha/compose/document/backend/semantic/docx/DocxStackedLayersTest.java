package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

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
    void eachLineLaidOverByTheNextTakesTheDistanceDownToItsTop() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .add(title(2 * PITCH + 40)))) {
            assertThat(line(paragraph(document, "One"))).isCloseTo(PITCH, within(0.05));
            assertThat(line(paragraph(document, "Two"))).isCloseTo(PITCH, within(0.05));
            assertThat(line(paragraph(document, "Three"))).as("room for its own line under the box: its own height")
                    .isCloseTo(LARGE_LINE, within(0.05));
        }
    }

    @Test
    void theLastLineOfAStackEndsAtTheContainersFootAndHangsPastNothing() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addSection("Cover", cover -> cover.spacing(30)
                        .add(title(2 * PITCH + 30))
                        .addParagraph("After")))) {
            assertThat(line(paragraph(document, "Three"))).as("the rest of the box, not its own 36pt")
                    .isCloseTo(30, within(0.05));
            assertThat(before(paragraph(document, "After"))).as("the whole gap under the box")
                    .isCloseTo(30, within(0.05));
        }
    }

    @Test
    void aLineLaidTooTightForItsFaceKeepsItsOwnHeight() throws Exception {
        // 18pt apart is under two thirds of a 30pt face: Word would cut its letters off.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .add(new ShapeContainerBuilder().name("Tight").rectangle(300, 60)
                        .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                        .position(new ParagraphBuilder().name("One").text("One").textStyle(LARGE).build(),
                                0, 0, LayerAlign.TOP_LEFT)
                        .position(new ParagraphBuilder().name("Two").text("Two").textStyle(LARGE).build(),
                                0, 18, LayerAlign.TOP_LEFT)
                        .build()))) {
            assertThat(line(paragraph(document, "Two"))).isEqualTo(line(paragraph(document, "One")));
        }
    }

    @Test
    void aLineHoldingAPictureKeepsItsOwnHeight() throws Exception {
        // A picture is not its line's face: squeezed to the 28pt left in the box, Word would
        // cut its top off.
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
        // Two lines too tight to stack: the second keeps its own line, its foot 54pt down. A
        // box 4pt shorter leaves it hanging 4pt further below, and the paragraph after it that
        // much less space above.
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
                        .add(new ShapeContainerBuilder().name("Tight").rectangle(300, boxHeight)
                                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                                .position(new ParagraphBuilder().name("One").text("One").textStyle(LARGE).build(),
                                        0, 0, LayerAlign.TOP_LEFT)
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
