package com.demcha.components.content.rectangle;

import com.demcha.components.style.ColorComponent;
import lombok.NonNull;

import java.awt.*;

public record StrokeColor(Color color) {
    public static Color DEFAULT_STROKE_COLOR = ColorComponent.BLACK;

    public StrokeColor(@NonNull ColorComponent color) {
        this(color.color());
    }

    public static StrokeColor defaultColor() {
        return new StrokeColor(DEFAULT_STROKE_COLOR);
    }
}
