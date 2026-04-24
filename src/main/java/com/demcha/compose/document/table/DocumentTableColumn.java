package com.demcha.compose.document.table;

/**
 * Public table column sizing value for the canonical DSL.
 *
 * <p>Use {@link #auto()} for negotiated columns and {@link #fixed(double)} when
 * a column must reserve a known width. Instances are immutable and thread-safe.</p>
 *
 * @param type sizing mode
 * @param fixedWidth fixed width in points when {@code type} is {@link Type#FIXED}
 * @author Artem Demchyshyn
 */
public record DocumentTableColumn(Type type, Double fixedWidth) {

    /**
     * Creates a validated table column specification.
     */
    public DocumentTableColumn {
        if (type == null) {
            throw new IllegalArgumentException("Column type cannot be null.");
        }
        if (type == Type.FIXED && (fixedWidth == null || fixedWidth <= 0)) {
            throw new IllegalArgumentException("Fixed column width must be greater than 0.");
        }
    }

    /**
     * Creates an auto-width column.
     *
     * @return auto column specification
     */
    public static DocumentTableColumn auto() {
        return new DocumentTableColumn(Type.AUTO, null);
    }

    /**
     * Creates a fixed-width column.
     *
     * @param points width in points
     * @return fixed column specification
     */
    public static DocumentTableColumn fixed(double points) {
        return new DocumentTableColumn(Type.FIXED, points);
    }

    /**
     * Table column sizing modes.
     */
    public enum Type {
        /**
         * Column width is negotiated from content and available table width.
         */
        AUTO,
        /**
         * Column width is fixed to an explicit point value.
         */
        FIXED
    }
}
