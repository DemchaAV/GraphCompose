package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentColor;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Backend-neutral barcode payload configuration attached to semantic barcode nodes.
 *
 * @author Artem Demchyshyn
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DocumentBarcodeOptions {
    private final String content;

    @Builder.Default
    private final DocumentBarcodeType type = DocumentBarcodeType.QR_CODE;

    @Builder.Default
    private final DocumentColor foreground = DocumentColor.BLACK;

    @Builder.Default
    private final DocumentColor background = DocumentColor.WHITE;

    @Builder.Default
    private final int quietZoneMargin = 0;

    private DocumentBarcodeOptions() {
        this.content = null;
        this.type = DocumentBarcodeType.QR_CODE;
        this.foreground = DocumentColor.BLACK;
        this.background = DocumentColor.WHITE;
        this.quietZoneMargin = 0;
    }
}
