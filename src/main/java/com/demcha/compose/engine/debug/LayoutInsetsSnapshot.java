package com.demcha.compose.engine.debug;

import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;

/**
 * Snapshot-friendly inset model used for margins and padding.
 *
 * <p>The values are normalized through
 * {@link LayoutSnapshotExtractor#normalize(double)} so JSON baselines remain
 * stable across insignificant floating-point noise.</p>
 *
 * @param top top inset
 * @param right right inset
 * @param bottom bottom inset
 * @param left left inset
 */
public record LayoutInsetsSnapshot(double top, double right, double bottom, double left) {

    /**
     * Converts a margin component into a normalized snapshot representation.
     *
     * @param margin source margin, or {@code null} to treat as zero insets
     * @return normalized snapshot-friendly inset values
     */
    public static LayoutInsetsSnapshot from(Margin margin) {
        Margin safeMargin = margin == null ? Margin.zero() : margin;
        return new LayoutInsetsSnapshot(
                LayoutSnapshotExtractor.normalize(safeMargin.top()),
                LayoutSnapshotExtractor.normalize(safeMargin.right()),
                LayoutSnapshotExtractor.normalize(safeMargin.bottom()),
                LayoutSnapshotExtractor.normalize(safeMargin.left()));
    }

    /**
     * Converts a padding component into a normalized snapshot representation.
     *
     * @param padding source padding, or {@code null} to treat as zero insets
     * @return normalized snapshot-friendly inset values
     */
    public static LayoutInsetsSnapshot from(Padding padding) {
        Padding safePadding = padding == null ? Padding.zero() : padding;
        return new LayoutInsetsSnapshot(
                LayoutSnapshotExtractor.normalize(safePadding.top()),
                LayoutSnapshotExtractor.normalize(safePadding.right()),
                LayoutSnapshotExtractor.normalize(safePadding.bottom()),
                LayoutSnapshotExtractor.normalize(safePadding.left()));
    }
}
