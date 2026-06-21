package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTransform;

import java.util.Objects;

/**
 * Atomic semantic barcode or QR-code node.
 *
 * <p>The node carries backend-neutral barcode payload data that the canonical
 * PDF backend turns into a bitmap at render time.</p>
 *
 * @param name            node name used in snapshots and layout graph paths
 * @param barcodeOptions  canonical barcode payload
 * @param width           target rendered width
 * @param height          target rendered height
 * @param linkTarget      optional node-level link target (external URI or internal anchor)
 * @param bookmarkOptions optional node-level bookmark metadata
 * @param padding         inner padding
 * @param margin          outer margin
 * @param transform       render-time affine transform; defaults to
 *                        {@link DocumentTransform#NONE}.
 * @param anchor          optional in-document navigation anchor name at the barcode's
 *                        top-left, or {@code null} for none
 */
public record BarcodeNode(
        String name,
        DocumentBarcodeOptions barcodeOptions,
        double width,
        double height,
        DocumentLinkTarget linkTarget,
        DocumentBookmarkOptions bookmarkOptions,
        DocumentInsets padding,
        DocumentInsets margin,
        DocumentTransform transform,
        String anchor
) implements DocumentNode {
    /**
     * Creates a validated barcode or QR-code node.
     */
    public BarcodeNode {
        name = name == null ? "" : name;
        barcodeOptions = Objects.requireNonNull(barcodeOptions, "barcodeOptions");
        padding = padding == null ? DocumentInsets.zero() : padding;
        margin = margin == null ? DocumentInsets.zero() : margin;
        transform = transform == null ? DocumentTransform.NONE : transform;
        anchor = anchor == null || anchor.isBlank() ? null : anchor.trim();
        if (barcodeOptions.getContent() == null || barcodeOptions.getContent().isBlank()) {
            throw new IllegalArgumentException("barcodeOptions.content must not be blank.");
        }
        if (width <= 0 || Double.isNaN(width) || Double.isInfinite(width)) {
            throw new IllegalArgumentException("width must be finite and positive: " + width);
        }
        if (height <= 0 || Double.isNaN(height) || Double.isInfinite(height)) {
            throw new IllegalArgumentException("height must be finite and positive: " + height);
        }
    }

    /**
     * Backwards-compatible canonical constructor taking external
     * {@link DocumentLinkOptions} (wrapped) and no navigation anchor.
     *
     * @param name            node name used in snapshots and layout graph paths
     * @param barcodeOptions  canonical barcode payload
     * @param width           target rendered width
     * @param height          target rendered height
     * @param linkOptions     optional external link metadata
     * @param bookmarkOptions optional node-level bookmark metadata
     * @param padding         inner padding
     * @param margin          outer margin
     * @param transform       render-time affine transform
     */
    public BarcodeNode(String name,
                       DocumentBarcodeOptions barcodeOptions,
                       double width,
                       double height,
                       DocumentLinkOptions linkOptions,
                       DocumentBookmarkOptions bookmarkOptions,
                       DocumentInsets padding,
                       DocumentInsets margin,
                       DocumentTransform transform) {
        this(name, barcodeOptions, width, height,
                linkOptions == null ? null : new ExternalLinkTarget(linkOptions),
                bookmarkOptions, padding, margin, transform, null);
    }

    /**
     * Backward-compatible convenience constructor without link/bookmark metadata.
     *
     * @param name           node name used in snapshots and layout graph paths
     * @param barcodeOptions canonical barcode payload
     * @param width          target rendered width
     * @param height         target rendered height
     * @param padding        inner padding
     * @param margin         outer margin
     */
    public BarcodeNode(String name,
                       DocumentBarcodeOptions barcodeOptions,
                       double width,
                       double height,
                       DocumentInsets padding,
                       DocumentInsets margin) {
        this(name, barcodeOptions, width, height, null, null, padding, margin, DocumentTransform.NONE);
    }

    /**
     * Backward-compatible convenience constructor without transform — defaults
     * to {@link DocumentTransform#NONE}.
     *
     * @param name            node name used in snapshots and layout graph paths
     * @param barcodeOptions  canonical barcode payload
     * @param width           target rendered width
     * @param height          target rendered height
     * @param linkOptions     optional node-level link metadata
     * @param bookmarkOptions optional node-level bookmark metadata
     * @param padding         inner padding
     * @param margin          outer margin
     */
    public BarcodeNode(String name,
                       DocumentBarcodeOptions barcodeOptions,
                       double width,
                       double height,
                       DocumentLinkOptions linkOptions,
                       DocumentBookmarkOptions bookmarkOptions,
                       DocumentInsets padding,
                       DocumentInsets margin) {
        this(name, barcodeOptions, width, height, linkOptions, bookmarkOptions, padding, margin, DocumentTransform.NONE);
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
