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
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
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
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The remaining vector payloads land at graph coordinates: image fit modes
 * (including the COVER source crop), barcodes as freeform shapes, polygon and
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
    void barcodeRendersAsFreeformsOnTheFragmentBoxAndScans() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 200)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(new BarcodeBuilder().data("graphcompose").qrCode().size(60, 60).build());
            LayoutGraph graph = session.render(new GraphCapturingBackend());
            byte[] pptx = session.render(new PptxFixedLayoutBackend());
            PlacedFragment fragment = barcodeFragment(graph);
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                List<XSLFShape> shapes = show.getSlides().get(0).getShapes();
                assertThat(shapes).noneMatch(XSLFPictureShape.class::isInstance);
                List<XSLFFreeformShape> freeforms = shapes.stream()
                        .filter(XSLFFreeformShape.class::isInstance)
                        .map(XSLFFreeformShape.class::cast)
                        .toList();
                assertThat(freeforms).as("background and dark cells").hasSize(2);
                assertRect(freeforms.get(0).getAnchor(),
                        fragment.x(),
                        graph.canvas().height() - fragment.y() - fragment.height(),
                        fragment.width(), fragment.height());

                assertThat(decodeQr(rasterise(show, graph))).isEqualTo("graphcompose");
            }
        }
    }

    @Test
    void aTransparentBarcodeForegroundCutsTheCellsOutOfTheBackground() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 200)
                .margin(DocumentInsets.of(20))
                .pageBackground(new Color(30, 60, 120))
                .create()) {
            session.add(new BarcodeBuilder().data("knockout").qrCode().size(80, 80)
                    .foreground(new Color(0, 0, 0, 0)).background(Color.WHITE).build());
            LayoutGraph graph = session.render(new GraphCapturingBackend());
            byte[] pptx = session.render(new PptxFixedLayoutBackend());
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                // The page shows through the cells, so the symbol still reads.
                assertThat(decodeQr(rasterise(show, graph))).isEqualTo("knockout");
            }
        }
    }

    @Test
    void aTranslucentBarcodeForegroundCompositesWithTheSlideNotTheBackground() throws Exception {
        Color page = new Color(220, 235, 250);
        Color background = new Color(255, 255, 0, 90);
        Color foreground = new Color(200, 30, 30, 120);
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 200)
                .margin(DocumentInsets.of(20))
                .pageBackground(page)
                .create()) {
            session.add(new BarcodeBuilder().data("layers").qrCode().size(80, 80)
                    .foreground(foreground).background(background).build());
            LayoutGraph graph = session.render(new GraphCapturingBackend());
            byte[] pptx = session.render(new PptxFixedLayoutBackend());
            PlacedFragment fragment = barcodeFragment(graph);
            BitMatrix matrix = new MultiFormatWriter().encode("layers", BarcodeFormat.QR_CODE, 200, 200,
                    Map.of(EncodeHintType.CHARACTER_SET, "UTF-8", EncodeHintType.MARGIN, 0));
            // The top-left finder pattern: its first row is seven dark modules, the ring
            // inside it one light module wide.
            int[] corner = matrix.getTopLeftOnBit();
            int module = 0;
            while (matrix.get(corner[0] + module, corner[1])) {
                module++;
            }
            module /= 7;
            double cellWidth = fragment.width() / matrix.getWidth();
            double cellHeight = fragment.height() / matrix.getHeight();
            double top = graph.canvas().height() - fragment.y() - fragment.height();
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                BufferedImage slide = rasterise(show, graph);
                Color dark = sample(slide, fragment.x() + (corner[0] + module * 0.5) * cellWidth,
                        top + (corner[1] + module * 0.5) * cellHeight);
                Color light = sample(slide, fragment.x() + (corner[0] + module * 1.5) * cellWidth,
                        top + (corner[1] + module * 1.5) * cellHeight);

                // A cell is foreground or background, as in a bitmap of the matrix, so
                // each colour lands on the slide alone.
                assertThat(close(dark, over(foreground, page))).as("dark cell %s", dark).isTrue();
                assertThat(close(light, over(background, page))).as("light cell %s", light).isTrue();
            }
        }
    }

    private static Color sample(BufferedImage slide, double pointX, double pointY) {
        return new Color(slide.getRGB((int) Math.floor(pointX * 4), (int) Math.floor(pointY * 4)));
    }

    private static Color over(Color top, Color below) {
        double alpha = top.getAlpha() / 255.0;
        return new Color(
                (int) Math.round(top.getRed() * alpha + below.getRed() * (1 - alpha)),
                (int) Math.round(top.getGreen() * alpha + below.getGreen() * (1 - alpha)),
                (int) Math.round(top.getBlue() * alpha + below.getBlue() * (1 - alpha)));
    }

    private static boolean close(Color actual, Color expected) {
        return Math.abs(actual.getRed() - expected.getRed()) <= 6
                && Math.abs(actual.getGreen() - expected.getGreen()) <= 6
                && Math.abs(actual.getBlue() - expected.getBlue()) <= 6;
    }

    private static PlacedFragment barcodeFragment(LayoutGraph graph) {
        return graph.fragments().stream()
                .filter(candidate -> candidate.payload().getClass().getSimpleName()
                        .equals("BarcodeFragmentPayload"))
                .findFirst().orElseThrow();
    }

    private static BufferedImage rasterise(XMLSlideShow show, LayoutGraph graph) {
        double scale = 4;
        BufferedImage image = new BufferedImage(
                (int) Math.round(graph.canvas().width() * scale),
                (int) Math.round(graph.canvas().height() * scale),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.scale(scale, scale);
            show.getSlides().get(0).draw(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static String decodeQr(BufferedImage image) throws Exception {
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        return new QRCodeReader().decode(
                new BinaryBitmap(new HybridBinarizer(
                        new RGBLuminanceSource(image.getWidth(), image.getHeight(), pixels))),
                Map.of(DecodeHintType.TRY_HARDER, Boolean.TRUE)).getText();
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
    void gradientBoundaryStopsArePinnedToTheDomainLikeThePdfShading() throws Exception {
        try (XMLSlideShow show = new XMLSlideShow();
             PdfMeasurementResources measurement = PdfMeasurementResources.open(List.of())) {
            PptxRenderSession session = new PptxRenderSession(show, 300, 200, 1);
            PptxRenderEnvironment environment = new PptxRenderEnvironment(
                    show, session, 0, 200, measurement.fontLibrary(), List.of());

            // The PDF shading functions ignore the first/last stop offsets
            // (they span the full domain); the deck must shade identically,
            // so 0.3 / 0.5 / 0.9 lands as 0 / 0.5 / 1.
            PathFragmentPayload ramp = new PathFragmentPayload(
                    List.of(new DocumentPathSegment.MoveTo(0, 0),
                            new DocumentPathSegment.LineTo(1, 0),
                            new DocumentPathSegment.LineTo(1, 1),
                            new DocumentPathSegment.Close()),
                    null,
                    new DocumentPaint.Linear(List.of(
                            new DocumentPaint.Stop(0.3, DocumentColor.ROYAL_BLUE),
                            new DocumentPaint.Stop(0.5, DocumentColor.ORANGE),
                            new DocumentPaint.Stop(0.9, DocumentColor.DARK_GRAY)), 0.0),
                    null, null, null, null, null, null, null);
            new com.demcha.compose.document.backend.fixed.pptx.handlers
                    .PptxPathFragmentRenderHandler()
                    .render(new PlacedFragment("root/ramp", 0, 0, 40, 30, 120, 40, null, null,
                            ramp), ramp, environment);

            var properties = ((CTShape) show.getSlides().get(0).getShapes().get(0).getXmlObject()).getSpPr();
            var stopArray = properties.getGradFill().getGsLst().getGsArray();
            assertThat(stopArray.length).isEqualTo(3);
            assertThat(stopArray[0].getPos()).isEqualTo(0);
            assertThat(stopArray[1].getPos()).isEqualTo(50000);
            assertThat(stopArray[2].getPos()).isEqualTo(100000);
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
