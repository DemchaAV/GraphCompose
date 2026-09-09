package com.demcha.compose.document.dsl;

import com.demcha.compose.document.layout.ResolvedLayoutAnchor;

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
