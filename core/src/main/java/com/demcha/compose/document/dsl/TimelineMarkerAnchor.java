package com.demcha.compose.document.dsl;

import com.demcha.compose.document.layout.ResolvedLayoutAnchor;
import com.demcha.compose.document.node.HorizontalAlign;

/**
 * Which point of a marker the rail passes through.
 *
 * <p>A fraction of the marker's own box plus an offset in points, and that shape is what
 * lets one equation serve both the timeline written years ago and the one written to sit
 * on its rail. Measured before it was chosen: today's rail sits at the entry's left edge
 * while a marker's centre is {@code margin + gutter + size/2}, so the distance between
 * them <em>grows with the marker</em> — 12pt at size 8, 18pt at size 20. Expressed as
 * {@code relativeX = 0.0, offsetX = -gutter} it is one constant at every size; expressed
 * as a centre it would need a different number for each.</p>
 *
 * <p>So there is no legacy branch anywhere in the layout. There are two anchors:</p>
 * <ul>
 *   <li>{@link #atLeftEdge(double)} — {@code (0.0, 0.5)} offset {@code (-gutter, 0)}, what
 *       a timeline that predates the choice already renders;</li>
 *   <li>{@link #onTheRail()} — {@code (0.5, 0.5)} offset {@code (0, 0)}, the marker
 *       centred on its own rail.</li>
 * </ul>
 *
 * @param relativeX fraction of the marker's width, left to right
 * @param relativeY fraction of the marker's height, bottom to top
 * @param offsetX   points added after the fraction, positive to the right
 * @param offsetY   points added after the fraction, positive upwards
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
record TimelineMarkerAnchor(double relativeX, double relativeY, double offsetX, double offsetY) {

    /** The marker's left edge, pulled back by the gutter — where the rail has always been. */
    static TimelineMarkerAnchor atLeftEdge(double gutter) {
        return new TimelineMarkerAnchor(0.0, 0.5, -gutter, 0.0);
    }

    /** The marker's centre, which is what "the marker sits on the rail" means. */
    static TimelineMarkerAnchor onTheRail() {
        return new TimelineMarkerAnchor(0.5, 0.5, 0.0, 0.0);
    }

    /**
     * Where the marker has to sit in its axis column for its anchor to land on the axis.
     *
     * <p>Placement follows from the anchor rather than from a mode: an anchor on the
     * marker's left edge wants the marker at the column's left edge, one on its centre
     * wants it at the column's centre. That is what puts markers of 6, 14 and 24pt on one
     * line — each centred in the same column, so each centre is the column's centre — and
     * it holds for a weighted axis, whose width nobody knows until layout.</p>
     *
     * <p>The mapping is exhaustive over the fractions that exist, and refuses the ones that
     * do not. Placing a {@code relativeX} of, say, 0.25 would need the marker inset by a
     * quarter of the leftover width, which is a fractional alignment the engine does not
     * have; centring it instead would put its anchor somewhere other than the axis and
     * report a resolved anchor that quietly disagrees with the rail. There is no public API
     * that can produce such a fraction today, so this throws rather than invent a
     * behaviour — the day fractional placement exists as a general capability, this is the
     * one place that learns about it.</p>
     *
     * @return the alignment for the marker inside its axis column
     * @throws IllegalStateException if the anchor's {@code relativeX} has no placement
     */
    HorizontalAlign horizontalAlign() {
        if (relativeX == 0.0) {
            return HorizontalAlign.LEFT;
        }
        if (relativeX == 0.5) {
            return HorizontalAlign.CENTER;
        }
        throw new IllegalStateException(
                "A timeline marker anchored at relativeX " + relativeX + " cannot be placed: the "
                + "engine aligns a child left, centre or right, and nothing between. Placing it "
                + "anywhere else would put its resolved anchor off the axis the rail is drawn on.");
    }

    /**
     * Where this anchor puts the rail, horizontally, for a resolved marker.
     *
     * @param marker the marker's resolved box
     * @return the x the rail passes through
     */
    double x(ResolvedLayoutAnchor marker) {
        return marker.pointX(relativeX) + offsetX;
    }

    /**
     * Where this anchor puts the rail's end, vertically, for a resolved marker.
     *
     * @param marker the marker's resolved box
     * @return the y the rail starts or stops at
     */
    double y(ResolvedLayoutAnchor marker) {
        return marker.pointY(relativeY) + offsetY;
    }
}
