package com.demcha.compose.document.style;

/**
 * How a PDF reader should lay out pages — the page layout written to the document
 * catalog.
 *
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
public enum DocumentPageLayout {
    /** One page at a time (the default). */
    SINGLE_PAGE,
    /** A single continuously scrolling column. */
    ONE_COLUMN,
    /** Two continuous columns, odd-numbered pages on the left. */
    TWO_COLUMN_LEFT,
    /** Two continuous columns, odd-numbered pages on the right. */
    TWO_COLUMN_RIGHT,
    /** Two pages side by side, odd-numbered pages on the left. */
    TWO_PAGE_LEFT,
    /** Two pages side by side, odd-numbered pages on the right. */
    TWO_PAGE_RIGHT
}
