package com.demcha.compose.document.backend.semantic.docx;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * A picture this export draws, encoded as a PNG in memory.
 *
 * <p>{@code ImageIO.write} to a stream caches through a temporary file by default; a writer
 * on a {@link MemoryCacheImageOutputStream} writes the same bytes without touching the
 * disk.</p>
 */
final class DocxPng {

    private DocxPng() {
    }

    /**
     * Encodes an image as a PNG.
     *
     * @param image the image
     * @return the PNG bytes
     * @throws IOException when the image cannot be encoded
     */
    static byte[] encode(BufferedImage image) throws IOException {
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
