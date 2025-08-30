package com.demcha.components.content.shape;

import com.demcha.components.core.Component;

import java.awt.*;

public record Stroke(StrokeColor strokeColor, double width) implements Component {
    public static final double DEFAULT_WIDTH = 2.0;

    public Stroke(Color color) {
        this(color, DEFAULT_WIDTH);
    }

    public Stroke(Color strokeColor, double width) {
        this(new StrokeColor(strokeColor), width);
    }

    public Stroke(double width) {
        this(StrokeColor.DEFAULT_STROKE_COLOR, width);
    }

    public Stroke() {
        this(StrokeColor.DEFAULT_STROKE_COLOR, DEFAULT_WIDTH);
    }
}
