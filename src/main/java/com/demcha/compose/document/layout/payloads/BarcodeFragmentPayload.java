package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.engine.components.content.barcode.BarcodeData;

/**
 * PDF payload for a resolved barcode fragment.
 *
 * @param barcodeData encoded barcode payload
 * @param linkOptions optional fragment-level link metadata
 * @param bookmarkOptions optional fragment-level bookmark metadata
 */
public record BarcodeFragmentPayload(
        BarcodeData barcodeData,
        DocumentLinkOptions linkOptions,
        DocumentBookmarkOptions bookmarkOptions
) implements PdfSemanticFragmentPayload {
}
