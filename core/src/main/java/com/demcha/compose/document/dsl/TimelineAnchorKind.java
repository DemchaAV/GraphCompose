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

    /** The marker in an entry's axis column — where the rail passes, horizontally. */
    MARKER,

    /**
     * A whole entry — where the rail starts and stops, vertically.
     *
     * <p>Separate from the marker because they answer different questions, and because an
     * entry is the only one of the two that can span pages: its slices carry each page's
     * content band, which is what a rail crossing a page break needs.</p>
     */
    ENTRY
}
