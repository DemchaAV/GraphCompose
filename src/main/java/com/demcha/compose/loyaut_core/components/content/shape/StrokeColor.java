package com.demcha.compose.loyaut_core.components.content.shape;

import com.demcha.compose.loyaut_core.components.style.ComponentColor;
import lombok.NonNull;

import java.awt.*;

public record StrokeColor(Color color) {
    public static Color DEFAULT_STROKE_COLOR = ComponentColor.BLACK;

    public StrokeColor(@NonNull ComponentColor color) {
        this(color.color());
    }

    public static StrokeColor defaultColor() {
        return new StrokeColor(DEFAULT_STROKE_COLOR);
    }
}
