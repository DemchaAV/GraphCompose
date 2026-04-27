package com.demcha.compose.document.layout;

import java.util.List;

/**
 * Generic child-layout contract for composite semantic nodes.
 *
 * <p>The default contract describes a vertical flow with even sibling spacing.
 * Horizontal rows additionally carry optional per-child weights that govern how
 * the row's inner width is distributed across children.</p>
 *
 * @param spacing spacing between child nodes (vertical for {@link Axis#VERTICAL}, horizontal for {@link Axis#HORIZONTAL})
 * @param axis composite stacking axis
 * @param weights optional per-child weights (only consulted for {@link Axis#HORIZONTAL})
 */
public record CompositeLayoutSpec(double spacing, Axis axis, List<Double> weights) {
    /**
     * Composite stacking axis.
     */
    public enum Axis {
        /** Children stack top to bottom. */
        VERTICAL,
        /** Children flow left to right inside a single row band. */
        HORIZONTAL,
        /**
         * Children share the same bounding box and are painted in source order
         * (first child behind, last child in front). Used by {@code LayerStackNode}
         * to compose background panels, watermarks, and overlay decorations.
         */
        STACK
    }

    /**
     * Creates a validated composite child-layout contract.
     */
    public CompositeLayoutSpec {
        if (spacing < 0 || Double.isNaN(spacing) || Double.isInfinite(spacing)) {
            throw new IllegalArgumentException("spacing must be finite and non-negative: " + spacing);
        }
        axis = axis == null ? Axis.VERTICAL : axis;
        weights = weights == null ? List.of() : List.copyOf(weights);
        for (Double weight : weights) {
            if (weight == null || Double.isNaN(weight) || Double.isInfinite(weight) || weight <= 0.0) {
                throw new IllegalArgumentException("weights must be positive finite numbers: " + weights);
            }
        }
    }

    /**
     * Convenience constructor for the default vertical axis.
     *
     * @param spacing vertical spacing between children
     */
    public CompositeLayoutSpec(double spacing) {
        this(spacing, Axis.VERTICAL, List.of());
    }

    /**
     * Convenience constructor without explicit weights.
     *
     * @param spacing spacing between children
     * @param axis stacking axis
     */
    public CompositeLayoutSpec(double spacing, Axis axis) {
        this(spacing, axis, List.of());
    }
}
