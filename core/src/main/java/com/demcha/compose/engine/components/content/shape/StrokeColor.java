package com.demcha.compose.engine.components.content.shape;

import com.demcha.compose.engine.components.style.ComponentColor;

import java.awt.*;

public record StrokeColor(Color color) {
    public static Color DEFAULT_STROKE_COLOR = ComponentColor.BLACK;

    public StrokeColor(ComponentColor color) {
        this(color.color());
    }

    public static StrokeColor defaultColor() {
        return new StrokeColor(DEFAULT_STROKE_COLOR);
    }
}
