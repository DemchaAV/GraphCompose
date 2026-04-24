package com.demcha.compose.document.node;

import com.demcha.compose.document.backend.fixed.pdf.options.PdfBookmarkOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfLinkOptions;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.style.DocumentInsets;

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
        DocumentImageData imageData,
        Double width,
        Double height,
        PdfLinkOptions linkOptions,
        PdfBookmarkOptions bookmarkOptions,
        DocumentInsets padding,
        DocumentInsets margin
) implements DocumentNode {
    /**
     * Normalizes spacing defaults and validates explicit image dimensions.
     */
    public ImageNode {
        name = name == null ? "" : name;
        imageData = Objects.requireNonNull(imageData, "imageData");
        padding = padding == null ? DocumentInsets.zero() : padding;
        margin = margin == null ? DocumentInsets.zero() : margin;
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
                     DocumentImageData imageData,
                     Double width,
                     Double height,
                     DocumentInsets padding,
                     DocumentInsets margin) {
        this(name, imageData, width, height, null, null, padding, margin);
    }
}


