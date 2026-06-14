package com.demcha.compose.document.chart;

/**
 * How multiple series share one category slot in a bar chart.
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public enum BarGrouping {
    /**
     * Series render side by side inside the category slot.
     */
    GROUPED,
    /**
     * Series stack on top of each other; the axis scales to stacked sums.
     * The stacked baseline is always zero (parts summing to a whole), so an
     * explicit {@code valueAxis().min()} of either sign does not move it.
     */
    STACKED
}
