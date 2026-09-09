package com.demcha.compose.document.dsl;

/**
 * What a timeline anchor marks.
 *
 * <p>An enum constant because the resolved-layout seam compares an anchor's kind by
 * reference — a string would work or fail on interning, which is why it refuses one.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
enum TimelineAnchorKind {

    /** The marker in an entry's axis column. */
    MARKER
}
