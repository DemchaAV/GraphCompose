package com.demcha.compose.v2.nodes;

import com.demcha.compose.layout_core.components.content.ImageData;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.v2.DocumentNode;

import java.util.Objects;

/**
 * Atomic semantic image node.
 */
public record ImageNode(
        String name,
        ImageData imageData,
        Double width,
        Double height,
        Padding padding,
        Margin margin
) implements DocumentNode {
    public ImageNode {
        name = name == null ? "" : name;
        imageData = Objects.requireNonNull(imageData, "imageData");
        padding = padding == null ? Padding.zero() : padding;
        margin = margin == null ? Margin.zero() : margin;
        if (width != null && (width <= 0 || Double.isNaN(width) || Double.isInfinite(width))) {
            throw new IllegalArgumentException("width must be finite and positive when set: " + width);
        }
        if (height != null && (height <= 0 || Double.isNaN(height) || Double.isInfinite(height))) {
            throw new IllegalArgumentException("height must be finite and positive when set: " + height);
        }
    }
}
