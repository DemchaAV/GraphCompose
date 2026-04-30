package com.demcha.compose.document.style;

/**
 * Public per-corner radius value for rectangle-like canonical document
 * nodes.
 *
 * <p>Each corner radius is in points. The values are render-only — they
 * do not change layout measurement, padding, margins, or layout
 * snapshots. The render layer additionally clamps every corner radius
 * to half of the smaller box side at draw time so a too-large radius
 * never overshoots the box geometry.</p>
 *
 * <p>v1.5 generalised the original single-radius value to four
 * independent corner radii. The single-argument constructor and the
 * single-argument {@link #of(double)} factory remain so every caller
 * that wrote {@code DocumentCornerRadius.of(8)} or
 * {@code new DocumentCornerRadius(8)} continues to compile and behaves
 * identically (all four corners get the same radius). New callers that
 * need asymmetry use the four-arg form or one of the convenience
 * helpers below.</p>
 *
 * @param topLeft top-left radius in points
 * @param topRight top-right radius in points
 * @param bottomRight bottom-right radius in points
 * @param bottomLeft bottom-left radius in points
 *
 * @author Artem Demchyshyn
 */
public record DocumentCornerRadius(double topLeft, double topRight, double bottomRight, double bottomLeft) {
    /**
     * Radius value representing square corners on every side.
     */
    public static final DocumentCornerRadius ZERO = new DocumentCornerRadius(0.0, 0.0, 0.0, 0.0);

    /**
     * Validates every corner radius — finite and non-negative.
     */
    public DocumentCornerRadius {
        requireNonNegativeFinite("topLeft", topLeft);
        requireNonNegativeFinite("topRight", topRight);
        requireNonNegativeFinite("bottomRight", bottomRight);
        requireNonNegativeFinite("bottomLeft", bottomLeft);
    }

    /**
     * Backward-compatible single-argument constructor — applies the same
     * radius to all four corners.
     *
     * @param radius radius in points (non-negative)
     */
    public DocumentCornerRadius(double radius) {
        this(radius, radius, radius, radius);
    }

    /**
     * Creates a uniform corner radius applied to all four corners.
     *
     * @param radius radius in points
     * @return uniform corner radius
     */
    public static DocumentCornerRadius of(double radius) {
        return new DocumentCornerRadius(radius);
    }

    /**
     * Creates a corner radius with explicit values per corner. Order
     * matches CSS-style top-left, top-right, bottom-right, bottom-left.
     *
     * @param topLeft top-left radius
     * @param topRight top-right radius
     * @param bottomRight bottom-right radius
     * @param bottomLeft bottom-left radius
     * @return per-corner radius
     */
    public static DocumentCornerRadius of(double topLeft, double topRight, double bottomRight, double bottomLeft) {
        return new DocumentCornerRadius(topLeft, topRight, bottomRight, bottomLeft);
    }

    /**
     * Convenience: round only the right side (top-right + bottom-right);
     * leave left corners square. Useful for cards that sit flush against
     * a left accent strip or page edge.
     *
     * @param radius radius applied to both right corners in points
     * @return right-side rounded corner radius
     */
    public static DocumentCornerRadius right(double radius) {
        return new DocumentCornerRadius(0.0, radius, radius, 0.0);
    }

    /**
     * Convenience: round only the left side (top-left + bottom-left);
     * leave right corners square.
     *
     * @param radius radius applied to both left corners in points
     * @return left-side rounded corner radius
     */
    public static DocumentCornerRadius left(double radius) {
        return new DocumentCornerRadius(radius, 0.0, 0.0, radius);
    }

    /**
     * Convenience: round only the top corners (top-left + top-right);
     * leave bottom corners square. Useful for tab-style or stacked
     * cards whose bottom edge butts against another panel.
     *
     * @param radius radius applied to both top corners in points
     * @return top-side rounded corner radius
     */
    public static DocumentCornerRadius top(double radius) {
        return new DocumentCornerRadius(radius, radius, 0.0, 0.0);
    }

    /**
     * Convenience: round only the bottom corners (bottom-left +
     * bottom-right); leave top corners square.
     *
     * @param radius radius applied to both bottom corners in points
     * @return bottom-side rounded corner radius
     */
    public static DocumentCornerRadius bottom(double radius) {
        return new DocumentCornerRadius(0.0, 0.0, radius, radius);
    }

    /**
     * Backward-compatible accessor for the previous single-radius API.
     * Returns the {@code topLeft} value, which equals every other
     * corner when constructed via the single-radius factory. Callers
     * that need asymmetric corners should switch to the
     * {@link #topLeft()}, {@link #topRight()}, {@link #bottomRight()},
     * and {@link #bottomLeft()} accessors.
     *
     * @return the top-left radius in points
     */
    public double radius() {
        return topLeft;
    }

    /**
     * @return whether every corner radius equals zero
     */
    public boolean isZero() {
        return topLeft == 0.0 && topRight == 0.0 && bottomRight == 0.0 && bottomLeft == 0.0;
    }

    /**
     * @return whether every corner has the same radius value
     */
    public boolean isUniform() {
        return topLeft == topRight && topLeft == bottomRight && topLeft == bottomLeft;
    }

    private static void requireNonNegativeFinite(String label, double value) {
        if (value < 0 || Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(label + " corner radius must be finite and non-negative: " + value);
        }
    }
}
