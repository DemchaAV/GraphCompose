package com.demcha.compose.document.layout;

/**
 * Where a pass's fragment sits relative to the document body.
 *
 * <p>This engine has no z-index: fragments draw in list order, so depth is expressed by
 * splice position rather than by a number. The driver builds one list —
 * under-body additions, then the compiled body, then over-body additions — and the
 * backends walk it front to back.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
public enum LayoutDepth {

    /**
     * Behind the body. A rail belongs here: a filled marker should cover the line running
     * under it rather than be crossed by it.
     */
    UNDER_BODY,

    /**
     * In front of the body, for something that must remain legible over content — a
     * callout leader, a highlight.
     */
    OVER_BODY
}
