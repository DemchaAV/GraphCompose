package com.demcha.compose.document.style;

/**
 * Public stroke value for borders, dividers, and simple shapes.
 *
 * <p>The canonical authoring API uses this value so application code can
 * describe visible outlines without importing internal engine shape components.
 * Instances are immutable and thread-safe.</p>
 *
 * @param color stroke color
 * @param width stroke width in points
 * @author Artem Demchyshyn
 */
public record DocumentStroke(DocumentColor color, double width) {
    public static final double DEFAULT_WIDTH = 1.0;

    /**
     * Creates a normalized stroke value.
     *
     * @param color stroke color
     * @param width stroke width in points
     */
    public DocumentStroke {
        color = color == null ? DocumentColor.BLACK : color;
        if (width < 0 || Double.isNaN(width) || Double.isInfinite(width)) {
            throw new IllegalArgumentException("Stroke width must be finite and non-negative: " + width);
        }
    }

    /**
     * Creates a default-width stroke.
     *
     * @param color stroke color
     * @return stroke value
     */
    public static DocumentStroke of(DocumentColor color) {
        return new DocumentStroke(color, DEFAULT_WIDTH);
    }

    /**
     * Creates a stroke with explicit width.
     *
     * @param color stroke color
     * @param width stroke width in points
     * @return stroke value
     */
    public static DocumentStroke of(DocumentColor color, double width) {
        return new DocumentStroke(color, width);
    }
}
