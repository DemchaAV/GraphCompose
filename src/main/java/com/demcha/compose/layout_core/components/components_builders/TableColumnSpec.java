package com.demcha.compose.layout_core.components.components_builders;

/**
 * Column sizing specification for {@link TableBuilder}.
 */
public record TableColumnSpec(Type type, Double fixedWidth) {

    public TableColumnSpec {
        if (type == null) {
            throw new IllegalArgumentException("Column type cannot be null.");
        }
        if (type == Type.FIXED) {
            if (fixedWidth == null || fixedWidth <= 0) {
                throw new IllegalArgumentException("Fixed column width must be greater than 0.");
            }
        }
    }

    public static TableColumnSpec auto() {
        return new TableColumnSpec(Type.AUTO, null);
    }

    public static TableColumnSpec fixed(double points) {
        return new TableColumnSpec(Type.FIXED, points);
    }

    public boolean isAuto() {
        return type == Type.AUTO;
    }

    public boolean isFixed() {
        return type == Type.FIXED;
    }

    public double requiredFixedWidth() {
        if (!isFixed()) {
            throw new IllegalStateException("Column is not fixed.");
        }
        return fixedWidth;
    }

    public enum Type {
        AUTO,
        FIXED
    }
}
