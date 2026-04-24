package com.demcha.compose.document.backend.fixed.pdf.options;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.awt.Color;

/**
 * Canonical barcode payload configuration attached to semantic barcode nodes.
 *
 * <p>The semantic compiler carries these options into the resolved layout graph,
 * and the canonical PDF backend generates the bitmap at render time.</p>
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class PdfBarcodeOptions {
    private final String content;

    @Builder.Default
    private final PdfBarcodeType type = PdfBarcodeType.QR_CODE;

    @Builder.Default
    private final Color foreground = Color.BLACK;

    @Builder.Default
    private final Color background = Color.WHITE;

    @Builder.Default
    private final int quietZoneMargin = 0;

    private PdfBarcodeOptions() {
        this.content = null;
        this.type = PdfBarcodeType.QR_CODE;
        this.foreground = Color.BLACK;
        this.background = Color.WHITE;
        this.quietZoneMargin = 0;
    }
}
