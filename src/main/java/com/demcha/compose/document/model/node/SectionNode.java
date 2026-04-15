package com.demcha.compose.document.model.node;

import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.document.model.node.DocumentNode;

import java.awt.Color;
import java.util.List;
import java.util.Objects;

/**
 * Vertical semantic section node.
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


