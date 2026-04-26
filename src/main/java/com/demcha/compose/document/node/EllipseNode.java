package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;

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
        DocumentInsets margin
) implements DocumentNode {
    /**
     * Normalizes spacing defaults and validates explicit ellipse dimensions.
     */
    public EllipseNode {
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
}
