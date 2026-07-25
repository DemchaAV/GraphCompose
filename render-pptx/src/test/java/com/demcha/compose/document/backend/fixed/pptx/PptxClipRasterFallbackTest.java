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
                assertThat(anchor.getHeight()).isCloseTo(clipFragment.height(),
                        org.assertj.core.data.Offset.offset(0.5));

                BufferedImage bitmap = ImageIO.read(
                        new ByteArrayInputStream(composite.getPictureData().getData()));
                assertThat(bitmap.getColorModel().hasAlpha())
                        .as("the composite must keep its transparent background").isTrue();
                // The layer circle (top-left aligned, r=25pt at centre 25,25)
                // covers point (10,10)pt, but that point lies OUTSIDE the
                // container ellipse (centre 35,35, r=35) — so it is opaque in
                // an unclipped render and transparent only when the clip
                // actually cut the layer. The circle centre stays opaque.
                double pixelsPerPoint = bitmap.getWidth() / clipFragment.width();
                int cut = bitmap.getRGB(
                        (int) Math.round(10 * pixelsPerPoint),
                        (int) Math.round(10 * pixelsPerPoint));
                int inside = bitmap.getRGB(
                        (int) Math.round(25 * pixelsPerPoint),
                        (int) Math.round(25 * pixelsPerPoint));
                assertThat((inside >>> 24)).as("circle centre opaque").isGreaterThan(200);
                assertThat((cut >>> 24))
                        .as("point inside the layer but outside the outline must be clipped away")
                        .isLessThan(30);

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
    void clipInsideATransformStaysInsideTheRotatedGroup() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 240)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(new ShapeContainerBuilder()
                    .ellipse(70, 70)
                    .layer(new EllipseBuilder().circle(50)
                            .fillColor(DocumentColor.ROYAL_BLUE).build())
                    .transform(new com.demcha.compose.document.style.DocumentTransform(30, 1, 1))
                    .build());
            byte[] pptx = session.render(new PptxFixedLayoutBackend());
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                org.apache.poi.xslf.usermodel.XSLFGroupShape group =
                        show.getSlides().get(0).getShapes().stream()
                                .filter(org.apache.poi.xslf.usermodel.XSLFGroupShape.class::isInstance)
                                .map(org.apache.poi.xslf.usermodel.XSLFGroupShape.class::cast)
                                .findFirst().orElseThrow();
                assertThat(group.getRotation())
                        .as("the enclosing rotation applies to the rasterized clip")
                        .isEqualTo(30.0);
                assertThat(group.getShapes())
                        .anyMatch(shape -> "GraphCompose Clipped Composite"
                                .equals(shape.getShapeName()));
            }
        }
    }

    @Test
    void nestedClipOfADifferentOwnerIsConsumedIntoOneComposite() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(320, 260)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(new ShapeContainerBuilder()
                    .ellipse(90, 90)
                    .layer(new ShapeContainerBuilder()
                            .ellipse(50, 50)
                            .layer(new EllipseBuilder().circle(40)
                                    .fillColor(DocumentColor.ORANGE).build())
                            .build())
                    .build());
            byte[] pptx = session.render(new PptxFixedLayoutBackend());
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                List<XSLFShape> shapes = show.getSlides().get(0).getShapes();
                assertThat(shapes.stream()
                        .filter(shape -> "GraphCompose Clipped Composite"
                                .equals(shape.getShapeName()))
                        .count())
                        .as("the outer region swallows the inner clip; the PDF "
                                + "sub-render applies both clips inside one picture")
                        .isEqualTo(1);
            }
        }
    }

    @Test
    void aProvablyNoOpClipStaysNativeAndEditable() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 240)
                .margin(DocumentInsets.of(20))
                .create()) {
            // A defensive clip: the centered circle never reaches the card's
            // corners, so clipping removes no ink and the raster fallback is
            // skipped — the card and its content stay editable vector shapes.
            session.add(new ShapeContainerBuilder()
                    .outline(new com.demcha.compose.document.style.ShapeOutline
                            .RoundedRectangle(80, 44, 8))
                    .clipPolicy(com.demcha.compose.document.style.ClipPolicy.CLIP_PATH)
                    .fillColor(DocumentColor.LIGHT_GRAY)
                    .center(new EllipseBuilder().circle(16)
                            .fillColor(DocumentColor.ROYAL_BLUE).build())
                    .build());
            byte[] pptx = session.render(new PptxFixedLayoutBackend());
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                List<XSLFShape> shapes = show.getSlides().get(0).getShapes();
                assertThat(shapes)
                        .noneMatch(shape -> "GraphCompose Clipped Composite"
                                .equals(shape.getShapeName()));
                assertThat(shapes.stream()
                        .filter(org.apache.poi.xslf.usermodel.XSLFAutoShape.class::isInstance)
                        .count())
                        .as("card fill and its layer circle render as native shapes")
                        .isEqualTo(2);
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

    @Test
    void aClipRegionLargerThanTheRasterTargetStillRendersAtLeastAtNativeResolution() throws Exception {
        // The raster scale aims for a 2048px long edge. A clip box WIDER than
        // 2048pt drives that ratio below 1.0, and without a floor the picture is
        // downscaled while still being anchored at the full clip size — an
        // A0-scale composite landing at ~44 DPI, visibly blurry. Every other clip
        // test uses a page a few hundred points wide, where the ratio saturates at
        // the upscale cap, so this branch was never exercised.
        double pageWidth = 3400;
        double pageHeight = 2400;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(pageWidth, pageHeight)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(new ShapeContainerBuilder()
                    .ellipse(3000, 2000)
                    .layer(new EllipseBuilder().circle(2600)
                            .fillColor(DocumentColor.ROYAL_BLUE).build())
                    .build());

            LayoutGraph graph = session.render(new GraphCapturingBackend());
            byte[] pptx = session.render(new PptxFixedLayoutBackend());

            PlacedFragment clipFragment = graph.fragments().stream()
                    .filter(fragment -> fragment.payload() instanceof ShapeClipBeginPayload)
                    .findFirst().orElseThrow();
            assertThat(clipFragment.width())
                    .as("the fixture must actually exceed the raster target, or it proves nothing")
                    .isGreaterThan(2048.0);

            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                XSLFPictureShape composite = show.getSlides().get(0).getShapes().stream()
                        .filter(shape -> "GraphCompose Clipped Composite".equals(shape.getShapeName()))
                        .map(XSLFPictureShape.class::cast)
                        .findFirst().orElseThrow();
                BufferedImage bitmap = ImageIO.read(
                        new ByteArrayInputStream(composite.getPictureData().getData()));

                assertThat(bitmap.getWidth() / clipFragment.width())
                        .as("a rasterized clip must never be published below native 72 DPI")
                        .isGreaterThanOrEqualTo(1.0);
            }
        }
    }
}
