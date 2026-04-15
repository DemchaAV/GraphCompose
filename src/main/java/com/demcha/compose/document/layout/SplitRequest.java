package com.demcha.compose.document.layout;

/**
 * Split request for a splittable semantic node.
 */
public record SplitRequest(
        BoxConstraints constraints,
        double remainingHeight,
        double pageInnerHeight,
        PrepareContext context
) {
    public SplitRequest {
        if (remainingHeight < 0 || Double.isNaN(remainingHeight) || Double.isInfinite(remainingHeight)) {
            throw new IllegalArgumentException("remainingHeight must be finite and non-negative: " + remainingHeight);
        }
        if (pageInnerHeight <= 0 || Double.isNaN(pageInnerHeight) || Double.isInfinite(pageInnerHeight)) {
            throw new IllegalArgumentException("pageInnerHeight must be finite and positive: " + pageInnerHeight);
        }
    }
}


