package com.demcha.compose.document.node;

import com.demcha.compose.document.backend.fixed.pdf.options.PdfBookmarkOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfLinkOptions;
import com.demcha.compose.engine.components.content.ImageData;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.document.node.DocumentNode;

import java.util.Objects;

/**
 * Atomic semantic image node.
 *
 * @param name node name used in snapshots and layout graph paths
 * @param imageData semantic image payload
 * @param width optional target width
 * @param height optional target height
 * @param linkOptions optional node-level link metadata
 * @param bookmarkOptions optional node-level bookmark metadata
 * @param padding inner padding
 * @param margin outer margin
 */
public record ImageNode(
        String name,
        ImageData imageData,
        Double width,
        Double height,
        PdfLinkOptions linkOptions,
        PdfBookmarkOptions bookmarkOptions,
        Padding padding,
        Margin margin
) implements DocumentNode {
    /**
     * Normalizes spacing defaults and validates explicit image dimensions.
     */
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

    /**
     * Backward-compatible convenience constructor without link/bookmark metadata.
     *
     * @param name node name used in snapshots and layout graph paths
     * @param imageData semantic image payload
     * @param width optional target width
     * @param height optional target height
     * @param padding inner padding
     * @param margin outer margin
     */
    public ImageNode(String name,
                     ImageData imageData,
                     Double width,
                     Double height,
                     Padding padding,
                     Margin margin) {
        this(name, imageData, width, height, null, null, padding, margin);
    }
}


