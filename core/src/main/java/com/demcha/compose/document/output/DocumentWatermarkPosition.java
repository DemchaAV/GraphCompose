package com.demcha.compose.document.output;

/**
 * Backend-neutral watermark anchor positions. The set mirrors the value range
 * supported by the canonical PDF backend; backends that recognise fewer
 * positions may approximate.
 *
 * @author Artem Demchyshyn
 */
public enum DocumentWatermarkPosition {
    /**
     * Single watermark centred on the page.
     */
    CENTER,
    /**
     * Single watermark anchored to the top-left corner.
     */
    TOP_LEFT,
    /**
     * Single watermark anchored to the top-right corner.
     */
    TOP_RIGHT,
    /**
     * Single watermark anchored to the bottom-left corner.
     */
    BOTTOM_LEFT,
    /**
     * Single watermark anchored to the bottom-right corner.
     */
    BOTTOM_RIGHT,
    /**
     * Repeated tiled watermark pattern across the page.
     */
    TILE
}
