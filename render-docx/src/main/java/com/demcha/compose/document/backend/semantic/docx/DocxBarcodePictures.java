package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.backend.fixed.pdf.handlers.BarcodeMatrices;
import com.demcha.compose.engine.components.content.barcode.BarcodeData;
import com.google.zxing.common.BitMatrix;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * A barcode as the picture Word can hold.
 *
 * <p>Word has no barcode, and drawing the symbol from shapes would make every bar a separate
 * object a reader can knock out of place. So the symbol is a picture: the same ZXing matrix
 * the PDF and PPTX backends draw ({@link BarcodeMatrices}), one pixel per cell — the matrix is
 * already at least two cells per point and two hundred a side — in two colours, the foreground
 * for a dark cell and the background for the rest, each with its own alpha. The data is not
 * editable in Word; the picture scans as the page's does.</p>
 *
 * <p>The PNG is encoded in memory. {@code ImageIO.write} to a stream caches through a temporary
 * file by default, which is the per-barcode cost the PDF backend's barcodes were measured
 * paying and stopped paying when they became vectors.</p>
 */
final class DocxBarcodePictures {

    private DocxBarcodePictures() {
    }

    /**
     * Encodes a barcode as a PNG sized for a box of the given size.
     *
     * @param data   the symbol, its colours and its quiet zone
     * @param width  the box width in points
     * @param height the box height in points
     * @return the PNG bytes, one pixel per matrix cell
     * @throws IOException when ZXing cannot encode the content in that symbology
     */
    static byte[] png(BarcodeData data, double width, double height) throws IOException {
        BitMatrix matrix = BarcodeMatrices.encode(data, (int) width, (int) height);
        BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(),
                BufferedImage.TYPE_BYTE_BINARY, twoColours(data.getBackground(), data.getForeground()));
        WritableRaster raster = image.getRaster();
        int[] row = new int[matrix.getWidth()];
        for (int y = 0; y < matrix.getHeight(); y++) {
            for (int x = 0; x < row.length; x++) {
                row[x] = matrix.get(x, y) ? 1 : 0;
            }
            raster.setPixels(0, y, row.length, 1, row);
        }
        return encode(image);
    }

    /** Index 0 is the background, index 1 the foreground, alpha included. */
    private static IndexColorModel twoColours(Color background, Color foreground) {
        return new IndexColorModel(1, 2,
                new byte[]{(byte) background.getRed(), (byte) foreground.getRed()},
                new byte[]{(byte) background.getGreen(), (byte) foreground.getGreen()},
                new byte[]{(byte) background.getBlue(), (byte) foreground.getBlue()},
                new byte[]{(byte) background.getAlpha(), (byte) foreground.getAlpha()});
    }

    private static byte[] encode(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             MemoryCacheImageOutputStream out = new MemoryCacheImageOutputStream(bytes)) {
            writer.setOutput(out);
            writer.write(image);
            out.flush();
            return bytes.toByteArray();
        } finally {
            writer.dispose();
        }
    }
}
