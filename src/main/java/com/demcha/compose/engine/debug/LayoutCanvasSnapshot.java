package com.demcha.compose.engine.debug;

/**
 * Snapshot-friendly projection of the resolved document canvas.
 *
 * <p>This captures the page box, inner drawable area, and outer margin that
 * were active when the layout snapshot was produced.</p>
 *
 * @param pageWidth full page width
 * @param pageHeight full page height
 * @param innerWidth drawable width after margins
 * @param innerHeight drawable height after margins
 * @param margin resolved canvas margin
 */
public record LayoutCanvasSnapshot(
        double pageWidth,
        double pageHeight,
        double innerWidth,
        double innerHeight,
        LayoutInsetsSnapshot margin) {
}
