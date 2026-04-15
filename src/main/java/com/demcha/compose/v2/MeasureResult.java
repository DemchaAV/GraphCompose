package com.demcha.compose.v2;

/**
 * Measured node box size excluding margin.
 */
public record MeasureResult(double width, double height) {
    public MeasureResult {
        if (width < 0 || Double.isNaN(width) || Double.isInfinite(width)) {
            throw new IllegalArgumentException("width must be finite and non-negative: " + width);
        }
        if (height < 0 || Double.isNaN(height) || Double.isInfinite(height)) {
            throw new IllegalArgumentException("height must be finite and non-negative: " + height);
        }
    }
}
