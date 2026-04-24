package com.demcha.compose.document.layout;

/**
 * Pagination contract for a semantic node.
 */
public enum PaginationPolicy {
    /**
     * Node must fit on a single page as one unit.
     */
    ATOMIC,
    /**
     * Node may be split by its definition across pages.
     */
    SPLITTABLE
}


