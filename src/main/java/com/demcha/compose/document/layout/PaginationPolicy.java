package com.demcha.compose.document.layout;

/**
 * Pagination contract for a semantic node.
 *
 * <p>The page-breaker treats {@link #ATOMIC} and {@link #SHAPE_ATOMIC} the same
 * way — both keep the entire node together on a single page. They are kept
 * separate so downstream consumers (snapshots, render handlers) can tell the
 * difference between "atomic because its layers share a bounding box" and
 * "atomic because the outline path clips the layers, so a split would tear
 * the geometry."</p>
 */
public enum PaginationPolicy {
    /**
     * Node must fit on a single page as one unit.
     */
    ATOMIC,
    /**
     * Node may be split by its definition across pages.
     */
    SPLITTABLE,
    /**
     * Node behaves atomically because its layers are clipped to a shape
     * outline. The outline plus every layer stays on the same page; if the
     * outer height exceeds page capacity, the page-breaker raises an
     * {@code AtomicNodeTooLargeException} with the offending path so the
     * caller can either reduce the outline or set
     * {@link com.demcha.compose.document.style.ClipPolicy#OVERFLOW_VISIBLE}
     * and accept overflow.
     */
    SHAPE_ATOMIC
}
