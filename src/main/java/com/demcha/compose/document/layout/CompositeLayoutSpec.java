package com.demcha.compose.document.layout;

/**
 * Generic child-layout contract for composite semantic nodes.
 *
 * @param spacing vertical spacing between child nodes
 */
public record CompositeLayoutSpec(double spacing) {
    /**
     * Creates a validated composite child-layout contract.
     */
    public CompositeLayoutSpec {
        if (spacing < 0 || Double.isNaN(spacing) || Double.isInfinite(spacing)) {
            throw new IllegalArgumentException("spacing must be finite and non-negative: " + spacing);
        }
    }
}


