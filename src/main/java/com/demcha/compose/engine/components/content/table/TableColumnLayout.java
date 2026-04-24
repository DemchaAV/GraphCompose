package com.demcha.compose.engine.components.content.table;

/**
 * Internal table column sizing model used by the V2 layout engine.
 *
 * <p>Canonical authoring code should use document-level table facade types and
 * let the layout pipeline adapt them to this runtime model.</p>
 *
 * @author Artem Demchyshyn
 */
public record TableColumnLayout(Type type, Double fixedWidth) {

    public TableColumnLayout {
        if (type == null) {
            throw new IllegalArgumentException("Column type cannot be null.");
        }
        if (type == Type.FIXED && (fixedWidth == null || fixedWidth <= 0)) {
            throw new IllegalArgumentException("Fixed column width must be greater than 0.");
        }
    }

    /**
     * Creates an auto-sized column.
     *
     * @return auto column layout
     */
    public static TableColumnLayout auto() {
        return new TableColumnLayout(Type.AUTO, null);
    }

    /**
     * Creates a fixed-width column.
     *
     * @param points width in document points
     * @return fixed column layout
     */
    public static TableColumnLayout fixed(double points) {
        return new TableColumnLayout(Type.FIXED, points);
    }

    /**
     * Returns whether this column uses natural/auto width negotiation.
     *
     * @return true for auto columns
     */
    public boolean isAuto() {
        return type == Type.AUTO;
    }

    /**
     * Returns whether this column has an explicit fixed width.
     *
     * @return true for fixed columns
     */
    public boolean isFixed() {
        return type == Type.FIXED;
    }

    /**
     * Returns the fixed width or fails if the column is not fixed.
     *
     * @return fixed width in points
     */
    public double requiredFixedWidth() {
        if (!isFixed()) {
            throw new IllegalStateException("Column is not fixed.");
        }
        return fixedWidth;
    }

    /**
     * Column sizing mode.
     */
    public enum Type {
        AUTO,
        FIXED
    }
}
