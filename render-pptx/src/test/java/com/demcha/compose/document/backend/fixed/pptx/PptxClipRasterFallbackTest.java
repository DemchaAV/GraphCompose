package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.EllipseBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ShapeClipBeginPayload;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The clip raster fallback: a clipped composite renders through the PDF
 * backend into one transparent picture anchored on the clip bounds —
 * pixel-exact clipping in a format that cannot express it — while disabling
 * the fallback restores the unclipped vector rendering.
 */
class PptxClipRasterFallbackTest {

    private static DocumentSession composeClippedBadge() {
        DocumentSession session = GraphCompose.document()
                .pageSize(300, 240)
                .margin(DocumentInsets.of(20))
                .create();
        session.add(new ShapeContainerBuilder()
                .ellipse(70, 70)
                .layer(new EllipseBuilder().circle(50)
                        .fillColor(DocumentColor.ROYAL_BLUE).build())
                .build());
        return session;
    }

    @Test
    void clippedCompositeRasterizesToOneTransparentPictureOnTheClipBounds() throws Exception {
        try (DocumentSession session = composeClippedBadge()) {
            LayoutGraph graph = session.render(new GraphCapturingBackend());
            byte[] pptx = session.render(new PptxFixedLayoutBackend());

            PlacedFragment clipFragment = graph.fragments().stream()
                    .filter(fragment -> fragment.payload() instanceof ShapeClipBeginPayload)
                    .findFirst().orElseThrow();
            double canvasHeight = graph.canvas().height();

            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                List<XSLFShape> shapes = show.getSlides().get(0).getShapes();
                List<XSLFPictureShape> composites = shapes.stream()
                        .filter(shape -> "GraphCompose Clipped Composite".equals(shape.getShapeName()))
                        .map(XSLFPictureShape.class::cast)
                        .toList();
                assertThat(composites).hasSize(1);
                XSLFPictureShape composite = composites.get(0);

                Rectangle2D anchor = composite.getAnchor();
                assertThat(anchor.getX()).isCloseTo(clipFragment.x(),
                        org.assertj.core.data.Offset.offset(0.5));
                assertThat(anchor.getY()).isCloseTo(
                        canvasHeight - clipFragment.y() - clipFragment.height(),
                        org.assertj.core.data.Offset.offset(0.5));
                assertThat(anchor.getWidth()).isCloseTo(clipFragment.width(),
                        org.assertj.core.data.Offset.offset(0.5));

                BufferedImage bitmap = ImageIO.read(
                        new ByteArrayInputStream(composite.getPictureData().getData()));
                assertThat(bitmap.getColorModel().hasAlpha())
                        .as("the composite must keep its transparent background").isTrue();
                // Centre pixel carries the layer fill; the top-left corner lies
                // outside the ellipse outline and must stay fully transparent —
                // the clip actually cut the layer.
                int centre = bitmap.getRGB(bitmap.getWidth() / 2, bitmap.getHeight() / 2);
                int corner = bitmap.getRGB(1, 1);
                assertThat((centre >>> 24)).as("centre opaque").isGreaterThan(200);
                assertThat((corner >>> 24)).as("corner transparent").isLessThan(30);

                // No vector ellipse for the clipped layer leaks next to the
                // picture (the unfilled, unstroked outline draws nothing).
                assertThat(shapes.stream()
                        .filter(shape -> shape instanceof org.apache.poi.xslf.usermodel.XSLFAutoShape)
                        .count())
                        .as("the clipped layer must not leak as a vector shape")
                        .isZero();
            }
        }
    }

    @Test
    void disablingTheFallbackRendersUnclippedVectors() throws Exception {
        try (DocumentSession session = composeClippedBadge()) {
            byte[] pptx = session.render(
                    PptxFixedLayoutBackend.builder().clipRasterFallback(false).build());
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                List<XSLFShape> shapes = show.getSlides().get(0).getShapes();
                assertThat(shapes)
                        .noneMatch(shape -> "GraphCompose Clipped Composite"
                                .equals(shape.getShapeName()));
                assertThat(shapes.stream()
                        .filter(org.apache.poi.xslf.usermodel.XSLFAutoShape.class::isInstance)
                        .count())
                        .as("the unclipped layer stays a vector shape")
                        .isEqualTo(1);
            }
        }
    }
}
