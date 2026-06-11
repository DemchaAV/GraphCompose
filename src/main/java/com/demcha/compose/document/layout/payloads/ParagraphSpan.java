package com.demcha.compose.document.layout.payloads;

import com.demcha.compose.document.node.DocumentLinkOptions;

/**
 * One measured span inside a paragraph line. Sealed because the wrapping
 * algorithm can produce text, image or shape spans for the same line — all
 * contribute to wrapping width and per-line height.
 */
public sealed interface ParagraphSpan permits ParagraphTextSpan, ParagraphImageSpan, ParagraphShapeSpan {
    /**
     * Measured width of this span.
     *
     * @return measured span width in points
     */
    double width();

    /**
     * Link metadata anchored to this span, if any.
     *
     * @return optional link metadata anchored to this span
     */
    DocumentLinkOptions linkOptions();

    /**
     * Height this span contributes to its line.
     *
     * @return effective height contribution for line metrics (font line
     * height for text spans, image height for image spans)
     */
    double height();
}
