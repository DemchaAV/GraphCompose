package com.demcha.compose.document.style;

/**
 * Public corner-radius value for rectangle-like canonical document nodes.
 *
 * <p>The radius is a render-only style in points. It does not change layout
 * measurement, padding, margins, or layout snapshots.</p>
 *
 * @param radius radius in points
 * @author Artem Demchyshyn
 */
public record DocumentCornerRadius(double radius) {
    /**
     * Radius value representing square corners.
     */
    public static final DocumentCornerRadius ZERO = new DocumentCornerRadius(0.0);

    /**
     * Creates a validated corner radius.
     */
    public DocumentCornerRadius {
        if (radius < 0 || Double.isNaN(radius) || Double.isInfinite(radius)) {
            throw new IllegalArgumentException("Corner radius must be finite and non-negative: " + radius);
        }
    }

    /**
     * Creates a corner-radius value in points.
     *
     * @param radius radius in points
     * @return corner-radius value
     */
    public static DocumentCornerRadius of(double radius) {
        return new DocumentCornerRadius(radius);
    }
}
