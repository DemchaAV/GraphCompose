package com.demcha.compose.document.node;

import com.demcha.compose.document.backend.fixed.pdf.options.PdfBarcodeOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfBookmarkOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfLinkOptions;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;

import java.util.Objects;

/**
 * Atomic semantic barcode or QR-code node.
 *
 * <p>The node carries backend-neutral barcode payload data that the canonical
 * PDF backend turns into a bitmap at render time.</p>
 */
public record BarcodeNode(
        String name,
        PdfBarcodeOptions barcodeOptions,
        double width,
        double height,
        PdfLinkOptions linkOptions,
        PdfBookmarkOptions bookmarkOptions,
        Padding padding,
        Margin margin
) implements DocumentNode {
    public BarcodeNode {
        name = name == null ? "" : name;
        barcodeOptions = Objects.requireNonNull(barcodeOptions, "barcodeOptions");
        padding = padding == null ? Padding.zero() : padding;
        margin = margin == null ? Margin.zero() : margin;
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
                       Padding padding,
                       Margin margin) {
        this(name, barcodeOptions, width, height, null, null, padding, margin);
    }
}
