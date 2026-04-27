package com.demcha.compose.document.templates.support.common;

import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;

import java.util.List;
import java.util.Objects;

/**
 * One vertical column inside a template row.
 *
 * @param name semantic column name
 * @param blocks blocks rendered top-to-bottom inside the column
 * @param spacing vertical spacing between blocks
 * @param padding inner column padding
 * @param margin outer column margin
 */
public record TemplateColumnSpec(
        String name,
        List<TemplateModuleBlock> blocks,
        double spacing,
        Padding padding,
        Margin margin
) {
    /**
     * Creates a normalized column specification.
     */
    public TemplateColumnSpec {
        name = name == null ? "" : name;
        blocks = blocks == null ? List.of() : List.copyOf(blocks);
        if (spacing < 0 || Double.isNaN(spacing) || Double.isInfinite(spacing)) {
            throw new IllegalArgumentException("spacing must be finite and non-negative: " + spacing);
        }
        padding = padding == null ? Padding.zero() : padding;
        margin = margin == null ? Margin.zero() : margin;
    }

    /**
     * Creates a zero-padding, zero-margin column.
     *
     * @param name semantic column name
     * @param blocks column body blocks
     * @param spacing vertical spacing between blocks
     * @return column specification
     */
    public static TemplateColumnSpec of(String name, List<TemplateModuleBlock> blocks, double spacing) {
        return new TemplateColumnSpec(name, blocks, spacing, Padding.zero(), Margin.zero());
    }

    /**
     * Creates a zero-padding, zero-margin column from varargs blocks.
     *
     * @param name semantic column name
     * @param spacing vertical spacing between blocks
     * @param blocks column body blocks
     * @return column specification
     */
    public static TemplateColumnSpec of(String name, double spacing, TemplateModuleBlock... blocks) {
        return of(name, blocks == null ? List.of() : List.of(blocks), spacing);
    }
}
