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

    @Test
    void eachLineLaidOverTheOneAboveTakesTheDistanceBetweenTheirFeet() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .add(title(2 * PITCH + 40)))) {
            assertThat(line(paragraph(document, "One"))).as("the first line keeps its own height")
                    .isGreaterThan(PITCH);
            assertThat(line(paragraph(document, "Two"))).isCloseTo(PITCH, within(0.05));
            assertThat(line(paragraph(document, "Three"))).isCloseTo(PITCH, within(0.05));
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
        // A box 4pt shorter leaves the last line hanging 4pt further below it, and the
        // paragraph after it that much less space above.
        double shorter = spaceAboveTheParagraphAfter(2 * PITCH + 30);
        double taller = spaceAboveTheParagraphAfter(2 * PITCH + 34);

        assertThat(taller - shorter).isCloseTo(4, within(0.1));
    }

    @Test
    void anIconBesideItsLabelIsDrawnWhereThePagePutsItNotWrittenAboveIt() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .add(fact(30)))) {
            String body = document.getDocument().xmlText();

            assertThat(body).as("no line of its own in the flow").doesNotContain("<wp:inline");
            assertThat(body).as("drawn where the page puts it").contains("<wp:anchor").contains("<pic:pic");
            assertThat(paragraph(document, "Project duration")).isNotNull();
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
                        .add(title(boxHeight))
                        .addParagraph("After")))) {
            CTPPr properties = paragraph(document, "After").getCTP().getPPr();
            return properties != null && properties.isSetSpacing() && properties.getSpacing().isSetBefore()
                    ? DocxTwips.of(properties.getSpacing().getBefore()) / 20.0
                    : 0;
        }
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
