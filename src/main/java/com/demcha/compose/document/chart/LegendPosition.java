package com.demcha.compose.document.chart;

/**
 * Placement of the series legend relative to the plot area.
 *
 * <p>v1 of the geometry resolver supports {@link #NONE} and {@link #BOTTOM}; the
 * remaining placements are reserved and rejected by validation until the
 * corresponding layout lands. Adding resolver support for a reserved constant is
 * an additive, non-breaking change.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public enum LegendPosition {
    /** No legend is rendered. */
    NONE,
    /** Single legend row below the plot area. */
    BOTTOM,
    /** Reserved — legend column to the right of the plot area. */
    RIGHT,
    /** Reserved — single legend row above the plot area. */
    TOP
}
