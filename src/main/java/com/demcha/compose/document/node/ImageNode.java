package com.demcha.compose.document.node;

import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.image.DocumentImageFitMode;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTransform;

import java.util.Objects;

/**
 * Atomic semantic image node.
 *
 * @param name            node name used in snapshots and layout graph paths
 * @param imageData       semantic image payload
 * @param width           optional target width
 * @param height          optional target height
 * @param scale           optional uniform scale applied when width and height are omitted
 * @param fitMode         image fit policy used when drawing inside explicit bounds
 * @param linkTarget      optional node-level link target (external URI or internal anchor)
 * @param bookmarkOptions optional node-level bookmark metadata
 * @param padding         inner padding
 * @param margin          outer margin
 * @param transform       render-time affine transform; defaults to
 *                        {@link DocumentTransform#NONE}.
 * @param anchor          optional in-document navigation anchor name at the image's
 *                        top-left, or {@code null} for none
 * @author Artem Demchyshyn
 */
public record ImageNode(
        String name,
        DocumentImageData imageData,
        Double width,
        Double height,
        Double scale,
        DocumentImageFitMode fitMode,
        DocumentLinkTarget linkTarget,
        DocumentBookmarkOptions bookmarkOptions,
        DocumentInsets padding,
        DocumentInsets margin,
        DocumentTransform transform,
        String anchor
) implements DocumentNode {
    /**
     * Normalizes spacing defaults and validates explicit image dimensions.
     */
    public ImageNode {
        name = name == null ? "" : name;
        imageData = Objects.requireNonNull(imageData, "imageData");
        padding = padding == null ? DocumentInsets.zero() : padding;
        margin = margin == null ? DocumentInsets.zero() : margin;
        fitMode = fitMode == null ? DocumentImageFitMode.STRETCH : fitMode;
        transform = transform == null ? DocumentTransform.NONE : transform;
        anchor = anchor == null || anchor.isBlank() ? null : anchor.trim();
        if (width != null && (width <= 0 || Double.isNaN(width) || Double.isInfinite(width))) {
            throw new IllegalArgumentException("width must be finite and positive when set: " + width);
        }
        if (height != null && (height <= 0 || Double.isNaN(height) || Double.isInfinite(height))) {
            throw new IllegalArgumentException("height must be finite and positive when set: " + height);
        }
        if (scale != null && (scale <= 0 || Double.isNaN(scale) || Double.isInfinite(scale))) {
            throw new IllegalArgumentException("scale must be finite and positive when set: " + scale);
        }
    }

    /**
     * Backwards-compatible canonical constructor taking external
     * {@link DocumentLinkOptions} (wrapped) and no navigation anchor.
     *
     * @param name            node name used in snapshots and layout graph paths
     * @param imageData       semantic image payload
     * @param width           optional target width
     * @param height          optional target height
     * @param scale           optional uniform scale applied when width and height are omitted
     * @param fitMode         image fit policy used when drawing inside explicit bounds
     * @param linkOptions     optional external link metadata
     * @param bookmarkOptions optional node-level bookmark metadata
     * @param padding         inner padding
     * @param margin          outer margin
     * @param transform       render-time affine transform
     */
    public ImageNode(String name,
                     DocumentImageData imageData,
                     Double width,
                     Double height,
                     Double scale,
                     DocumentImageFitMode fitMode,
                     DocumentLinkOptions linkOptions,
                     DocumentBookmarkOptions bookmarkOptions,
                     DocumentInsets padding,
                     DocumentInsets margin,
                     DocumentTransform transform) {
        this(name, imageData, width, height, scale, fitMode,
                linkOptions == null ? null : new ExternalLinkTarget(linkOptions),
                bookmarkOptions, padding, margin, transform, null);
    }

    /**
     * Backward-compatible convenience constructor without link/bookmark metadata.
     *
     * @param name      node name used in snapshots and layout graph paths
     * @param imageData semantic image payload
     * @param width     optional target width
     * @param height    optional target height
     * @param padding   inner padding
     * @param margin    outer margin
     */
    public ImageNode(String name,
                     DocumentImageData imageData,
                     Double width,
                     Double height,
                     DocumentInsets padding,
                     DocumentInsets margin) {
        this(name, imageData, width, height, null, DocumentImageFitMode.STRETCH, null, null, padding, margin, DocumentTransform.NONE);
    }

    /**
     * Backward-compatible convenience constructor without image fit options.
     *
     * @param name            node name used in snapshots and layout graph paths
     * @param imageData       semantic image payload
     * @param width           optional target width
     * @param height          optional target height
     * @param linkOptions     optional node-level link metadata
     * @param bookmarkOptions optional node-level bookmark metadata
     * @param padding         inner padding
     * @param margin          outer margin
     */
    public ImageNode(String name,
                     DocumentImageData imageData,
                     Double width,
                     Double height,
                     DocumentLinkOptions linkOptions,
                     DocumentBookmarkOptions bookmarkOptions,
                     DocumentInsets padding,
                     DocumentInsets margin) {
        this(name, imageData, width, height, null, DocumentImageFitMode.STRETCH, linkOptions, bookmarkOptions, padding, margin, DocumentTransform.NONE);
    }

    /**
     * Backward-compatible convenience constructor without transform — defaults
     * to {@link DocumentTransform#NONE}.
     *
     * @param name            node name used in snapshots and layout graph paths
     * @param imageData       semantic image payload
     * @param width           optional target width
     * @param height          optional target height
     * @param scale           optional uniform scale applied when width and height are omitted
     * @param fitMode         image fit policy used when drawing inside explicit bounds
     * @param linkOptions     optional node-level link metadata
     * @param bookmarkOptions optional node-level bookmark metadata
     * @param padding         inner padding
     * @param margin          outer margin
     */
    public ImageNode(String name,
                     DocumentImageData imageData,
                     Double width,
                     Double height,
                     Double scale,
                     DocumentImageFitMode fitMode,
                     DocumentLinkOptions linkOptions,
                     DocumentBookmarkOptions bookmarkOptions,
                     DocumentInsets padding,
                     DocumentInsets margin) {
        this(name, imageData, width, height, scale, fitMode, linkOptions, bookmarkOptions, padding, margin, DocumentTransform.NONE);
    }

    /**
     * Returns the external link options, or {@code null} when the node has no
     * link or targets an internal anchor.
     *
     * @return external link metadata, or {@code null}
     * @deprecated use {@link #linkTarget()}; this bridge only exposes external links
     */
    @Deprecated(since = "1.9.0")
    public DocumentLinkOptions linkOptions() {
        return linkTarget instanceof ExternalLinkTarget external ? external.options() : null;
    }
}


