package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.fixed.pdf.PdfMeasurementResources;
import com.demcha.compose.document.dsl.BarcodeBuilder;
import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.image.DocumentImageFitMode;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ImageFragmentPayload;
import com.demcha.compose.document.layout.payloads.PathFragmentPayload;
import com.demcha.compose.document.layout.payloads.PolygonFragmentPayload;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentPathSegment;
import com.demcha.compose.document.style.DocumentTransform;
import com.demcha.compose.document.style.ShapePoint;
import com.demcha.compose.engine.components.content.shape.Stroke;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFFreeformShape;
import org.apache.poi.xslf.usermodel.XSLFGroupShape;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.drawingml.x2006.main.CTLineProperties;
import org.openxmlformats.schemas.presentationml.x2006.main.CTPicture;
import org.openxmlformats.schemas.presentationml.x2006.main.CTShape;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The remaining vector payloads land at graph coordinates: image fit modes
 * (including the COVER source crop), the ZXing barcode picture, polygon and
 * free-path freeforms with native gradient fills and strokes, and transform
 * markers materializing as rotated, scaled group shapes.
 */
class PptxVectorFragmentsTest {

    private static byte[] twoByOnePng() throws Exception {
        BufferedImage image = new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, Color.RED.getRGB());
        image.setRGB(1, 0, Color.BLUE.getRGB());
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        }
    }

    @Test
    void imageFitModesAnchorAndCropLikeThePdfHandler() throws Exception {
        byte[] png = twoByOnePng();
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .create()) {
            for (DocumentImageFitMode mode : new DocumentImageFitMode[]{
                    DocumentImageFitMode.STRETCH,
                    DocumentImageFitMode.CONTAIN,
                    DocumentImageFitMode.COVER}) {
                session.add(new ImageBuilder().source(png).size(80, 40).fitMode(mode).build());
            }
            LayoutGraph graph = session.render(new GraphCapturingBackend());
            byte[] pptx = session.render(new PptxFixedLayoutBackend());

            List<PlacedFragment> imageFragments = graph.fragments().stream()
                    .filter(fragment -> fragment.payload() instanceof ImageFragmentPayload)
                    .toList();
            assertThat(imageFragments).hasSize(3);
            double canvasHeight = graph.canvas().height();

            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                List<XSLFPictureShape> pictures = show.getSlides().get(0).getShapes().stream()
                        .filter(XSLFPictureShape.class::isInstance)
                        .map(XSLFPictureShape.class::cast)
                        .toList();
                assertThat(pictures).hasSize(3);

                // STRETCH: anchor is exactly the fragment box.
                PlacedFragment stretch = imageFragments.get(0);
                assertRect(pictures.get(0).getAnchor(),
                        stretch.x(), canvasHeight - stretch.y() - stretch.height(),
                        stretch.width(), stretch.height());

                // CONTAIN on a 2:1 source in an 80×40 box fills it exactly
                // (same aspect), so the letter-box equals the fragment box.
                PlacedFragment contain = imageFragments.get(1);
                assertRect(pictures.get(1).getAnchor(),
                        contain.x(), canvasHeight - contain.y() - contain.height(),
                        contain.width(), contain.height());

                // COVER: full box plus a symmetric source crop.
                PlacedFragment cover = imageFragments.get(2);
                assertRect(pictures.get(2).getAnchor(),
                        cover.x(), canvasHeight - cover.y() - cover.height(),
                        cover.width(), cover.height());
                var srcRect = ((CTPicture) pictures.get(2).getXmlObject())
                        .getBlipFill().getSrcRect();
                assertThat(srcRect).as("COVER crops in source space").isNotNull();
            }
        }
    }

    @Test
    void barcodeRendersAsAPictureOnTheFragmentBox() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 200)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(new BarcodeBuilder().data("graphcompose").qrCode().size(60, 60).build());
            LayoutGraph graph = session.render(new GraphCapturingBackend());
            byte[] pptx = session.render(new PptxFixedLayoutBackend());
            PlacedFragment fragment = graph.fragments().stream()
                    .filter(candidate -> candidate.payload().getClass().getSimpleName()
                            .equals("BarcodeFragmentPayload"))
                    .findFirst().orElseThrow();
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                XSLFPictureShape picture = show.getSlides().get(0).getShapes().stream()
                        .filter(XSLFPictureShape.class::isInstance)
                        .map(XSLFPictureShape.class::cast)
                        .findFirst().orElseThrow();
                assertRect(picture.getAnchor(),
                        fragment.x(),
                        graph.canvas().height() - fragment.y() - fragment.height(),
                        fragment.width(), fragment.height());
                assertThat(picture.getPictureData().getData().length).isPositive();
            }
        }
    }

    @Test
    void polygonAndGradientPathRenderAsFreeforms() throws Exception {
        try (XMLSlideShow show = new XMLSlideShow();
             PdfMeasurementResources measurement = PdfMeasurementResources.open(List.of())) {
            PptxRenderSession session = new PptxRenderSession(show, 300, 200, 1);
            PptxRenderEnvironment environment = new PptxRenderEnvironment(
                    show, session, 0, 200, measurement.fontLibrary(), List.of());

            PolygonFragmentPayload triangle = new PolygonFragmentPayload(
                    List.of(new ShapePoint(0, 0), new ShapePoint(1, 0), new ShapePoint(0.5, 1)),
                    Color.ORANGE, new Stroke(Color.BLACK, 1), null, null);
            new com.demcha.compose.document.backend.fixed.pptx.handlers
                    .PptxPolygonFragmentRenderHandler()
                    .render(new PlacedFragment("root/poly", 0, 0, 40, 60, 100, 80, null, null,
                            triangle), triangle, environment);

            PathFragmentPayload wave = new PathFragmentPayload(
                    List.of(new DocumentPathSegment.MoveTo(0, 0.5),
                            new DocumentPathSegment.CubicTo(0.25, 1, 0.75, 0, 1, 0.5)),
                    null,
                    new DocumentPaint.Linear(List.of(
                            new DocumentPaint.Stop(0.0, DocumentColor.ROYAL_BLUE),
                            new DocumentPaint.Stop(1.0, DocumentColor.ORANGE)), 90.0),
                    new Stroke(Color.BLACK, 2),
                    new DocumentPaint.Linear(List.of(
                            new DocumentPaint.Stop(0.0, DocumentColor.ORANGE),
                            new DocumentPaint.Stop(1.0, DocumentColor.ROYAL_BLUE)), 0.0),
                    null, null, null, null, null);
            new com.demcha.compose.document.backend.fixed.pptx.handlers
                    .PptxPathFragmentRenderHandler()
                    .render(new PlacedFragment("root/path", 0, 0, 40, 30, 120, 40, null, null,
                            wave), wave, environment);

            List<XSLFShape> shapes = show.getSlides().get(0).getShapes();
            assertThat(shapes).hasSize(2).allMatch(XSLFFreeformShape.class::isInstance);

            Rectangle2D triangleBounds = shapes.get(0).getAnchor();
            assertRect(triangleBounds, 40, 200 - 60 - 80, 100, 80);

            var pathProperties = ((CTShape) shapes.get(1).getXmlObject()).getSpPr();
            assertThat(pathProperties.isSetGradFill())
                    .as("gradient fill must be stamped as DrawingML gradFill").isTrue();
            assertThat(pathProperties.getGradFill().getGsLst().sizeOfGsArray()).isEqualTo(2);
            // Engine 90 degrees (bottom-to-top, counter-clockwise) is DrawingML
            // 270 degrees clockwise, in sixtieths of a degree.
            assertThat(pathProperties.getGradFill().getLin().getAng()).isEqualTo(270 * 60000);
            CTLineProperties line = pathProperties.getLn();
            assertThat(line.isSetGradFill())
                    .as("gradient stroke must stamp ln/gradFill").isTrue();
            assertThat(line.isSetNoFill())
                    .as("a leftover noFill would make PowerPoint drop the stroke").isFalse();
            assertThat(line.getW()).isEqualTo(2 * 12700);
        }
    }

    @Test
    void transformMarkersBecomeRotatedScaledGroups() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 260)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(new ShapeContainerBuilder()
                    .ellipse(80, 80)
                    .layer(new com.demcha.compose.document.dsl.EllipseBuilder()
                            .circle(40).fillColor(DocumentColor.ROYAL_BLUE).build())
                    .transform(new DocumentTransform(30, 1.5, 1.5))
                    .build());
            LayoutGraph graph = session.render(new GraphCapturingBackend());
            byte[] pptx = session.render(new PptxFixedLayoutBackend());

            PlacedFragment beginFragment = graph.fragments().stream()
                    .filter(candidate -> candidate.payload().getClass().getSimpleName()
                            .equals("TransformBeginPayload"))
                    .findFirst().orElseThrow();
            double canvasHeight = graph.canvas().height();
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                XSLFGroupShape group = show.getSlides().get(0).getShapes().stream()
                        .filter(XSLFGroupShape.class::isInstance)
                        .map(XSLFGroupShape.class::cast)
                        .findFirst().orElseThrow();
                assertThat(group.getRotation()).isEqualTo(30.0);
                assertThat(group.getShapes())
                        .as("the transformed composite draws inside the group")
                        .isNotEmpty();
                Rectangle2D interior = group.getInteriorAnchor();
                assertRect(interior,
                        beginFragment.x(),
                        canvasHeight - beginFragment.y() - beginFragment.height(),
                        beginFragment.width(), beginFragment.height());
                Rectangle2D exterior = group.getAnchor();
                assertThat(exterior.getWidth())
                        .isCloseTo(beginFragment.width() * 1.5,
                                org.assertj.core.data.Offset.offset(0.5));
                assertThat(exterior.getCenterX())
                        .as("scaling keeps the fragment centre as the pivot")
                        .isCloseTo(interior.getCenterX(), org.assertj.core.data.Offset.offset(0.5));
            }
        }
    }

    private static void assertRect(Rectangle2D actual,
                                   double x, double y, double width, double height) {
        assertThat(actual.getX()).isCloseTo(x, org.assertj.core.data.Offset.offset(0.5));
        assertThat(actual.getY()).isCloseTo(y, org.assertj.core.data.Offset.offset(0.5));
        assertThat(actual.getWidth()).isCloseTo(width, org.assertj.core.data.Offset.offset(0.5));
        assertThat(actual.getHeight()).isCloseTo(height, org.assertj.core.data.Offset.offset(0.5));
    }
}
