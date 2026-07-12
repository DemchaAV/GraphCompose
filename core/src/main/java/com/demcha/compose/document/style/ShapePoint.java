package com.demcha.compose.document.style;

/**
 * A normalized vertex of a {@link ShapeOutline.Polygon}, expressed in the
 * outline's own unit box: {@code x} runs 0 (left) → 1 (right), {@code y} runs
 * 0 (bottom) → 1 (top), following the PDF y-up convention. Points are scaled to
 * the outline's {@code width × height} at render time, so the same normalized
 * polygon renders at any size.
 *
 * @param x normalized horizontal position in {@code [0, 1]}
 * @param y normalized vertical position in {@code [0, 1]} (0 = bottom, 1 = top)
 * @author Artem Demchyshyn
 * @since 1.7.0
 */
public record ShapePoint(double x, double y) {
    /**
     * Validates that both coordinates are finite and within the unit box.
     */
    public ShapePoint {
        requireUnit("x", x);
        requireUnit("y", y);
    }

    private static void requireUnit(String label, double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(label + " must be a finite value within [0, 1]: " + value);
        }
    }
}
