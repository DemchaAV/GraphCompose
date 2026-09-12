package com.demcha.compose.document.dsl;

import com.demcha.compose.document.layout.LayoutDepth;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.NodeDefinitionSupport;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.ResolvedLayoutAddition;
import com.demcha.compose.document.layout.ResolvedLayoutAnchor;
import com.demcha.compose.document.layout.ResolvedLayoutMetadata;
import com.demcha.compose.document.layout.ResolvedLayoutPass;
import com.demcha.compose.document.layout.payloads.ShapeFragmentPayload;
import com.demcha.compose.document.layout.payloads.SideBorders;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One timeline's identity, its rail's configuration, and the pass that draws that rail.
 *
 * <p>Every marker and every entry in a timeline anchors on the same instance of this, so
 * the pass asks for <em>its</em> anchors and gets that timeline's and nobody else's. Two
 * timelines on one page are two owners and never merge.</p>
 *
 * <p>Being the pass as well as the identity is what removes the registration step. A
 * feature declares an owner on the semantic tree; if that owner is also a
 * {@link ResolvedLayoutPass}, the document has said it has something to draw once the
 * layout is settled. Nothing calls a register method, no session is passed into the DSL,
 * and the driver that runs it knows nothing about timelines — it asks whether an owner is
 * a pass, not what kind of thing it is.</p>
 *
 * <p>Deliberately a plain final class and not a record. The seam compares these with
 * {@code ==}, and a record invites the reader to think in value equality: two timelines
 * configured identically are still two timelines.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
final class TimelineRailOwner implements ResolvedLayoutPass {

    private static final double EPS = 1e-9;

    private final TimelineRailSpec rail;
    private final TimelineRailEnd start;
    private final TimelineRailEnd end;
    private final TimelineMarkerAnchor markerAnchor;

    TimelineRailOwner(TimelineRailSpec rail, TimelineRailEnd start, TimelineRailEnd end,
                      TimelineMarkerAnchor markerAnchor) {
        this.rail = rail;
        this.start = start;
        this.end = end;
        this.markerAnchor = markerAnchor;
    }

    /**
     * The rail this timeline asked for.
     *
     * @return the rail spec
     */
    TimelineRailSpec rail() {
        return rail;
    }

    @Override
    public String id() {
        return "timeline-rail";
    }

    @Override
    public List<ResolvedLayoutAddition> contribute(LayoutGraph graph, ResolvedLayoutMetadata metadata) {
        List<ResolvedLayoutAnchor> markers = metadata.anchors(this, TimelineAnchorKind.MARKER);
        if (markers.isEmpty()) {
            // Not this timeline's document. No inspection, no feature flag: the anchors
            // are simply not there.
            return List.of();
        }

        // X from the marker, y from the extent — two independent questions. Every marker
        // in a timeline resolves to the same x, so the first one answers for all of them;
        // a marker of a different size moves its own centre but not its left edge, which
        // is why the legacy anchor is expressed as an edge plus a constant.
        double railX = markerAnchor.x(markers.get(0));
        List<Segment> segments = segments(metadata, markers);

        List<ResolvedLayoutAddition> additions = new ArrayList<>(segments.size());
        SideBorders leftOnly = new SideBorders(null, null, null,
                NodeDefinitionSupport.toStroke(rail.stroke()));
        for (Segment segment : segments) {
            double height = segment.top - segment.bottom;
            if (height <= EPS) {
                // A rail of no length is not a shorter rail. One entry with
                // MARKER_TO_MARKER lands here, and nothing reaches a backend.
                continue;
            }
            additions.add(new ResolvedLayoutAddition(LayoutDepth.UNDER_BODY,
                    PlacedFragment.withZeroInsets("@timeline-rail", additions.size(),
                            segment.page, railX, segment.bottom,
                            rail.stroke().width(), height,
                            new ShapeFragmentPayload(null, null, null, null, null, leftOnly, null))));
        }
        return additions;
    }

    /**
     * The rail's vertical extent, one segment per page it appears on.
     *
     * <p>Every segment starts as the entries' own resolved slices, because those already
     * carry the one thing neither the markers nor the node boxes do: what a page's content
     * band is, on that page, after per-page margins. An end asked for on the marker then
     * trims that page's slice back to the marker rather than deriving a band of its own —
     * and only that page's, which is what lets the two ends be chosen one at a time. A
     * middle page is trimmed by neither, because neither the first marker nor the last is
     * on it.</p>
     *
     * <p>Trimming <em>back</em> is the whole of it: {@code min} at the top and {@code max}
     * at the bottom, so an end on a marker can only shorten the line, never invent rail
     * outside the entries. A page past the marker that bounds it drops out for the same
     * reason — with the start on the first marker there is nothing to draw on a page before
     * it.</p>
     */
    private List<Segment> segments(ResolvedLayoutMetadata metadata, List<ResolvedLayoutAnchor> markers) {
        Map<Integer, Segment> byPage = new LinkedHashMap<>();
        for (ResolvedLayoutAnchor entry : metadata.anchors(this, TimelineAnchorKind.ENTRY)) {
            byPage.merge(entry.pageIndex(),
                    new Segment(entry.pageIndex(), entry.pointY(1.0), entry.y()),
                    Segment::union);
        }
        boolean startsOnMarker = start == TimelineRailEnd.MARKER;
        boolean endsOnMarker = end == TimelineRailEnd.MARKER;
        if (!startsOnMarker && !endsOnMarker) {
            return List.copyOf(byPage.values());
        }

        ResolvedLayoutAnchor first = markers.get(0);
        ResolvedLayoutAnchor last = markers.get(markers.size() - 1);
        double startY = markerAnchor.y(first);
        double endY = markerAnchor.y(last);
        List<Segment> trimmed = new ArrayList<>();
        for (Segment segment : byPage.values()) {
            if (startsOnMarker && segment.page < first.pageIndex()) {
                continue;
            }
            if (endsOnMarker && segment.page > last.pageIndex()) {
                continue;
            }
            double top = startsOnMarker && segment.page == first.pageIndex()
                    ? Math.min(segment.top, startY)
                    : segment.top;
            double bottom = endsOnMarker && segment.page == last.pageIndex()
                    ? Math.max(segment.bottom, endY)
                    : segment.bottom;
            trimmed.add(new Segment(segment.page, top, bottom));
        }
        return trimmed;
    }

    /** One page's worth of rail, in page coordinates. */
    private record Segment(int page, double top, double bottom) {

        Segment union(Segment other) {
            return new Segment(page, Math.max(top, other.top), Math.min(bottom, other.bottom));
        }
    }
}
