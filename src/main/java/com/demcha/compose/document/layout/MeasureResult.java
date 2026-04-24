package com.demcha.compose.document.layout;

/**
 * Measured node box size excluding margin.
 *
 * @param width measured content width
 * @param height measured content height
 */
public record MeasureResult(double width, double height) {
    /**
     * Validates measured width and height.
     */
    public MeasureResult {
        if (width < 0 || Double.isNaN(width) || Double.isInfinite(width)) {
            throw new IllegalArgumentException("width must be finite and non-negative: " + width);
        }
        if (height < 0 || Double.isNaN(height) || Double.isInfinite(height)) {
            throw new IllegalArgumentException("height must be finite and non-negative: " + height);
        }
    }
}


