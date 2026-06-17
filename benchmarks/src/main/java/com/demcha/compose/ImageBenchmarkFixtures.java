package com.demcha.compose;

import com.demcha.compose.document.image.DocumentImageData;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Deterministic synthetic raster fixtures for the image embed/scale benches and
 * the {@code PdfImageCache} reuse gate.
 *
 * <p>The images are generated in code (a fixed gradient placeholder, a few KB
 * each) so the suite needs no committed binary asset and the bytes — hence the
 * cache fingerprint — are stable. {@link #demoImage()} returns the same logical
 * image every call; {@link #distinctImage(int)} returns visually distinct images
 * with distinct fingerprints, to exercise the distinct-embed path.</p>
 *
 * @author Artem Demchyshyn
 */
public final class ImageBenchmarkFixtures {

    /** Native pixel size of every generated fixture. */
    public static final int NATIVE_WIDTH_PX = 360;
    /** Native pixel height of every generated fixture. */
    public static final int NATIVE_HEIGHT_PX = 200;

    /**
     * Draw size (points) that keeps the original-embed path: at 144 DPI this is
     * a {@code 360x200 px} target, i.e. &gt; 50% of native, so {@code PdfImageCache}
     * does not build a downscaled variant and the embed count stays at one.
     */
    public static final double DRAW_WIDTH_PT = 180.0;
    /** Companion draw height (points) for {@link #DRAW_WIDTH_PT}. */
    public static final double DRAW_HEIGHT_PT = 100.0;

    private ImageBenchmarkFixtures() {
    }

    /**
     * One fixed gradient placeholder. Returns equal bytes every call, so all
     * placements share a fingerprint and the cache treats them as one image.
     *
     * @return the shared demo image descriptor
     */
    public static DocumentImageData demoImage() {
        return DocumentImageData.fromBytes(pngBytes(0));
    }

    /**
     * The {@code index}-th of a family of visually distinct images, each with a
     * distinct fingerprint so the cache embeds each one separately.
     *
     * @param index variant index (any non-negative int)
     * @return a distinct image descriptor
     */
    public static DocumentImageData distinctImage(int index) {
        return DocumentImageData.fromBytes(pngBytes(index + 1));
    }

    private static byte[] pngBytes(int seed) {
        BufferedImage image = new BufferedImage(NATIVE_WIDTH_PX, NATIVE_HEIGHT_PX, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            int r = 20 + (seed * 23) % 200;
            int b = 95 + (seed * 17) % 150;
            g.setPaint(new GradientPaint(0, 0, new Color(r, 45, 80),
                    NATIVE_WIDTH_PX, NATIVE_HEIGHT_PX, new Color(20, 80, b)));
            g.fillRect(0, 0, NATIVE_WIDTH_PX, NATIVE_HEIGHT_PX);
            g.setPaint(new Color(196, 153, 76));
            g.setStroke(new BasicStroke(6f));
            g.drawLine(0, 170, NATIVE_WIDTH_PX, 110 - (seed % 40));
            // A seed-positioned 1px marker guarantees byte-distinct content per
            // seed — the modular gradient/line colours above can repeat at large
            // seeds, but a unique x keeps distinctImage(i) fingerprints distinct
            // for i in [0, NATIVE_WIDTH_PX - 1].
            if (seed < NATIVE_WIDTH_PX) {
                g.setPaint(new Color(0, 0, 0));
                g.fillRect(seed, 0, 1, 6);
            }
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", png);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to encode synthetic benchmark image", e);
        }
        return png.toByteArray();
    }
}
