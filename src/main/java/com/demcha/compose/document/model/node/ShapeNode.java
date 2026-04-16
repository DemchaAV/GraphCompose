package com.demcha.compose.document.model.node;

import com.demcha.compose.document.backend.fixed.pdf.options.PdfBookmarkOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfLinkOptions;
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
        PdfLinkOptions linkOptions,
        PdfBookmarkOptions bookmarkOptions,
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

    /**
     * Backward-compatible convenience constructor without link/bookmark metadata.
     *
     * @param name node name used in snapshots and layout graph paths
     * @param width resolved shape width
     * @param height resolved shape height
     * @param fillColor fill color, or {@code null}
     * @param stroke stroke descriptor, or {@code null}
     * @param padding inner padding
     * @param margin outer margin
     */
    public ShapeNode(String name,
                     double width,
                     double height,
                     Color fillColor,
                     Stroke stroke,
                     Padding padding,
                     Margin margin) {
        this(name, width, height, fillColor, stroke, null, null, padding, margin);
    }
}


