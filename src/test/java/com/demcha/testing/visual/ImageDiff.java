package com.demcha.testing.visual;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Pixel-by-pixel image comparison utility for visual regression tests.
 *
 * <p>The diff treats each pixel as an RGB triple and reports two numbers:</p>
 * <ul>
 *   <li>{@code maxChannelDelta} — the largest single-channel difference observed
 *       across all pixels. Values are 0..255 in the same scale as the source
 *       channels.</li>
 *   <li>{@code mismatchedPixelCount} — the number of pixels with at least one
 *       channel difference greater than the configured per-pixel tolerance.</li>
 * </ul>
 *
 * <p>The result also keeps an optional difference image where mismatched
 * pixels are coloured red and matching pixels stay greyscale, so test failures
 * can write the diff to disk for inspection.</p>
 *
 * @author Artem Demchyshyn
 */
public final class ImageDiff {

    private ImageDiff() {
    }

    /**
     * Compares two images pixel-by-pixel.
     *
     * @param expected baseline image
     * @param actual rendered image
     * @param perPixelTolerance maximum allowed per-channel delta (0..255) before a pixel is flagged
     * @return diff result with mismatch counts and optional difference image
     */
    public static Result compare(BufferedImage expected, BufferedImage actual, int perPixelTolerance) {
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(actual, "actual");
        if (perPixelTolerance < 0 || perPixelTolerance > 255) {
            throw new IllegalArgumentException("perPixelTolerance must be 0..255, got " + perPixelTolerance);
        }

        int width = expected.getWidth();
        int height = expected.getHeight();
        if (actual.getWidth() != width || actual.getHeight() != height) {
            return new Result(true,
                    Long.MAX_VALUE,
                    255,
                    expected.getWidth() + "x" + expected.getHeight()
                            + " vs " + actual.getWidth() + "x" + actual.getHeight(),
                    null);
        }

        BufferedImage diffImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        long mismatched = 0;
        int maxDelta = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int expectedRgb = expected.getRGB(x, y);
                int actualRgb = actual.getRGB(x, y);
                int dr = Math.abs(red(expectedRgb) - red(actualRgb));
                int dg = Math.abs(green(expectedRgb) - green(actualRgb));
                int db = Math.abs(blue(expectedRgb) - blue(actualRgb));
                int delta = Math.max(dr, Math.max(dg, db));
                if (delta > maxDelta) {
                    maxDelta = delta;
                }
                if (delta > perPixelTolerance) {
                    mismatched++;
                    diffImage.setRGB(x, y, Color.RED.getRGB());
                } else {
                    int gray = (red(expectedRgb) + green(expectedRgb) + blue(expectedRgb)) / 3;
                    diffImage.setRGB(x, y, new Color(gray, gray, gray).getRGB());
                }
            }
        }

        long totalPixels = (long) width * height;
        return new Result(mismatched > 0, mismatched, maxDelta,
                "size=" + width + "x" + height + " mismatched=" + mismatched + "/" + totalPixels
                        + " maxDelta=" + maxDelta,
                diffImage);
    }

    private static int red(int rgb) {
        return (rgb >> 16) & 0xFF;
    }

    private static int green(int rgb) {
        return (rgb >> 8) & 0xFF;
    }

    private static int blue(int rgb) {
        return rgb & 0xFF;
    }

    /**
     * Diff result.
     *
     * @param differs whether at least one pixel differs above the tolerance
     * @param mismatchedPixelCount number of pixels above the tolerance
     * @param maxChannelDelta largest single-channel delta observed
     * @param summary human-readable summary line for failure messages
     * @param diffImage optional visualisation; {@code null} when sizes differed
     */
    public record Result(
            boolean differs,
            long mismatchedPixelCount,
            int maxChannelDelta,
            String summary,
            BufferedImage diffImage
    ) {
        /**
         * Returns whether the diff is below the supplied pixel-count budget.
         *
         * @param maxMismatchedPixels maximum allowed mismatched pixel count
         * @return {@code true} when the diff is within budget
         */
        public boolean withinBudget(long maxMismatchedPixels) {
            return mismatchedPixelCount <= maxMismatchedPixels;
        }
    }
}
