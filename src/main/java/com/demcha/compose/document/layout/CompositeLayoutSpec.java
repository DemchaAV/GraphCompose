package com.demcha.compose.document.layout;

/**
 * Generic child-layout contract for composite semantic nodes.
 */
public record CompositeLayoutSpec(double spacing) {
    public CompositeLayoutSpec {
        if (spacing < 0 || Double.isNaN(spacing) || Double.isInfinite(spacing)) {
            throw new IllegalArgumentException("spacing must be finite and non-negative: " + spacing);
        }
    }
}


