package com.demcha.compose.document.model.node;

import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.document.model.node.DocumentNode;

import java.awt.Color;

/**
 * Atomic rectangle-like semantic shape.
 */
public record ShapeNode(
        String name,
        double width,
        double height,
        Color fillColor,
        Stroke stroke,
        Padding padding,
        Margin margin
) implements DocumentNode {
    public ShapeNode {
        name = name == null ? "" : name;
        padding = padding == null ? Padding.zero() : padding;
        margin = margin == null ? Margin.zero() : margin;
        if (width <= 0 || Double.isNaN(width) || Double.isInfinite(width)) {
            throw new IllegalArgumentException("width must be finite and positive: " + width);
        }
        if (height <= 0 || Double.isNaN(height) || Double.isInfinite(height)) {
            throw new IllegalArgumentException("height must be finite and positive: " + height);
        }
    }
}


