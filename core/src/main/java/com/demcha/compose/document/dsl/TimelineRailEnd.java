package com.demcha.compose.document.dsl;

/**
 * Where one end of a timeline's rail sits.
 *
 * <p>The two ends are chosen separately, because a design that wants them the same is only
 * one of four and was never the interesting case. A rail can begin at the first marker and
 * still run to the foot of the last entry; that is a real design — a sidebar whose line
 * starts at the first dot and carries on past the last one to close the block — and with a
 * single value naming both ends it could not be asked for. The previous model had two
 * symmetric constants and no way to mix them, so a template wanting this one painted the
 * top of an entry-bounds rail over with the page colour and redrew the rest. Measured, that
 * did not even work: an accent draws above the mask.</p>
 *
 * <p>Nothing here says where the rail sits horizontally. That comes from the marker anchor,
 * and the two are independent on purpose: an end moves the line's start or finish and never
 * moves it sideways.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
public enum TimelineRailEnd {

    /**
     * The entries' own bound on that page — the top of the first entry at the start, the
     * foot of the last at the end.
     *
     * <p>What a timeline written before there was a choice draws at both ends, to the
     * point: measured against the per-entry border it replaces, the two agree to 0.000000
     * on every page. The gaps between entries are inside it, because an entry's spacing is
     * padding within its own box, and there is no tail after the last entry because the
     * last entry has no such padding.</p>
     */
    ENTRY_BOUND,

    /**
     * The marker's anchor point — the first marker's at the start, the last's at the end.
     *
     * <p>No rail above the first marker or below the last. With one entry and both ends on
     * the marker the extent is zero and no rail is drawn at all, because a zero-length line
     * is not a shorter line.</p>
     */
    MARKER
}
