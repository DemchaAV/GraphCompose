package com.demcha.compose.document.style;

/**
 * A physical page edge, used to declare on which sides a node bleeds past the
 * page content margin to the trimmed page edge.
 *
 * @author Artem Demchyshyn
 * @see DocumentBleed
 * @since 1.9.0
 */
public enum DocumentEdge {
    /** The top page edge. */
    TOP,
    /** The right page edge. */
    RIGHT,
    /** The bottom page edge. */
    BOTTOM,
    /** The left page edge. */
    LEFT
}
