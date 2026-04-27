package com.demcha.compose.document.output;

/**
 * Backend-neutral watermark anchor positions. The set mirrors the value range
 * supported by the canonical PDF backend; backends that recognise fewer
 * positions may approximate.
 *
 * @author Artem Demchyshyn
 */
public enum DocumentWatermarkPosition {
    CENTER,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    /** Repeated tiled watermark pattern across the page. */
    TILE
}
