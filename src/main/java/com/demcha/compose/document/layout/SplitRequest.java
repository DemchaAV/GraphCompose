package com.demcha.compose.document.layout;

/**
 * Split request for a splittable semantic node.
 *
 * @param constraints available layout space for the split
 * @param remainingHeight remaining height on the current page
 * @param pageInnerHeight full inner page height for tail preparation
 * @param context active prepare context used by split logic
 */
public record SplitRequest(
        BoxConstraints constraints,
        double remainingHeight,
        double pageInnerHeight,
        PrepareContext context
) {
    /**
     * Validates split heights for pagination.
     */
    public SplitRequest {
        if (remainingHeight < 0 || Double.isNaN(remainingHeight) || Double.isInfinite(remainingHeight)) {
            throw new IllegalArgumentException("remainingHeight must be finite and non-negative: " + remainingHeight);
        }
        if (pageInnerHeight <= 0 || Double.isNaN(pageInnerHeight) || Double.isInfinite(pageInnerHeight)) {
            throw new IllegalArgumentException("pageInnerHeight must be finite and positive: " + pageInnerHeight);
        }
    }
}


