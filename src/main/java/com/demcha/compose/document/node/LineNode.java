package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;

/**
 * Atomic semantic line drawn inside a fixed-size box.
 *
 * @param name node name used in snapshots and layout graph paths
 * @param width resolved line box width
 * @param height resolved line box height
 * @param startX line start x offset inside the box
 * @param startY line start y offset inside the box
 * @param endX line end x offset inside the box
 * @param endY line end y offset inside the box
 * @param stroke line stroke descriptor
 * @param linkOptions optional node-level link metadata
 * @param bookmarkOptions optional node-level bookmark metadata
 * @param padding inner padding
 * @param margin outer margin
 * @author Artem Demchyshyn
 */
public record LineNode(
        String name,
        double width,
        double height,
        double startX,
        double startY,
        double endX,
        double endY,
        DocumentStroke stroke,
        DocumentLinkOptions linkOptions,
        DocumentBookmarkOptions bookmarkOptions,
        DocumentInsets padding,
        DocumentInsets margin
) implements DocumentNode {
    /**
     * Normalizes spacing defaults and validates explicit line geometry.
     */
    public LineNode {
        name = name == null ? "" : name;
        padding = padding == null ? DocumentInsets.zero() : padding;
        margin = margin == null ? DocumentInsets.zero() : margin;
        requireNonNegativeFinite(width, "width");
        requireNonNegativeFinite(height, "height");
        requireFinite(startX, "startX");
        requireFinite(startY, "startY");
        requireFinite(endX, "endX");
        requireFinite(endY, "endY");
    }

    private static void requireNonNegativeFinite(double value, String name) {
        if (value < 0 || Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(name + " must be finite and non-negative: " + value);
        }
    }

    private static void requireFinite(double value, String name) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(name + " must be finite: " + value);
        }
    }
}
