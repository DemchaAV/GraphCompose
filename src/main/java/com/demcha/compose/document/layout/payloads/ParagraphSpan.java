package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.node.DocumentLinkOptions;

/**
 * One measured span inside a paragraph line. Sealed because the wrapping
 * algorithm can produce either text spans or image spans for the same
 * line — both contribute to wrapping width and per-line height.
 */
public sealed interface ParagraphSpan permits ParagraphTextSpan, ParagraphImageSpan {
    /**
     * @return measured span width in points
     */
    double width();

    /**
     * @return optional link metadata anchored to this span
     */
    DocumentLinkOptions linkOptions();

    /**
     * @return effective height contribution for line metrics (font line
     *         height for text spans, image height for image spans)
     */
    double height();
}
