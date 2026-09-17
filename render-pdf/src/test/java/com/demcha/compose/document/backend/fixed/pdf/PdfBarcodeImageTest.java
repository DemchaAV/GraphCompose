package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.BarcodeBuilder;
import com.demcha.compose.document.node.DocumentBarcodeType;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.testing.VisualTestOutputs;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The barcode embedded in the PDF is the barcode that was asked for: decoding the
 * image XObject a barcode fragment writes gives back its content and format, and
 * the chosen colours — a transparent background included — survive into the file.
 * The assertions read the written PDF only, so they hold whichever way the
 * renderer builds the image.
 */
class PdfBarcodeImageTest {

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
                        (Consumer<BarcodeBuilder>) b -> b.type(DocumentBarcodeType.DATA_MATRIX).size(80, 80)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("barcodes")
    void embeddedImageDecodesToTheBarcodeContent(BarcodeFormat format,
                                                 String content,
                                                 Consumer<BarcodeBuilder> shape) throws Exception {
        byte[] pdf = render(b -> shape.accept(b.data(content)));
        Files.write(VisualTestOutputs.preparePdf(format.name().toLowerCase(), "barcode-image"), pdf);

        BufferedImage image = singleImage(pdf);
        Result decoded = decode(image, format);

        assertThat(decoded.getBarcodeFormat()).isEqualTo(format);
        assertThat(decoded.getText()).isEqualTo(content);
    }

    @Test
    void foregroundAndBackgroundColoursReachTheImage() throws Exception {
        Color foreground = new Color(18, 40, 74);
        Color background = new Color(250, 236, 200);
        byte[] pdf = render(b -> b.qrCode().data("colours").size(90, 90)
                .foreground(foreground).background(background));

        BufferedImage image = singleImage(pdf);

        assertThat(distinctArgb(image))
                .as("a two-colour barcode draws exactly its two colours")
                .containsExactlyInAnyOrder(foreground.getRGB(), background.getRGB());
    }

    @Test
    void transparentBackgroundStaysTransparent() throws Exception {
        Color foreground = new Color(200, 30, 30);
        byte[] pdf = render(b -> b.qrCode().data("transparent").size(90, 90)
                .foreground(foreground).background(new Color(255, 255, 255, 0)));

        BufferedImage image = singleImage(pdf);

        assertThat(distinctAlpha(image))
                .as("the background is see-through and the modules are opaque")
                .containsExactlyInAnyOrder(0, 255);
        assertThat(decode(image, BarcodeFormat.QR_CODE).getText()).isEqualTo("transparent");
    }

    private static byte[] render(Consumer<BarcodeBuilder> barcode) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(320, 200)
                .margin(DocumentInsets.of(12))
                .create()) {
            session.pageFlow().addBarcode(barcode).build();
            return session.toPdfBytes();
        }
    }

    private static BufferedImage singleImage(byte[] pdf) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDResources resources = document.getPage(0).getResources();
            List<PDImageXObject> images = new ArrayList<>();
            for (COSName name : resources.getXObjectNames()) {
                if (resources.getXObject(name) instanceof PDImageXObject image) {
                    images.add(image);
                }
            }
            assertThat(images).as("one barcode, one image XObject").hasSize(1);
            // getImage() applies the soft mask, so alpha reads back as written.
            return images.get(0).getImage();
        }
    }

    private static Result decode(BufferedImage image, BarcodeFormat format) throws Exception {
        int[] pixels = argb(image);
        // Composite onto white before binarising, as a scanner sees a transparent
        // background over paper.
        for (int i = 0; i < pixels.length; i++) {
            if ((pixels[i] >>> 24) == 0) {
                pixels[i] = 0xFFFFFFFF;
            }
        }
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(
                new RGBLuminanceSource(image.getWidth(), image.getHeight(), pixels)));
        return new MultiFormatReader().decode(bitmap, Map.of(
                DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(format),
                DecodeHintType.PURE_BARCODE, Boolean.TRUE,
                DecodeHintType.TRY_HARDER, Boolean.TRUE));
    }

    private static List<Integer> distinctArgb(BufferedImage image) {
        return Arrays.stream(argb(image)).distinct().boxed().toList();
    }

    private static List<Integer> distinctAlpha(BufferedImage image) {
        return Arrays.stream(argb(image)).map(pixel -> pixel >>> 24).distinct().boxed().toList();
    }

    private static int[] argb(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }
}
