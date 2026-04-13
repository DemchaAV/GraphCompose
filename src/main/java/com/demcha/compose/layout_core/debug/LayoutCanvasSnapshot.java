package com.demcha.compose.layout_core.debug;

/**
 * Canvas metadata captured alongside a resolved layout snapshot.
 */
public record LayoutCanvasSnapshot(
        double pageWidth,
        double pageHeight,
        double innerWidth,
        double innerHeight,
        LayoutInsetsSnapshot margin) {
}
