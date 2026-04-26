package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentInsets;

/**
 * Invisible fixed-size semantic spacer.
 *
 * @param name node name used in snapshots and layout graph paths
 * @param width spacer width contribution
 * @param height spacer height contribution
 * @param padding inner padding
 * @param margin outer margin
 * @author Artem Demchyshyn
 */
public record SpacerNode(
        String name,
        double width,
        double height,
        DocumentInsets padding,
        DocumentInsets margin
) implements DocumentNode {
    /**
     * Normalizes spacing defaults and validates spacer dimensions.
     */
    public SpacerNode {
        name = name == null ? "" : name;
        padding = padding == null ? DocumentInsets.zero() : padding;
        margin = margin == null ? DocumentInsets.zero() : margin;
        if (width < 0 || Double.isNaN(width) || Double.isInfinite(width)) {
            throw new IllegalArgumentException("width must be finite and non-negative: " + width);
        }
        if (height < 0 || Double.isNaN(height) || Double.isInfinite(height)) {
            throw new IllegalArgumentException("height must be finite and non-negative: " + height);
        }
    }
}
