package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.api.Internal;
import com.demcha.compose.engine.components.content.barcode.BarcodeData;
import com.demcha.compose.engine.components.content.barcode.BarcodeType;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.Writer;
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

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/**
 * Encodes a barcode into the ZXing bit matrix the renderer draws.
 *
 * <p>The matrix is requested at twice the fragment box, and at least 200 cells on
 * each axis, and is then stretched over the whole box. Inside it ZXing scales the
 * symbol by a whole number of cells and centres it, so the symbol is not always
 * flush with the box edges; PDF417 returns a matrix of its own size instead. When a
 * box side is under 100 points the 200-cell floor gives that axis a different scale,
 * so cells need not be square in the box. The drawing maps whatever matrix comes
 * back onto the box, which keeps each format's placement exactly as ZXing lays it
 * out.</p>
 *
 * <p>Shared by the PDF and PPTX barcode handlers so both draw the same matrix.</p>
 */
@Internal
public final class BarcodeMatrices {

    private static final int MIN_MATRIX_SIZE = 200;

    private BarcodeMatrices() {
    }

    /**
     * Encodes {@code data} for a fragment box of the given size.
     *
     * @param data      barcode content, symbology and quiet-zone margin
     * @param boxWidth  fragment box width in points
     * @param boxHeight fragment box height in points
     * @return the encoded matrix; a set bit is a dark cell
     * @throws IOException if ZXing cannot encode the content in that symbology
     */
    public static BitMatrix encode(BarcodeData data, int boxWidth, int boxHeight) throws IOException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        if (data.getMargin() >= 0) {
            hints.put(EncodeHintType.MARGIN, data.getMargin());
        }
        try {
            return writer(data.getType()).encode(
                    data.getContent(),
                    format(data.getType()),
                    Math.max(boxWidth * 2, MIN_MATRIX_SIZE),
                    Math.max(boxHeight * 2, MIN_MATRIX_SIZE),
                    hints);
        } catch (WriterException ex) {
            throw new IOException("Failed to generate barcode for type " + data.getType(), ex);
        }
    }

    private static Writer writer(BarcodeType type) {
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

    private static BarcodeFormat format(BarcodeType type) {
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
