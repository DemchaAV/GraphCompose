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
            assertThat(dot.getCTPicture().getSpPr().getXfrm().getExt().getCx())
                    .as("the dot and a pixel's room either side, so its rim is not cut")
                    .isEqualTo(Units.toEMU(6 + 2 * DocxShapePictures.EDGE));
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
    void aStrokedShapeIsAsWideAsItsStroke() throws Exception {
        // The page draws a stroke centred on the outline, half of it outside the run's box: the
        // picture takes that half on every side, so the ring is not cut.
        try (XWPFDocument stroked = export(page -> page.addParagraph(p -> p
                .inlineText("A ").dot(8, INK, DocumentStroke.of(DocumentColor.rgb(0, 0, 0), 2))))) {
            XWPFPicture ring = stroked.getParagraphs().get(0).getRuns().get(1).getEmbeddedPictures().get(0);

            assertThat(ring.getCTPicture().getSpPr().getXfrm().getExt().getCx())
                    .isEqualTo(Units.toEMU(10 + 2 * DocxShapePictures.EDGE));
        }
    }

    @Test
    void aShapeThePageRaisesCarriesTheRiseAndNeedsNoPosition() throws Exception {
        // A separator dot centred on a line sits above the baseline. LibreOffice stands every
        // picture on the baseline, ignoring w:position, so the rise is empty rows under the dot.
        try (XWPFDocument document = export(page -> page.addParagraph(p -> p
                .inlineText("London").dot(3, INK).inlineText("Berlin")))) {
            XWPFRun separator = document.getParagraphs().get(0).getRuns().get(1);
            XWPFPicture dot = separator.getEmbeddedPictures().get(0);
            BufferedImage image = pixels(dot);

            assertThat(Math.abs(position(separator))).as("a rounding at most").isLessThanOrEqualTo(1);
            assertThat(Units.toPoints(dot.getCTPicture().getSpPr().getXfrm().getExt().getCy()))
                    .as("taller than the dot by its rise")
                    .isGreaterThan(3 + 2 * DocxShapePictures.EDGE + 1);
            assertThat(image.getRGB(image.getWidth() / 2, image.getHeight() - 1) >>> 24)
                    .as("the rise is empty").isZero();
            int dotMiddle = (int) Math.round((DocxShapePictures.EDGE + 1.5) * image.getHeight()
                    / Units.toPoints(dot.getCTPicture().getSpPr().getXfrm().getExt().getCy()));
            assertThat(image.getRGB(image.getWidth() / 2, dotMiddle) & 0xFFFFFF)
                    .as("the dot, at the top of the picture").isEqualTo(0x1A5694);
        }
    }

    @Test
    void anIconThePageRaisesCarriesTheRiseToo() throws Exception {
        com.demcha.compose.document.svg.SvgIcon icon = com.demcha.compose.document.svg.SvgIcon.parse(
                "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'><circle cx='12' cy='12' r='12'/></svg>");
        try (XWPFDocument document = export(page -> page.addParagraph(p -> p
                .inlineText("Call ").inlineSvgIcon(icon, 4).inlineText(" now")))) {
            XWPFRun run = document.getParagraphs().get(0).getRuns().get(1);

            assertThat(Math.abs(position(run))).isLessThanOrEqualTo(1);
            assertThat(Units.toPoints(run.getEmbeddedPictures().get(0).getCTPicture().getSpPr().getXfrm().getExt().getCy()))
                    .isGreaterThan(4 + 1);
        }
    }

    @Test
    void aShapeInsideItsTextLeavesTheLineExactThoughItsPictureHasAFrame() throws Exception {
        // Set on the text's bottom, a small dot is inside the text; the pixel of empty frame
        // around it is not ink, and counted as ink it had put the line at "at least".
        try (XWPFDocument plain = export(page -> page.addParagraph(p -> p.inlineText("Plain text")));
             XWPFDocument dotted = export(page -> page.addParagraph(p -> p
                     .inlineText("Plain ")
                     .shape(ShapeOutline.circle(4), INK, null,
                             com.demcha.compose.document.node.InlineImageAlignment.TEXT_BOTTOM, 0, null)
                     .inlineText(" text")))) {
            var spacing = dotted.getParagraphs().get(0).getCTP().getPPr().getSpacing();

            assertThat(spacing.getLineRule())
                    .isEqualTo(org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.EXACT);
            assertThat(DocxTwips.of(spacing.getLine()))
                    .isEqualTo(DocxTwips.of(plain.getParagraphs().get(0).getCTP().getPPr().getSpacing().getLine()));
        }
    }

    @Test
    void aSharpStrokedCornerReachesFartherThanHalfItsStroke() {
        // The miter join runs out to a star's point, so half the stroke would cut its tips.
        var star = new com.demcha.compose.document.node.ShapeLayer(ShapeOutline.star(10, 10), null,
                DocumentStroke.of(INK, 2));
        var circle = new com.demcha.compose.document.node.ShapeLayer(ShapeOutline.circle(10), null,
                DocumentStroke.of(INK, 2));

        assertThat(DocxShapePictures.reachPastTheBox(star, 10, 10)).isGreaterThan(1.5);
        assertThat(DocxShapePictures.reachPastTheBox(circle, 10, 10)).isCloseTo(1.0,
                org.assertj.core.api.Assertions.within(0.01));
    }

    @Test
    void aShapeThePageLowersKeepsItsPositionAndCarriesNothing() throws Exception {
        // A checkbox taller than its text reaches below the baseline, where no empty rows
        // can take it: Word moves it by w:position, and the picture is its own size.
        try (XWPFDocument document = export(page -> page.addParagraph(p -> p
                .checkbox(16, true, INK).inlineText(" Signed")))) {
            XWPFRun box = document.getParagraphs().get(0).getRuns().get(0);

            assertThat(position(box)).isNegative();
            assertThat(Units.toPoints(box.getEmbeddedPictures().get(0).getCTPicture().getSpPr().getXfrm().getExt().getCy()))
                    .isLessThan(16 + 3);
        }
    }

    @Test
    void anAuthorsOwnPictureIsWrittenAsGivenAndMovedByPosition() throws Exception {
        byte[] png = pngOf(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB));
        try (XWPFDocument document = export(page -> page.addParagraph(p -> p
                .inlineText("A ")
                .inlineImage(com.demcha.compose.document.image.DocumentImageData.fromBytes(png), 4, 4,
                        com.demcha.compose.document.node.InlineImageAlignment.BASELINE, 3, null)))) {
            XWPFRun picture = document.getParagraphs().get(0).getRuns().get(1);

            assertThat(position(picture)).isEqualTo(6);
            assertThat(picture.getEmbeddedPictures().get(0).getPictureData().getData()).isEqualTo(png);
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
            assertThat(position(document.getParagraphs().get(0).getRuns().get(1)))
                    .isEqualTo(Math.round(-(1 + DocxShapePictures.EDGE) * 2));
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
        // Each corner by its own radius: bottom-left 1 of 10 wide, bottom-right 3.
        assertThat(DocxShapePictures.unitSegments(new ShapeOutline.RoundedRectanglePerCorner(10, 10,
                new com.demcha.compose.document.style.DocumentCornerRadius(0, 0, 3, 1))))
                .startsWith(DocumentPathSegment.moveTo(0.1, 0), DocumentPathSegment.lineTo(0.7, 0));
        List<DocumentPathSegment> drawn = List.of(DocumentPathSegment.moveTo(0, 0),
                DocumentPathSegment.lineTo(1, 1), DocumentPathSegment.close());
        assertThat(DocxShapePictures.unitSegments(new ShapeOutline.Path(10, 10, drawn))).isEqualTo(drawn);
    }

    private static byte[] pngOf(BufferedImage image) throws Exception {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return bytes.toByteArray();
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
