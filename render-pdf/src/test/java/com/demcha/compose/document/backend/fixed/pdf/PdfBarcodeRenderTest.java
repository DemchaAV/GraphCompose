package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.BarcodeBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.BarcodeFragmentPayload;
import com.demcha.compose.document.layout.payloads.ShapeFragmentPayload;
import com.demcha.compose.document.node.DocumentBarcodeType;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.testing.VisualTestOutputs;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A barcode reaches the page as the symbol that was asked for. The page is
 * rasterised and read like a printout: every format decodes back to its content,
 * every cell of the symbol lands where the encoded matrix puts it and in the
 * chosen colour, translucent colours layer page, background and foreground, a
 * transparent background shows the page through, a transparent foreground cuts the
 * cells out of the background, and a translucent colour does not leak into what is
 * drawn next. The assertions read the written PDF only.
 */
class PdfBarcodeRenderTest {

    private static final float DPI = 600f;
    // Two pixels per point. A box side of 100 points or more asks ZXing for a matrix
    // twice as long, so on that axis every cell edge of these integer-point boxes falls
    // on a whole pixel. PDF417 keeps its own matrix size and reads without that.
    private static final float SCAN_DPI = 144f;
    private static final float POINTS_PER_INCH = 72f;

    // EAN and UPC readers locate the start guard by the quiet zone in front of it,
    // and the builder's default quiet zone is zero, so those cases ask for one.
    private static final int UPC_EAN_QUIET_ZONE = 9;

    static Stream<Arguments> barcodes() {
        return Stream.of(
                Arguments.of(BarcodeFormat.QR_CODE, "https://github.com/DemchaAV/GraphCompose",
                        (Consumer<BarcodeBuilder>) b -> b.qrCode().size(108, 108)),
                Arguments.of(BarcodeFormat.CODE_128, "GC-BENCH-2026-04-13",
                        (Consumer<BarcodeBuilder>) b -> b.code128().size(280, 68).quietZone(4)),
                Arguments.of(BarcodeFormat.CODE_39, "CODE39TEST",
                        (Consumer<BarcodeBuilder>) b -> b.code39().size(240, 60)),
                Arguments.of(BarcodeFormat.EAN_13, "5901234123457",
                        (Consumer<BarcodeBuilder>) b -> b.ean13().size(190, 80).quietZone(UPC_EAN_QUIET_ZONE)),
                Arguments.of(BarcodeFormat.EAN_8, "96385074",
                        (Consumer<BarcodeBuilder>) b -> b.ean8().size(140, 70).quietZone(UPC_EAN_QUIET_ZONE)),
                Arguments.of(BarcodeFormat.UPC_A, "036000291452",
                        (Consumer<BarcodeBuilder>) b -> b.type(DocumentBarcodeType.UPC_A).size(190, 80)
                                .quietZone(UPC_EAN_QUIET_ZONE)),
                Arguments.of(BarcodeFormat.PDF_417, "PDF417 payload",
                        (Consumer<BarcodeBuilder>) b -> b.type(DocumentBarcodeType.PDF_417).size(260, 90)),
                Arguments.of(BarcodeFormat.DATA_MATRIX, "DM-001",
                        (Consumer<BarcodeBuilder>) b -> b.type(DocumentBarcodeType.DATA_MATRIX).size(100, 100)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("barcodes")
    void theRenderedPageDecodesToTheBarcodeContent(BarcodeFormat format,
                                                   String content,
                                                   Consumer<BarcodeBuilder> shape) throws Exception {
        byte[] pdf = renderPdf(flow -> flow.addBarcode(b -> shape.accept(b.data(content))));
        Files.write(VisualTestOutputs.preparePdf(format.name().toLowerCase(), "barcode-render"), pdf);

        // Read where the module edges fall on whole pixels. ZXing does not locate a Data
        // Matrix on a page this size, and its pure-barcode fallback divides the symbol's
        // pixel extent by the module width in whole pixels, so the partly covered pixels
        // of an edge between pixels can cost it a module. That is why the Data Matrix box
        // is 100 points: below that the 200-cell floor puts its edges between pixels.
        Result decoded = decode(rasterise(pdf, SCAN_DPI), format);

        assertThat(decoded.getBarcodeFormat()).isEqualTo(format);
        assertThat(decoded.getText()).isEqualTo(content);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("barcodes")
    void aBarcodeIsDrawnAsPathsNotAnImage(BarcodeFormat format,
                                          String content,
                                          Consumer<BarcodeBuilder> shape) throws Exception {
        byte[] pdf = renderPdf(flow -> flow.addBarcode(b -> shape.accept(b.data(content))));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDPage page = document.getPage(0);
            assertThat(imagesOn(page.getResources())).as("image XObjects on the page").isZero();
            // The page background is one rectangle; the bars or modules are many more.
            assertThat(operatorNames(page).stream().filter("re"::equals).count())
                    .as("rectangles on the page").isGreaterThan(10);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("gridCases")
    void everyCellLandsWhereTheMatrixPutsIt(BarcodeFormat format, String content, int margin,
                                            Consumer<BarcodeBuilder> shape) throws Exception {
        Color foreground = new Color(18, 40, 74);
        Color background = new Color(250, 236, 200);
        Rendered rendered = render(flow -> flow.addBarcode(b -> shape.accept(
                b.data(content).quietZone(margin).foreground(foreground).background(background))));
        PlacedFragment box = rendered.barcodeBox();
        BitMatrix matrix = expectedMatrix(format, content, margin, box);

        assertThat(mismatchedCells(rendered, box, matrix, foreground, background))
                .as("cells drawn in the wrong colour or place").isZero();
    }

    static Stream<Arguments> gridCases() {
        return Stream.of(
                Arguments.of(BarcodeFormat.QR_CODE, "GC-2026-001", 1,
                        (Consumer<BarcodeBuilder>) b -> b.qrCode().size(90, 90)),
                Arguments.of(BarcodeFormat.QR_CODE, "GC-2026-001", 0,
                        (Consumer<BarcodeBuilder>) b -> b.qrCode().size(160, 60)),
                Arguments.of(BarcodeFormat.DATA_MATRIX, "DM-001", 0,
                        (Consumer<BarcodeBuilder>) b -> b.type(DocumentBarcodeType.DATA_MATRIX).size(80, 80)),
                Arguments.of(BarcodeFormat.PDF_417, "PDF417 payload", 0,
                        (Consumer<BarcodeBuilder>) b -> b.type(DocumentBarcodeType.PDF_417).size(260, 90)),
                Arguments.of(BarcodeFormat.CODE_128, "GC-BENCH-2026", 4,
                        (Consumer<BarcodeBuilder>) b -> b.code128().size(240, 60)),
                Arguments.of(BarcodeFormat.CODE_39, "CODE39TEST", 1,
                        (Consumer<BarcodeBuilder>) b -> b.code39().size(240, 60)),
                Arguments.of(BarcodeFormat.EAN_13, "5901234123457", 9,
                        (Consumer<BarcodeBuilder>) b -> b.ean13().size(190, 80)),
                Arguments.of(BarcodeFormat.EAN_8, "96385074", 0,
                        (Consumer<BarcodeBuilder>) b -> b.ean8().size(140, 70)),
                Arguments.of(BarcodeFormat.UPC_A, "036000291452", 9,
                        (Consumer<BarcodeBuilder>) b -> b.type(DocumentBarcodeType.UPC_A).size(190, 80)));
    }

    @Test
    void translucentColoursBlendAsLayers() throws Exception {
        Color page = Color.WHITE;
        Color background = new Color(255, 255, 0, 90);
        Color foreground = new Color(200, 30, 30, 120);
        Rendered rendered = render(page, flow -> flow.addBarcode(b -> b.qrCode().data("layers").size(90, 90)
                .foreground(foreground).background(background)));
        PlacedFragment box = rendered.barcodeBox();
        BitMatrix matrix = expectedMatrix(BarcodeFormat.QR_CODE, "layers", 0, box);

        // The background is painted over the page and the foreground over the background.
        Color light = over(background, page);
        Color dark = over(foreground, light);

        assertThat(mismatchedCells(rendered, box, matrix, dark, light))
                .as("cells not blended page < background < foreground").isZero();
    }

    @Test
    void aTransparentForegroundCutsTheCellsOutOfTheBackground() throws Exception {
        Color page = new Color(30, 60, 120);
        Color background = Color.WHITE;
        Rendered rendered = render(page, flow -> flow.addBarcode(b -> b.qrCode().data("knockout").size(90, 90)
                .foreground(new Color(0, 0, 0, 0)).background(background)));
        PlacedFragment box = rendered.barcodeBox();
        BitMatrix matrix = expectedMatrix(BarcodeFormat.QR_CODE, "knockout", 0, box);

        assertThat(mismatchedCells(rendered, box, matrix, page, background))
                .as("dark cells should show the page through the background").isZero();
        assertThat(decode(rendered.page(), BarcodeFormat.QR_CODE).getText()).isEqualTo("knockout");
    }

    @Test
    void aTransparentBackgroundShowsThePageThrough() throws Exception {
        Color page = new Color(220, 240, 220);
        Color foreground = new Color(200, 30, 30);
        Rendered rendered = render(page, flow -> flow.addBarcode(b -> b.qrCode().data("transparent").size(90, 90)
                .quietZone(2).foreground(foreground).background(new Color(255, 255, 255, 0))));
        PlacedFragment box = rendered.barcodeBox();

        // The quiet zone is light, so the box corner shows whatever is under the barcode.
        assertThat(rendered.pixel(box.x() + 1, rendered.pageHeight() - box.y() - box.height() + 1))
                .satisfies(pixel -> assertThat(close(pixel, page)).as("page colour at %s", pixel).isTrue());
        assertThat(decode(rendered.page(), BarcodeFormat.QR_CODE).getText()).isEqualTo("transparent");
    }

    @Test
    void translucentColoursStayInsideTheBarcode() throws Exception {
        Color shape = new Color(30, 90, 200);
        Rendered rendered = render(flow -> flow
                .addBarcode(b -> b.qrCode().data("alpha").size(80, 80)
                        .foreground(new Color(200, 30, 30, 120))
                        .background(new Color(255, 255, 0, 90)))
                .addShape(60, 30, DocumentColor.of(shape)));
        // The page background is a shape fragment too; the drawn shape is the 60 x 30 one.
        PlacedFragment square = rendered.fragments().stream()
                .filter(fragment -> fragment.payload() instanceof ShapeFragmentPayload)
                .filter(fragment -> fragment.width() == 60 && fragment.height() == 30)
                .findFirst().orElseThrow();

        Color centre = rendered.pixel(square.x() + 30, rendered.pageHeight() - square.y() - 15);

        assertThat(close(centre, shape))
                .as("opaque shape after the barcode at %s, got %s", square, centre).isTrue();
    }

    @Test
    void anOpaqueForegroundStaysOpaqueOverATranslucentBackground() throws Exception {
        Color foreground = new Color(18, 40, 74);
        Rendered rendered = render(flow -> flow.addBarcode(b -> b.qrCode().data("opaque").size(90, 90)
                .foreground(foreground).background(new Color(255, 255, 0, 90))));
        PlacedFragment box = rendered.barcodeBox();
        BitMatrix matrix = expectedMatrix(BarcodeFormat.QR_CODE, "opaque", 0, box);
        // The top-left finder pattern's corner module spans several cells; sample inside it.
        int[] corner = matrix.getTopLeftOnBit();

        Color cell = rendered.pixel(
                box.x() + (corner[0] + 1.5) * box.width() / matrix.getWidth(),
                rendered.pageHeight() - box.y() - box.height() + (corner[1] + 1.5) * box.height() / matrix.getHeight());

        assertThat(close(cell, foreground)).as("dark module colour, got %s", cell).isTrue();
    }

    /** Samples every matrix cell at its centre and counts those not in the expected colour. */
    private static int mismatchedCells(Rendered rendered, PlacedFragment box, BitMatrix matrix,
                                       Color dark, Color light) {
        int mismatches = 0;
        for (int row = 0; row < matrix.getHeight(); row++) {
            for (int column = 0; column < matrix.getWidth(); column++) {
                double pointX = box.x() + (column + 0.5) * box.width() / matrix.getWidth();
                double pointYFromTop = rendered.pageHeight() - box.y() - box.height()
                        + (row + 0.5) * box.height() / matrix.getHeight();
                Color expected = matrix.get(column, row) ? dark : light;
                if (!close(rendered.pixel(pointX, pointYFromTop), expected)) {
                    mismatches++;
                }
            }
        }
        return mismatches;
    }

    /** Source-over compositing of a possibly translucent colour onto an opaque one. */
    private static Color over(Color top, Color below) {
        double alpha = top.getAlpha() / 255.0;
        return new Color(
                (int) Math.round(top.getRed() * alpha + below.getRed() * (1 - alpha)),
                (int) Math.round(top.getGreen() * alpha + below.getGreen() * (1 - alpha)),
                (int) Math.round(top.getBlue() * alpha + below.getBlue() * (1 - alpha)));
    }

    private static BitMatrix expectedMatrix(BarcodeFormat format, String content, int margin,
                                            PlacedFragment box) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, margin);
        return new MultiFormatWriter().encode(content, format,
                Math.max((int) box.width() * 2, 200), Math.max((int) box.height() * 2, 200), hints);
    }

    private static Rendered render(Consumer<PageFlowBuilder> content) throws Exception {
        return render(Color.WHITE, content);
    }

    private static Rendered render(Color pageColor, Consumer<PageFlowBuilder> content) throws Exception {
        try (DocumentSession session = session(pageColor, content)) {
            List<PlacedFragment> fragments = session.layoutGraph().fragments();
            byte[] pdf = session.toPdfBytes();
            try (PDDocument document = Loader.loadPDF(pdf)) {
                return new Rendered(pdf, rasterise(pdf, DPI), fragments, document.getPage(0).getMediaBox().getHeight());
            }
        }
    }

    private static byte[] renderPdf(Consumer<PageFlowBuilder> content) throws Exception {
        try (DocumentSession session = session(Color.WHITE, content)) {
            return session.toPdfBytes();
        }
    }

    private static DocumentSession session(Color pageColor, Consumer<PageFlowBuilder> content) {
        DocumentSession session = GraphCompose.document()
                .pageSize(320, 220)
                .margin(DocumentInsets.of(12))
                .pageBackground(pageColor)
                .create();
        PageFlowBuilder flow = session.pageFlow();
        content.accept(flow);
        flow.build();
        return session;
    }

    private static BufferedImage rasterise(byte[] pdf, float dpi) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFRenderer(document).renderImageWithDPI(0, dpi, ImageType.RGB);
        }
    }

    private record Rendered(byte[] pdf, BufferedImage page, List<PlacedFragment> fragments, float pageHeight) {

        PlacedFragment barcodeBox() {
            return fragments.stream()
                    .filter(fragment -> fragment.payload() instanceof BarcodeFragmentPayload)
                    .findFirst().orElseThrow();
        }

        Color pixel(double pointX, double pointYFromTop) {
            int x = (int) Math.floor(pointX * DPI / POINTS_PER_INCH);
            int y = (int) Math.floor(pointYFromTop * DPI / POINTS_PER_INCH);
            return new Color(page.getRGB(x, y));
        }
    }

    private static boolean close(Color actual, Color expected) {
        return Math.abs(actual.getRed() - expected.getRed()) <= 6
                && Math.abs(actual.getGreen() - expected.getGreen()) <= 6
                && Math.abs(actual.getBlue() - expected.getBlue()) <= 6;
    }

    private static Result decode(BufferedImage image, BarcodeFormat format) throws Exception {
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(
                new RGBLuminanceSource(image.getWidth(), image.getHeight(), pixels)));
        try {
            return new MultiFormatReader().decode(bitmap, Map.of(
                    DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(format),
                    DecodeHintType.TRY_HARDER, Boolean.TRUE));
        } catch (NotFoundException notLocated) {
            // ZXing's Data Matrix detector does not find a symbol on a page this size;
            // the page holds nothing else, so read it as a pure barcode instead.
            return new MultiFormatReader().decode(bitmap, Map.of(
                    DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(format),
                    DecodeHintType.PURE_BARCODE, Boolean.TRUE));
        }
    }

    private static int imagesOn(PDResources resources) throws Exception {
        int images = 0;
        for (COSName name : resources.getXObjectNames()) {
            if (resources.getXObject(name) instanceof PDImageXObject) {
                images++;
            }
        }
        return images;
    }

    private static List<String> operatorNames(PDPage page) throws Exception {
        List<String> names = new ArrayList<>();
        PDFStreamParser parser = new PDFStreamParser(page);
        for (Object token : parser.parse()) {
            if (token instanceof Operator operator) {
                names.add(operator.getName());
            }
        }
        return names;
    }
}
