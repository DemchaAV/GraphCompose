package com.demcha.compose.document.node;

/**
 * Cross-axis (vertical) placement of a row's children within the row band, whose
 * height is the tallest child. The {@code align-items} analogue for a horizontal
 * row — line up shorter children to the top, middle, or bottom without manual
 * coordinates.
 *
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
public enum RowVerticalAlign {
    /** Children sit flush with the top of the row band (the default). */
    TOP,
    /** Children are centred vertically within the row band. */
    CENTER,
    /** Children sit flush with the bottom of the row band. */
    BOTTOM
}
