package com.demcha.compose.v2;

/**
 * Available layout space for one node measurement/layout step.
 */
public record BoxConstraints(double availableWidth, double availableHeight) {
    public BoxConstraints {
        if (availableWidth < 0 || Double.isNaN(availableWidth) || Double.isInfinite(availableWidth)) {
            throw new IllegalArgumentException("availableWidth must be finite and non-negative: " + availableWidth);
        }
        if (availableHeight < 0 || Double.isNaN(availableHeight) || Double.isInfinite(availableHeight)) {
            throw new IllegalArgumentException("availableHeight must be finite and non-negative: " + availableHeight);
        }
    }
}
