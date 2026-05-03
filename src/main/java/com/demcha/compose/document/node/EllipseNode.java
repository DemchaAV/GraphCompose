package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTransform;

/**
 * Atomic ellipse or circle semantic shape.
 *
 * @param name node name used in snapshots and layout graph paths
 * @param width resolved ellipse width
 * @param height resolved ellipse height
 * @param fillColor optional fill color
 * @param stroke optional stroke descriptor
 * @param linkOptions optional node-level link metadata
 * @param bookmarkOptions optional node-level bookmark metadata
 * @param padding inner padding
 * @param margin outer margin
 * @param transform render-time affine transform; defaults to
 *                  {@link DocumentTransform#NONE}.
 * @author Artem Demchyshyn
 */
public record EllipseNode(
        String name,
        double width,
        double height,
        DocumentColor fillColor,
        DocumentStroke stroke,
        DocumentLinkOptions linkOptions,
        DocumentBookmarkOptions bookmarkOptions,
        DocumentInsets padding,
        DocumentInsets margin,
        DocumentTransform transform
) implements DocumentNode {
    /**
     * Normalizes spacing defaults and validates explicit ellipse dimensions.
     */
    public EllipseNode {
        name = name == null ? "" : name;
        padding = padding == null ? DocumentInsets.zero() : padding;
        margin = margin == null ? DocumentInsets.zero() : margin;
        transform = transform == null ? DocumentTransform.NONE : transform;
        if (width <= 0 || Double.isNaN(width) || Double.isInfinite(width)) {
            throw new IllegalArgumentException("width must be finite and positive: " + width);
        }
        if (height <= 0 || Double.isNaN(height) || Double.isInfinite(height)) {
            throw new IllegalArgumentException("height must be finite and positive: " + height);
        }
    }

    /**
     * Backward-compatible convenience constructor without transform — defaults
     * to {@link DocumentTransform#NONE}.
     */
    public EllipseNode(String name,
                       double width,
                       double height,
                       DocumentColor fillColor,
                       DocumentStroke stroke,
                       DocumentLinkOptions linkOptions,
                       DocumentBookmarkOptions bookmarkOptions,
                       DocumentInsets padding,
                       DocumentInsets margin) {
        this(name, width, height, fillColor, stroke, linkOptions, bookmarkOptions, padding, margin, DocumentTransform.NONE);
    }
}
