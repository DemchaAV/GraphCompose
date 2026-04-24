package com.demcha.compose.document.node;

import com.demcha.compose.document.backend.fixed.pdf.options.PdfBookmarkOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfLinkOptions;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;

import java.awt.Color;

/**
 * Atomic rectangle-like semantic shape.
 *
 * @param name node name used in snapshots and layout graph paths
 * @param width resolved shape width
 * @param height resolved shape height
 * @param fillColor optional fill color
 * @param stroke optional stroke descriptor
 * @param linkOptions optional node-level link metadata
 * @param bookmarkOptions optional node-level bookmark metadata
 * @param padding inner padding
 * @param margin outer margin
 */
public record ShapeNode(
        String name,
        double width,
        double height,
        DocumentColor fillColor,
        DocumentStroke stroke,
        PdfLinkOptions linkOptions,
        PdfBookmarkOptions bookmarkOptions,
        DocumentInsets padding,
        DocumentInsets margin
) implements DocumentNode {
    /**
     * Normalizes spacing defaults and validates explicit shape dimensions.
     */
    public ShapeNode {
        name = name == null ? "" : name;
        padding = padding == null ? DocumentInsets.zero() : padding;
        margin = margin == null ? DocumentInsets.zero() : margin;
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
                     DocumentStroke stroke,
                     DocumentInsets padding,
                     DocumentInsets margin) {
        this(name, width, height, fillColor == null ? null : DocumentColor.of(fillColor), stroke, null, null, padding, margin);
    }
}


