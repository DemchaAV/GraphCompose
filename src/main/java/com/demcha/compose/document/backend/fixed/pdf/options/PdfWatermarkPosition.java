package com.demcha.compose.document.backend.fixed.pdf.options;

/**
 * Predefined page positions for document-wide watermarks.
 */
public enum PdfWatermarkPosition {
    /**
     * Center of the page.
     */
    CENTER,
    /**
     * Top-left page corner.
     */
    TOP_LEFT,
    /**
     * Top-right page corner.
     */
    TOP_RIGHT,
    /**
     * Bottom-left page corner.
     */
    BOTTOM_LEFT,
    /**
     * Bottom-right page corner.
     */
    BOTTOM_RIGHT,
    /**
     * Repeated tiled watermark pattern.
     */
    TILE
}
