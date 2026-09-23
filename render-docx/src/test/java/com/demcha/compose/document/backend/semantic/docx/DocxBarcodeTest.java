package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.engine.components.content.barcode.BarcodeData;
import com.demcha.compose.engine.components.content.barcode.BarcodeType;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBookmark;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A barcode reaches Word as a picture that scans.
 *
 * <p>It was dropped with the geometry-only nodes, so a receipt or a shipping label lost the
 * code a reader scans. The picture is the matrix the PDF and PPTX backends draw.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxBarcodeTest {

    @Test
    void aQrCodeIsAPictureAtItsSizeThatDecodesToItsData() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addBarcode(b -> b.qrCode().data("GC-2026-001").size(80, 80)))) {
            XWPFPicture picture = onlyPicture(document);

            assertThat(picture.getCTPicture().getSpPr().getXfrm().getExt().getCx()).isEqualTo(Units.toEMU(80));
            assertThat(picture.getCTPicture().getSpPr().getXfrm().getExt().getCy()).isEqualTo(Units.toEMU(80));
            assertThat(decode(picture.getPictureData())).isEqualTo("GC-2026-001");
        }
    }

    @Test
    void aLinearBarcodeDecodesToItsDataToo() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addBarcode(b -> b.code128().data("INV-0042").size(160, 40)))) {
            assertThat(decode(onlyPicture(document).getPictureData())).isEqualTo("INV-0042");
        }
    }

    @Test
    void theSymbolIsDrawnInItsOwnColours() throws Exception {
        DocumentColor ink = DocumentColor.rgb(26, 86, 148);
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addBarcode(b -> b.qrCode().data("colour").foreground(ink).size(60, 60)))) {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(onlyPicture(document).getPictureData().getData()));
            boolean inkFound = false;
            for (int y = 0; y < image.getHeight() && !inkFound; y++) {
                for (int x = 0; x < image.getWidth() && !inkFound; x++) {
                    inkFound = (image.getRGB(x, y) & 0xFFFFFF) == 0x1A5694;
                }
            }
            assertThat(inkFound).isTrue();
        }
    }

    @Test
    void theReportSaysItIsAPictureAndNothingIsDropped() throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (DocumentSession session = GraphCompose.document().pageSize(595, 842)
                .margin(DocumentInsets.of(36)).create()) {
            session.pageFlow(page -> page.addBarcode(b -> b.qrCode().data("x").size(50, 50)));
            session.export(new DocxSemanticBackend(report::set));
        }

        assertThat(report.get().bySubject()).containsKey("barcode");
        assertThat(report.get().count(DocxExportReport.Severity.APPROXIMATED)).isEqualTo(1);
        assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).isZero();
    }

    @Test
    void aBarcodesAnchorIsABookmarkOnItsParagraph() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addBarcode(b -> b.qrCode().data("x").size(50, 50).anchor("ticket")))) {
            List<CTBookmark> bookmarks = document.getParagraphs().get(0).getCTP().getBookmarkStartList();

            assertThat(bookmarks).extracting(CTBookmark::getName).containsExactly("ticket");
        }
    }

    @Test
    void theMarginAroundABarcodeIsTheSpaceAroundItsParagraph() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addParagraph(p -> p.text("Above"))
                .addBarcode(b -> b.qrCode().data("x").size(50, 50).margin(DocumentInsets.top(12))))) {
            var spacing = document.getParagraphs().get(1).getCTP().getPPr().getSpacing();

            assertThat(DocxTwips.of(spacing.getBefore())).isEqualTo(12 * 20L);
        }
    }

    @Test
    void thePictureIsTheMatrixThePageDrawsOnePixelACell() throws Exception {
        BarcodeData data = BarcodeData.of("cells", BarcodeType.CODE_128);
        var matrix = com.demcha.compose.document.backend.fixed.pdf.handlers.BarcodeMatrices.encode(data, 150, 40);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(DocxBarcodePictures.png(data, 150, 40)));

        assertThat(image.getWidth()).isEqualTo(matrix.getWidth());
        assertThat(image.getHeight()).isEqualTo(matrix.getHeight());
        for (int x = 0; x < matrix.getWidth(); x++) {
            boolean dark = (image.getRGB(x, matrix.getHeight() / 2) & 0xFFFFFF) == 0;
            assertThat(dark).as("cell " + x).isEqualTo(matrix.get(x, matrix.getHeight() / 2));
        }
    }

    @Test
    void aTranslucentBackgroundKeepsItsAlpha() throws Exception {
        BarcodeData data = BarcodeData.of("alpha", BarcodeType.QR_CODE,
                java.awt.Color.BLACK, new java.awt.Color(255, 255, 255, 0));
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(DocxBarcodePictures.png(data, 60, 60)));

        assertThat(image.getRGB(0, 0) >>> 24).as("the quiet zone shows what is under it").isZero();
    }

    @Test
    void thePicturesDescriptionIsItsData() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addBarcode(b -> b.qrCode().data("GC-2026-001").size(60, 60)))) {
            var docPr = document.getParagraphs().get(0).getRuns().get(0).getCTR()
                    .getDrawingArray(0).getInlineArray(0).getDocPr();

            assertThat(docPr.getDescr()).isEqualTo("GC-2026-001");
        }
    }

    @Test
    void aTransparentBarcodeStillHoldsItsPlaceAndItsAnchor() throws Exception {
        DocumentColor clear = DocumentColor.of(new java.awt.Color(0, 0, 0, 0));
        try (XWPFDocument document = DocxExports.withLayout(595, 842, 36, page -> page
                .addBarcode(b -> b.qrCode().data("x").size(50, 50).foreground(clear).background(clear)
                        .anchor("hidden")))) {
            XWPFPicture picture = onlyPicture(document);

            assertThat(picture.getCTPicture().getSpPr().getXfrm().getExt().getCy()).isEqualTo(Units.toEMU(50));
            assertThat(document.getParagraphs().get(0).getCTP().getBookmarkStartList())
                    .extracting(CTBookmark::getName)
                    .containsExactly("hidden");
        }
    }

    @Test
    void aLinkOnABarcodeIsReportedAsNotCarried() throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (DocumentSession session = GraphCompose.document().pageSize(595, 842)
                .margin(DocumentInsets.of(36)).create()) {
            session.pageFlow(page -> page
                    .addParagraph(p -> p.text("Target").anchor("target"))
                    .addBarcode(b -> b.qrCode().data("x").size(50, 50).linkTo("target")));
            session.export(new DocxSemanticBackend(report::set));
        }

        assertThat(report.get().bySubject().get("barcode").get(0).detail()).contains("link is not carried");
    }

    private static XWPFPicture onlyPicture(XWPFDocument document) {
        List<XWPFPicture> pictures = document.getParagraphs().stream()
                .flatMap(p -> p.getRuns().stream())
                .flatMap(r -> r.getEmbeddedPictures().stream())
                .toList();
        assertThat(pictures).hasSize(1);
        return pictures.get(0);
    }

    private static String decode(XWPFPictureData data) throws Exception {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(data.getData()));
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        return new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(
                new RGBLuminanceSource(image.getWidth(), image.getHeight(), pixels)))).getText();
    }
}
