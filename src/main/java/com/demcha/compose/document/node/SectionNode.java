package com.demcha.compose.document.node;

import com.demcha.compose.engine.components.content.shape.Stroke;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.document.node.DocumentNode;

import java.awt.Color;
import java.util.List;
import java.util.Objects;

/**
 * Vertical semantic section node.
 *
 * @param name node name used in snapshots and layout graph paths
 * @param children child semantic nodes in source order
 * @param spacing vertical spacing between children
 * @param padding inner padding
 * @param margin outer margin
 * @param fillColor optional background fill
 * @param stroke optional border stroke
 */
public record SectionNode(
        String name,
        List<DocumentNode> children,
        double spacing,
        Padding padding,
        Margin margin,
        Color fillColor,
        Stroke stroke
) implements DocumentNode {
    /**
     * Normalizes optional section fields and validates child spacing.
     */
    public SectionNode {
        name = name == null ? "" : name;
        Objects.requireNonNull(children, "children");
        children = List.copyOf(children);
        padding = padding == null ? Padding.zero() : padding;
        margin = margin == null ? Margin.zero() : margin;
        if (spacing < 0 || Double.isNaN(spacing) || Double.isInfinite(spacing)) {
            throw new IllegalArgumentException("spacing must be finite and non-negative: " + spacing);
        }
    }
}


