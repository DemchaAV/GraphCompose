package com.demcha.compose.document.style;

/**
 * Leader style filling the gap between a table-of-contents entry's label and its
 * page number.
 *
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
public enum DocumentLeader {
    /** No leader — the label and page number are separated by empty space. */
    NONE,
    /** A row of dots (the classic table-of-contents leader). */
    DOTS,
    /** A dashed line. */
    DASHES
}
