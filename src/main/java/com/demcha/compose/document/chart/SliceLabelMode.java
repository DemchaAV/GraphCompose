package com.demcha.compose.document.chart;

/**
 * What a pie/donut slice label shows. Labels render outside the slice at its
 * mid-angle, above the slice geometry, behind the configurable value-label halo.
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public enum SliceLabelMode {
    /** No slice labels. */
    NONE,
    /** The raw value, formatted with the spec's value format. */
    VALUE,
    /** The slice's share of the total, formatted with the spec's percent format. */
    PERCENT,
    /** The category name. */
    CATEGORY,
    /** Category name followed by the percent share. */
    CATEGORY_PERCENT
}
