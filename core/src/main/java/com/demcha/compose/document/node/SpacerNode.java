package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentInsets;

/**
 * Invisible fixed-size semantic spacer.
 *
 * @param name    node name used in snapshots and layout graph paths
 * @param width   spacer width contribution
 * @param height  spacer height contribution
 * @param padding inner padding
 * @param margin  outer margin
 * @param grow    flex grow factor inside a row: {@code 0} (the default) is a rigid
 *                spacer; {@code > 0} makes it a spring that absorbs a share of the
 *                row's leftover width proportional to its grow factor
 * @author Artem Demchyshyn
 */
public record SpacerNode(
        String name,
        double width,
        double height,
        DocumentInsets padding,
        DocumentInsets margin,
        double grow
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
        if (grow < 0 || Double.isNaN(grow) || Double.isInfinite(grow)) {
            throw new IllegalArgumentException("grow must be finite and non-negative: " + grow);
        }
    }

    /**
     * Backwards-compatible constructor for a rigid spacer ({@code grow == 0}).
     *
     * @param name    node name used in snapshots and layout graph paths
     * @param width   spacer width contribution
     * @param height  spacer height contribution
     * @param padding inner padding
     * @param margin  outer margin
     */
    public SpacerNode(String name, double width, double height, DocumentInsets padding, DocumentInsets margin) {
        this(name, width, height, padding, margin, 0.0);
    }
}
