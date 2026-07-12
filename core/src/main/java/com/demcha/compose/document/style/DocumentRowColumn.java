package com.demcha.compose.document.style;

/**
 * Per-child column width for a {@code row(...)} — a fixed point width, an
 * automatic (content-sized) width, or a weight that shares the leftover space.
 * The row peer of {@link com.demcha.compose.document.table.DocumentTableColumn};
 * resolution order is fixed → auto → weight-remainder.
 *
 * <p>An {@link Type#AUTO auto} column takes its content's natural width <em>up to
 * the row width</em> — content still wraps if it cannot fit, it does not overflow.
 * Fixed and auto columns are left-packed: if they do not fill the row, the
 * trailing space is left empty unless a {@link #weight(double) weight} column is
 * present to absorb the remainder. A weight-only column list resolves identically
 * to plain {@code weights(...)}, which remains the sugar for the common even /
 * weighted split. Instances are immutable and thread-safe.</p>
 *
 * @param type  the sizing strategy
 * @param value points for {@code FIXED}, the weight for {@code WEIGHT}, or
 *              {@code null} for {@code AUTO}
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
public record DocumentRowColumn(Type type, Double value) {

    /** How a row column is sized. */
    public enum Type {
        /** Width is the column's content natural width, capped at the row width. */
        AUTO,
        /** Width is a fixed point value. */
        FIXED,
        /** Width is a share of the space left after the fixed and auto columns. */
        WEIGHT
    }

    /**
     * Validates the type/value pairing.
     */
    public DocumentRowColumn {
        if (type == null) {
            throw new IllegalArgumentException("Column type cannot be null.");
        }
        if (type == Type.FIXED && !isPositiveFinite(value)) {
            throw new IllegalArgumentException("Fixed column width must be a positive finite number, got: " + value);
        }
        if (type == Type.WEIGHT && !isPositiveFinite(value)) {
            throw new IllegalArgumentException("Column weight must be a positive finite number, got: " + value);
        }
    }

    /**
     * A column sized to a fixed point width.
     *
     * @param points width in points (must be positive)
     * @return a fixed column
     */
    public static DocumentRowColumn fixed(double points) {
        return new DocumentRowColumn(Type.FIXED, points);
    }

    /**
     * A column sized to its content's natural width (capped at the row width).
     *
     * @return an auto column
     */
    public static DocumentRowColumn auto() {
        return new DocumentRowColumn(Type.AUTO, null);
    }

    /**
     * A column that takes a share of the space left after the fixed and auto
     * columns, proportional to {@code weight}.
     *
     * @param weight relative share (must be positive)
     * @return a weight column
     */
    public static DocumentRowColumn weight(double weight) {
        return new DocumentRowColumn(Type.WEIGHT, weight);
    }

    private static boolean isPositiveFinite(Double value) {
        return value != null && value > 0.0 && !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
