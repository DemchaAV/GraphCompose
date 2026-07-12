package com.demcha.compose.document.chart;

/**
 * Placement of the series legend relative to the plot area.
 *
 * <p>All four placements are laid out by the geometry resolver for every chart
 * kind, including pie: {@link #NONE}, {@link #BOTTOM} (a row below the plot),
 * {@link #TOP} (a row above), and {@link #RIGHT} (a column beside it). The
 * resolver reserves the corresponding gutter and packs the entries to fit.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public enum LegendPosition {
    /**
     * No legend is rendered.
     */
    NONE,
    /**
     * Single legend row below the plot area.
     */
    BOTTOM,
    /**
     * Legend column to the right of the plot area.
     */
    RIGHT,
    /**
     * Single legend row above the plot area.
     */
    TOP
}
