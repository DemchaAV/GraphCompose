package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.backend.fixed.pptx.PptxCoordinates;
import com.demcha.compose.document.backend.fixed.pptx.PptxFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.PptxRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.BarcodeFragmentPayload;
import com.demcha.compose.engine.components.content.barcode.BarcodeData;
import com.demcha.compose.engine.components.content.barcode.BarcodeType;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.datamatrix.DataMatrixWriter;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.oned.Code39Writer;
import com.google.zxing.oned.EAN13Writer;
import com.google.zxing.oned.EAN8Writer;
import com.google.zxing.oned.UPCAWriter;
import com.google.zxing.pdf417.PDF417Writer;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;

import javax.imageio.ImageIO;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/**
 * Renders semantic barcode fragments with the same ZXing raster the PDF
 * handler produces — identical writer, hints, oversampled render size, and
 * pixel colors — placed as a picture on the fragment box.
 *
 * @since 2.1.0
 */
public final class PptxBarcodeFragmentRenderHandler
        implements PptxFragmentRenderHandler<BarcodeFragmentPayload> {

    /**
     * Creates the barcode fragment renderer.
     */
    public PptxBarcodeFragmentRenderHandler() {
    }

    @Override
    public Class<BarcodeFragmentPayload> payloadType() {
        return BarcodeFragmentPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       BarcodeFragmentPayload payload,
                       PptxRenderEnvironment environment) throws IOException {
        if (fragment.width() <= 0 || fragment.height() <= 0) {
            return;
        }
        BufferedImage barcode = generateBarcodeImage(
                payload.barcodeData(), (int) fragment.width(), (int) fragment.height());
        byte[] png;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(barcode, "PNG", output);
            png = output.toByteArray();
        }
        XSLFPictureShape picture = environment.surface(fragment.pageIndex())
                .createPicture(environment.slideShow().addPicture(png, PictureData.PictureType.PNG));
        picture.setAnchor(new Rectangle2D.Double(
                fragment.x(),
                PptxCoordinates.topY(environment.canvasHeight(), fragment.y(), fragment.height()),
                fragment.width(),
                fragment.height()));
    }

    /**
     * Mirrors the PDF handler's raster generation exactly: same oversampling,
     * hints, and per-pixel foreground/background mapping, so both formats
     * carry the same barcode bitmap.
     */
    private static BufferedImage generateBarcodeImage(BarcodeData data,
                                                      int width,
                                                      int height) throws IOException {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            if (data.getMargin() >= 0) {
                hints.put(EncodeHintType.MARGIN, data.getMargin());
            }
            int renderWidth = Math.max(width * 2, 200);
            int renderHeight = Math.max(height * 2, 200);
            BitMatrix matrix = createWriter(data.getType()).encode(
                    data.getContent(), mapFormat(data.getType()), renderWidth, renderHeight, hints);
            BufferedImage image = new BufferedImage(
                    matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_ARGB);
            int foreground = data.getForeground().getRGB();
            int background = data.getBackground().getRGB();
            for (int py = 0; py < matrix.getHeight(); py++) {
                for (int px = 0; px < matrix.getWidth(); px++) {
                    image.setRGB(px, py, matrix.get(px, py) ? foreground : background);
                }
            }
            return image;
        } catch (WriterException ex) {
            throw new IOException("Failed to generate barcode for type " + data.getType(), ex);
        }
    }

    private static com.google.zxing.Writer createWriter(BarcodeType type) {
        return switch (type) {
            case QR_CODE -> new QRCodeWriter();
            case CODE_128 -> new Code128Writer();
            case CODE_39 -> new Code39Writer();
            case EAN_13 -> new EAN13Writer();
            case EAN_8 -> new EAN8Writer();
            case UPC_A -> new UPCAWriter();
            case PDF_417 -> new PDF417Writer();
            case DATA_MATRIX -> new DataMatrixWriter();
        };
    }

    private static BarcodeFormat mapFormat(BarcodeType type) {
        return switch (type) {
            case QR_CODE -> BarcodeFormat.QR_CODE;
            case CODE_128 -> BarcodeFormat.CODE_128;
            case CODE_39 -> BarcodeFormat.CODE_39;
            case EAN_13 -> BarcodeFormat.EAN_13;
            case EAN_8 -> BarcodeFormat.EAN_8;
            case UPC_A -> BarcodeFormat.UPC_A;
            case PDF_417 -> BarcodeFormat.PDF_417;
            case DATA_MATRIX -> BarcodeFormat.DATA_MATRIX;
        };
    }
}
