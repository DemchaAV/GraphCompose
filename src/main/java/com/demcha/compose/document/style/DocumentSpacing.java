package com.demcha.compose.document.style;

/**
 * Public one-dimensional spacing token used by document and template policies.
 *
 * <p>Use this value when a setting represents one distance, such as the gap
 * between modules, rows, wrapped lines, or list items. Use {@link DocumentInsets}
 * when a setting needs four box sides. Instances are immutable and thread-safe.</p>
 *
 * @param value spacing value in points
 * @author Artem Demchyshyn
 */
public record DocumentSpacing(double value) {

    /**
     * Creates a normalized spacing token.
     *
     * @param value spacing value in points
     */
    public DocumentSpacing {
        if (value < 0 || Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Spacing must be finite and non-negative: " + value);
        }
    }

    /**
     * Returns a zero spacing token.
     *
     * @return zero spacing
     */
    public static DocumentSpacing zero() {
        return new DocumentSpacing(0);
    }

    /**
     * Creates a spacing token in points.
     *
     * @param value spacing value in points
     * @return spacing token
     */
    public static DocumentSpacing of(double value) {
        return new DocumentSpacing(value);
    }
}
