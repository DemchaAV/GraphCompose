package com.demcha.compose.engine.components.content.shape;

import com.demcha.compose.engine.components.core.Component;

public record LinePath(double startX, double startY, double endX, double endY) implements Component {

    public LinePath {
        validateFinite("startX", startX);
        validateFinite("startY", startY);
        validateFinite("endX", endX);
        validateFinite("endY", endY);
    }

    public static LinePath horizontal() {
        return new LinePath(0.0, 0.5, 1.0, 0.5);
    }

    public static LinePath vertical() {
        return new LinePath(0.5, 0.0, 0.5, 1.0);
    }

    public static LinePath diagonalAscending() {
        return new LinePath(0.0, 0.0, 1.0, 1.0);
    }

    public static LinePath diagonalDescending() {
        return new LinePath(0.0, 1.0, 1.0, 0.0);
    }

    private static void validateFinite(String name, double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(name + " must be finite: " + value);
        }
    }
}
