package com.demcha.compose.document.backend.semantic.docx;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Raises a drawn picture by the transparent space it carries below what it shows.
 *
 * <p>Word moves an inline picture by {@code w:position}; LibreOffice ignores that on a picture
 * and stands it on the baseline — measured, one written at 0, −2, −10 and +10pt stood in one
 * place. A separator dot the page centres on its line sits two points above the baseline, so
 * in LibreOffice it came out two points low. A picture this export drew itself can carry that
 * rise as empty rows under the drawing: it then stands on the baseline in both editors with
 * what it shows where the page puts it, and needs no {@code w:position}. A picture the page
 * lowers below the baseline cannot be helped this way and keeps {@code w:position}.</p>
 */
final class DocxPictureLift {

    private DocxPictureLift() {
    }

    /**
     * A picture with the rise it carries.
     *
     * @param png    the picture, its empty rows added
     * @param height its height in points, the rows included
     * @param lift   the rise it carries, in points — the whole pixels nearest the one asked for
     */
    record Lifted(byte[] png, double height, double lift) {
    }

    /**
     * Adds transparent rows under a picture.
     *
     * @param png    the picture
     * @param height its height in points
     * @param rise   how far above the baseline its bottom should stand, in points
     * @return the picture carrying the rise, or the picture as it was when the rise is less
     *         than a pixel
     */
    static Lifted lift(byte[] png, double height, double rise) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
            if (image == null || !(height > 0) || !(rise > 0)) {
                return new Lifted(png, height, 0);
            }
            double pixelsPerPoint = image.getHeight() / height;
            int rows = (int) Math.round(rise * pixelsPerPoint);
            if (rows <= 0) {
                return new Lifted(png, height, 0);
            }
            BufferedImage lifted = new BufferedImage(image.getWidth(), image.getHeight() + rows,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = lifted.createGraphics();
            try {
                // Copied, not blended: the pixels arrive exactly as the raster drew them.
                graphics.setComposite(AlphaComposite.Src);
                graphics.drawImage(image, 0, 0, null);
            } finally {
                graphics.dispose();
            }
            double lift = rows / pixelsPerPoint;
            return new Lifted(DocxPng.encode(lifted), height + lift, lift);
        } catch (IOException failure) {
            throw new IllegalStateException("could not raise an inline picture", failure);
        }
    }
}
