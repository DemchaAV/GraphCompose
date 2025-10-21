package com.demcha.core;

import com.demcha.components.style.Margin;

public record CanvasSize(double width, double height, float x, float y, Margin margin) {
    public CanvasSize(double width, double height, float x, float y) {
        this(width, height, x, y, null);
    }
}
