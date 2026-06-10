package com.demcha.compose.document.chart;

/**
 * Whether and where per-point value labels render.
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public enum ValueLabelMode {
    /** No value labels. */
    NONE,
    /** Above a vertical bar / above a line point. */
    OUTSIDE,
    /** Reserved — inside the bar body near its end. */
    INSIDE
}
