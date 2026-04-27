package com.demcha.compose.document.templates.support.common;

import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;

import java.util.List;

/**
 * Horizontal row instruction for canonical template scene composers.
 *
 * @param name semantic row name
 * @param columns vertical sections rendered left-to-right
 * @param weights optional per-column width weights
 * @param gap horizontal gap between columns
 * @param padding inner row padding
 * @param margin outer row margin
 */
public record TemplateRowSpec(
        String name,
        List<TemplateColumnSpec> columns,
        List<Double> weights,
        double gap,
        Padding padding,
        Margin margin
) {
    /**
     * Creates a normalized row specification.
     */
    public TemplateRowSpec {
        name = name == null ? "" : name;
        columns = columns == null ? List.of() : List.copyOf(columns);
        weights = weights == null ? List.of() : List.copyOf(weights);
        if (!weights.isEmpty() && weights.size() != columns.size()) {
            throw new IllegalArgumentException("weights size must match columns size.");
        }
        for (Double weight : weights) {
            if (weight == null || weight <= 0 || Double.isNaN(weight) || Double.isInfinite(weight)) {
                throw new IllegalArgumentException("weights must be positive finite numbers: " + weights);
            }
        }
        if (gap < 0 || Double.isNaN(gap) || Double.isInfinite(gap)) {
            throw new IllegalArgumentException("gap must be finite and non-negative: " + gap);
        }
        padding = padding == null ? Padding.zero() : padding;
        margin = margin == null ? Margin.zero() : margin;
    }

    /**
     * Creates a weighted row with zero padding and margin.
     *
     * @param name semantic row name
     * @param columns row columns
     * @param weights per-column weights
     * @param gap horizontal gap
     * @return row specification
     */
    public static TemplateRowSpec weighted(String name, List<TemplateColumnSpec> columns, List<Double> weights, double gap) {
        return new TemplateRowSpec(name, columns, weights, gap, Padding.zero(), Margin.zero());
    }
}
