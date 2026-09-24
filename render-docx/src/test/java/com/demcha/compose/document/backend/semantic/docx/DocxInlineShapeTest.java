package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentPathSegment;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.ShapeOutline;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An inline shape — a dot, an arrow, a checkbox — reaches Word as a picture among the words.
 *
 * <p>It was dropped with a report entry, so the status dots, contact-line separators, arrows
 * and checkboxes the templates put among their words were not in the document. Word has
 * no drawing in a line of text, so each is drawn into a transparent picture from the outline,
 * fill and stroke the page draws, and placed where the page's alignment puts it.</p>
 */
class DocxInlineShapeTest {

    private static final DocumentColor INK = DocumentColor.rgb(0x1A, 0x56, 0x94);

    @Test
    void aDotIsAPictureInItsOwnRunBetweenTheWords() throws Exception {
        try (XWPFDocument document = export(page -> page.addParagraph(p -> p
                .inlineText("Open").dot(6, INK).inlineText("Paid")))) {
            List<XWPFRun> runs = document.getParagraphs().get(0).getRuns();

            assertThat(runs).extracting(XWPFRun::text).containsExactly("Open", "", "Paid");
            XWPFPicture dot = runs.get(1).getEmbeddedPictures().get(0);
            assertThat(dot.getCTPicture().getSpPr().getXfrm().getExt().getCx()).isEqualTo(Units.toEMU(6));
            BufferedImage image = pixels(dot);
            assertThat(image.getRGB(0, 0) >>> 24).as("the corner outside the circle shows the page").isZero();
            assertThat(image.getRGB(image.getWidth() / 2, image.getHeight() / 2) & 0xFFFFFF).isEqualTo(0x1A5694);
        }
    }

    @Test
    void anArrowIsDrawnPointingTheWayThePageDrawsIt() throws Exception {
        try (XWPFDocument document = export(page -> page.addParagraph(p -> p
                .inlineText("Next ").arrow(12, ShapeOutline.Direction.RIGHT, INK)))) {
            BufferedImage image = pixels(document.getParagraphs().get(0).getRuns().get(1).getEmbeddedPictures().get(0));
            int middle = image.getHeight() / 2;

            assertThat(image.getRGB(image.getWidth() - 3, middle) >>> 24).as("the tip, on the right").isNotZero();
            assertThat(image.getRGB(image.getWidth() - 3, 1) >>> 24).as("beside the tip, empty").isZero();
        }
    }

    @Test
    void aStrokedShapeIsAsLargeAsItsStrokeAndStandsWhereThePageDrawsItsOutline() throws Exception {
        // The page draws a stroke centred on the outline, half of it outside the run's box: the
        // picture takes that half on every side and is lowered by it, so the outline stays put.
        try (XWPFDocument plain = export(page -> page.addParagraph(p -> p.inlineText("A ").dot(8, INK)));
             XWPFDocument stroked = export(page -> page.addParagraph(p -> p
                     .inlineText("A ").dot(8, INK, DocumentStroke.of(DocumentColor.rgb(0, 0, 0), 2))))) {
            XWPFRun plainDot = plain.getParagraphs().get(0).getRuns().get(1);
            XWPFRun strokedDot = stroked.getParagraphs().get(0).getRuns().get(1);

            assertThat(strokedDot.getEmbeddedPictures().get(0).getCTPicture().getSpPr().getXfrm().getExt().getCy())
                    .isEqualTo(Units.toEMU(10));
            assertThat(position(strokedDot)).as("a point lower, in half-points").isEqualTo(position(plainDot) - 2);
        }
    }

    @Test
    void aStrokedShapeOnTheBaselineHangsItsHalfStrokeBelowIt() throws Exception {
        // Centred, the half-stroke comes out of the centring on its own; on the baseline it
        // does not: the outline's bottom is on the baseline, the picture's a point below it.
        try (XWPFDocument document = export(page -> page.addParagraph(p -> p
                .inlineText("A ")
                .shape(ShapeOutline.circle(8), INK, DocumentStroke.of(DocumentColor.rgb(0, 0, 0), 2),
                        com.demcha.compose.document.node.InlineImageAlignment.BASELINE, 0, null)))) {
            assertThat(position(document.getParagraphs().get(0).getRuns().get(1))).isEqualTo(-2);
        }
    }

    @Test
    void aCheckboxIsOnePictureWithItsCheckInsideItsFrame() throws Exception {
        try (XWPFDocument document = export(page -> page.addParagraph(p -> p
                .checkbox(12, true, INK).inlineText(" Signed")))) {
            List<XWPFRun> runs = document.getParagraphs().get(0).getRuns();

            assertThat(runs.get(0).getEmbeddedPictures()).hasSize(1);
            assertThat(runs.get(1).text()).isEqualTo(" Signed");
        }
    }

    @Test
    void everyOutlineIsAPathInTheUnitBox() {
        assertThat(DocxShapePictures.unitSegments(new ShapeOutline.Rectangle(10, 4)))
                .startsWith(DocumentPathSegment.moveTo(0, 0))
                .contains(DocumentPathSegment.lineTo(1, 1));
        assertThat(DocxShapePictures.unitSegments(new ShapeOutline.Ellipse(10, 4)))
                .startsWith(DocumentPathSegment.moveTo(1, 0.5))
                .endsWith(DocumentPathSegment.close());
        // A radius larger than half the shorter side is clamped to it, as the page clamps it:
        // on a 10 × 4 box a radius of 9 rounds each corner by 2, a fifth of the width.
        assertThat(DocxShapePictures.unitSegments(new ShapeOutline.RoundedRectangle(10, 4, 9)))
                .startsWith(DocumentPathSegment.moveTo(0.2, 0));
        assertThat(DocxShapePictures.unitSegments(ShapeOutline.triangle(10, 10)))
                .last().isEqualTo(DocumentPathSegment.close());
    }

    private static BufferedImage pixels(XWPFPicture picture) throws Exception {
        return ImageIO.read(new ByteArrayInputStream(picture.getPictureData().getData()));
    }

    private static int position(XWPFRun run) {
        return run.getCTR().isSetRPr() && run.getCTR().getRPr().sizeOfPositionArray() > 0
                ? ((Number) run.getCTR().getRPr().getPositionArray(0).getVal()).intValue()
                : 0;
    }

    private static XWPFDocument export(Consumer<PageFlowBuilder> content) throws Exception {
        return DocxExports.withLayout(400, 400, 20, content);
    }
}
