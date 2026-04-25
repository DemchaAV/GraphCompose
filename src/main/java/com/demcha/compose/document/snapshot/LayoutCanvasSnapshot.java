package com.demcha.compose.document.snapshot;

/**
 * Snapshot-friendly projection of the resolved document canvas.
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
