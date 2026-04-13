package com.demcha.compose.layout_core.debug;

import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;

/**
 * Snapshot-friendly box inset model used for margins and padding.
 */
public record LayoutInsetsSnapshot(double top, double right, double bottom, double left) {

    public static LayoutInsetsSnapshot from(Margin margin) {
        Margin safeMargin = margin == null ? Margin.zero() : margin;
        return new LayoutInsetsSnapshot(
                LayoutSnapshotExtractor.normalize(safeMargin.top()),
                LayoutSnapshotExtractor.normalize(safeMargin.right()),
                LayoutSnapshotExtractor.normalize(safeMargin.bottom()),
                LayoutSnapshotExtractor.normalize(safeMargin.left()));
    }

    public static LayoutInsetsSnapshot from(Padding padding) {
        Padding safePadding = padding == null ? Padding.zero() : padding;
        return new LayoutInsetsSnapshot(
                LayoutSnapshotExtractor.normalize(safePadding.top()),
                LayoutSnapshotExtractor.normalize(safePadding.right()),
                LayoutSnapshotExtractor.normalize(safePadding.bottom()),
                LayoutSnapshotExtractor.normalize(safePadding.left()));
    }
}
