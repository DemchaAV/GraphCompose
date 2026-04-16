package com.demcha.compose.document.backend.fixed.pdf.options;

/**
 * Z-layer for a document-wide watermark relative to main content.
 */
public enum PdfWatermarkLayer {
    /**
     * Draws the watermark before the main content so the watermark appears in
     * the background.
     */
    BEHIND_CONTENT,

    /**
     * Draws the watermark after the main content so the watermark overlays the
     * page.
     */
    ABOVE_CONTENT
}
