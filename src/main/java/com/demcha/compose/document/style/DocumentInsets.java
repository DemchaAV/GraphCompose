package com.demcha.compose.document.style;

/**
 * Public spacing value for margins, padding, and table cell insets.
 *
 * <p>The canonical DSL accepts this type so application code can describe
 * document spacing without importing low-level engine components. Instances are
 * immutable and thread-safe.</p>
 *
 * @param top top inset in points
 * @param right right inset in points
 * @param bottom bottom inset in points
 * @param left left inset in points
 * @author Artem Demchyshyn
 */
public record DocumentInsets(double top, double right, double bottom, double left) {

    /**
     * Creates zero spacing.
     *
     * @return zero insets
     */
    public static DocumentInsets zero() {
        return of(0);
    }

    /**
     * Creates equal spacing on all sides.
     *
     * @param value inset value in points
     * @return uniform insets
     */
    public static DocumentInsets of(double value) {
        return new DocumentInsets(value, value, value, value);
    }

    /**
     * Creates spacing from vertical and horizontal values.
     *
     * @param vertical top and bottom inset in points
     * @param horizontal left and right inset in points
     * @return symmetric insets
     */
    public static DocumentInsets symmetric(double vertical, double horizontal) {
        return new DocumentInsets(vertical, horizontal, vertical, horizontal);
    }

    /**
     * Creates top-only spacing.
     *
     * @param value top inset in points
     * @return top-only insets
     */
    public static DocumentInsets top(double value) {
        return new DocumentInsets(value, 0, 0, 0);
    }

    /**
     * Creates bottom-only spacing.
     *
     * @param value bottom inset in points
     * @return bottom-only insets
     */
    public static DocumentInsets bottom(double value) {
        return new DocumentInsets(0, 0, value, 0);
    }

    /**
     * Calculates the combined left and right spacing.
     *
     * @return horizontal spacing in points
     */
    public double horizontal() {
        return left + right;
    }

    /**
     * Calculates the combined top and bottom spacing.
     *
     * @return vertical spacing in points
     */
    public double vertical() {
        return top + bottom;
    }
}
