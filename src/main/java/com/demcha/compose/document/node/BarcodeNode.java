package com.demcha.compose.document.node;

import com.demcha.compose.document.backend.fixed.pdf.options.PdfBarcodeOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfBookmarkOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfLinkOptions;
import com.demcha.compose.document.style.DocumentInsets;

import java.util.Objects;

/**
 * Atomic semantic barcode or QR-code node.
 *
 * <p>The node carries backend-neutral barcode payload data that the canonical
 * PDF backend turns into a bitmap at render time.</p>
 *
 * @param name node name used in snapshots and layout graph paths
 * @param barcodeOptions canonical barcode payload
 * @param width target rendered width
 * @param height target rendered height
 * @param linkOptions optional node-level link metadata
 * @param bookmarkOptions optional node-level bookmark metadata
 * @param padding inner padding
 * @param margin outer margin
 */
public record BarcodeNode(
        String name,
        PdfBarcodeOptions barcodeOptions,
        double width,
        double height,
        PdfLinkOptions linkOptions,
        PdfBookmarkOptions bookmarkOptions,
        DocumentInsets padding,
        DocumentInsets margin
) implements DocumentNode {
    /**
     * Creates a validated barcode or QR-code node.
     */
    public BarcodeNode {
        name = name == null ? "" : name;
        barcodeOptions = Objects.requireNonNull(barcodeOptions, "barcodeOptions");
        padding = padding == null ? DocumentInsets.zero() : padding;
        margin = margin == null ? DocumentInsets.zero() : margin;
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
     * Backward-compatible convenience constructor without link/bookmark metadata.
     *
     * @param name node name used in snapshots and layout graph paths
     * @param barcodeOptions canonical barcode payload
     * @param width target rendered width
     * @param height target rendered height
     * @param padding inner padding
     * @param margin outer margin
     */
    public BarcodeNode(String name,
                       PdfBarcodeOptions barcodeOptions,
                       double width,
                       double height,
                       DocumentInsets padding,
                       DocumentInsets margin) {
        this(name, barcodeOptions, width, height, null, null, padding, margin);
    }
}
