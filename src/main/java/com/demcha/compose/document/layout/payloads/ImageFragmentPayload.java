package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.image.DocumentImageFitMode;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.engine.components.content.ImageData;

/**
 * PDF payload for a resolved image fragment.
 *
 * @param imageData image source data
 * @param fitMode image fit policy used inside the resolved fragment
 * @param linkOptions optional fragment-level link metadata
 * @param bookmarkOptions optional fragment-level bookmark metadata
 */
public record ImageFragmentPayload(
        ImageData imageData,
        DocumentImageFitMode fitMode,
        DocumentLinkOptions linkOptions,
        DocumentBookmarkOptions bookmarkOptions
) implements PdfSemanticFragmentPayload {
    /**
     * Normalizes optional fit policy.
     */
    public ImageFragmentPayload {
        fitMode = fitMode == null ? DocumentImageFitMode.STRETCH : fitMode;
    }
}
