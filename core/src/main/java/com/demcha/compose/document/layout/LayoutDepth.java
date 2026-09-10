package com.demcha.compose.document.layout;

/**
 * Where a pass's fragment sits relative to the document body.
 *
 * <p>This engine has no z-index: fragments draw in list order, so depth is expressed by
 * splice position rather than by a number. The driver builds one list and the backends walk
 * it front to back — an under-body addition goes in immediately before the first fragment
 * the contributing feature drew on that page, an over-body addition after the whole body.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
public enum LayoutDepth {

    /**
     * Behind the feature's own body. A rail belongs here: a filled marker should cover the
     * line running under it rather than be crossed by it.
     *
     * <p>Behind <em>its</em> body, not behind the document's. A fragment put at the front of
     * the list would also sit beneath the fill of whatever the feature is inside — a card, a
     * panel, a tinted section — and disappear under it while remaining present in the
     * geometry, which is a defect no coordinate can show.</p>
     */
    UNDER_BODY,

    /**
     * In front of the body, for something that must remain legible over content — a
     * callout leader, a highlight.
     */
    OVER_BODY
}
