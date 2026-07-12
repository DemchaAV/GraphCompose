package com.demcha.compose.document.node;

/**
 * Horizontal placement of a fixed-size block within the available content
 * width of a flow — the {@code margin: auto} / {@code align(center)} analogue
 * for nodes that do not fill the width on their own (paths, images, icons,
 * barcodes, shape containers).
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public enum HorizontalAlign {
    /** Flush with the left edge (the flow default). */
    LEFT,
    /** Centred in the available width. */
    CENTER,
    /** Flush with the right edge. */
    RIGHT
}
