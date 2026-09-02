package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.image.DocumentImageData;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.imageio.ImageIO;

/**
 * The photograph the CV fixtures use.
 *
 * <p>It is drawn rather than committed: a real face in the repository would
 * be a licensed asset guarding a layout, and what the gates need is only
 * that the disc has something in it. It is deterministic, so the pixel
 * baselines that include it are stable.</p>
 */
final class CvFixturePortrait {

    private CvFixturePortrait() {
    }

    /**
     * A flat two-tone silhouette.
     *
     * @return the portrait image data
     */
    static DocumentImageData silhouette() {
        BufferedImage image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(222, 226, 231));
            g.fillRect(0, 0, 512, 512);
            g.setColor(new Color(150, 160, 173));
            g.fillOval(171, 102, 169, 169);
            g.fillOval(67, 307, 379, 379);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", png);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to draw the fixture portrait", e);
        }
        return DocumentImageData.fromBytes(png.toByteArray());
    }
}
