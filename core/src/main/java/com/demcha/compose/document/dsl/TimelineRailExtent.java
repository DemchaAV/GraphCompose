package com.demcha.compose.document.dsl;

/**
 * How far a timeline's rail runs.
 *
 * <p>The two ends of the line, and nothing about where it sits horizontally — that comes
 * from the marker anchor. The two are independent on purpose: a rail can start and stop at
 * the markers while passing through their left edges, or span the entries while passing
 * through their centres.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
public enum TimelineRailExtent {

    /**
     * From the first marker's anchor point to the last's.
     *
     * <p>No rail above the first marker or below the last. With a single entry the extent
     * is zero and no rail is drawn at all — a zero-length line is not a shorter line.</p>
     */
    MARKER_TO_MARKER,

    /**
     * The union of the entries' resolved boxes, page by page.
     *
     * <p>What a timeline written before there was a choice already draws, to the point:
     * measured against the per-entry border it replaces, the two agree to 0.000000 in both
     * ends on every page. The gaps between entries are inside it, because an entry's
     * spacing is padding within its own box; there is no tail after the last entry,
     * because the last entry has no such padding.</p>
     *
     * <p><em>Page by page</em> is load-bearing. An entry that spans pages has a different
     * extent on each of them, and its box as a whole is a coordinate belonging to no
     * page.</p>
     */
    ENTRY_BOUNDS,

    /**
     * The timeline's own box, rather than the union of the entries in it.
     *
     * <p>Defined but <b>not implemented</b>: it is indistinguishable from
     * {@link #ENTRY_BOUNDS} on a single page — a timeline has no padding of its own — and
     * across pages there is nothing to measure it against, because the container draws
     * nothing. Asking for it throws rather than quietly resolving to the neighbour it
     * happens to equal in the easy case.</p>
     */
    TIMELINE_BOUNDS
}
