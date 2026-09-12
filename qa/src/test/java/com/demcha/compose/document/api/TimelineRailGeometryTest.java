package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.dsl.TimelineBuilder;
import com.demcha.compose.document.dsl.TimelineMarker;
import com.demcha.compose.document.dsl.TimelineRailEnd;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.ResolvedLayoutAnchor;
import com.demcha.compose.document.layout.ResolvedLayoutMetadata;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

/**
 * The rail as one logical line, computed from resolved anchors.
 *
 * <p>It used to be a left border repeated on every entry section, which is why it sat at
 * the entry's edge, could not stop at the markers, and drew a slightly darker row wherever
 * two entries abutted. It is now contributed by a pass reading the anchors the layout
 * resolved: <b>x</b> from the marker anchor, <b>the two ends</b> from the extent, and one
 * fragment per page it appears on.</p>
 *
 * <p>The two are independent, and the tests keep them so. A marker anchor moves the line
 * sideways and never changes where it starts; an extent changes where it starts and never
 * moves it sideways.</p>
 */
class TimelineRailGeometryTest {

    private static final DocumentColor RAIL = DocumentColor.rgb(150, 158, 172);
    private static final DocumentColor INK = DocumentColor.rgb(20, 40, 70);

    /** The margin {@link #timeline(double, double, Consumer)} sets, so the content band is known. */
    private static final double PAGE_MARGIN = 20.0;

    // --- extent ---------------------------------------------------------------

    @Test
    void entryBoundsIsExactlyTheUnionOfTheEntriesResolvedSlices() throws Exception {
        // The compatibility contract, stated as an equation rather than as a picture. The
        // slices are the same boxes the per-entry border used to be drawn on — measured at
        // Δ = 0 against it — so a rail equal to their union is the rail that shipped.
        LayoutGraph graph = timeline(320, 300, t -> t
                .spacing(14)
                .entry(TimelineMarker.dot(8, INK), e -> e.title("First").body("Body one."))
                .entry(TimelineMarker.dot(8, INK), e -> e.title("Second").body("Body two."))
                .entry(TimelineMarker.dot(8, INK), e -> e.title("Third").body("Body three.")));

        List<PlacedFragment> rails = rails(graph);
        List<ResolvedLayoutAnchor> entries = entryAnchors(graph);
        assertThat(rails).as("one page, one rail").hasSize(1);
        assertThat(entries).hasSize(3);

        double top = entries.stream().mapToDouble(a -> a.pointY(1.0)).max().orElseThrow();
        double bottom = entries.stream().mapToDouble(ResolvedLayoutAnchor::y).min().orElseThrow();
        assertThat(rails.get(0).y() + rails.get(0).height()).isEqualTo(top, within(1e-9));
        assertThat(rails.get(0).y()).isEqualTo(bottom, within(1e-9));

        // And the union is contiguous, which is why one line can replace three: an entry's
        // spacing is padding inside its own box, so the gaps are covered and there is no
        // tail after the last entry.
        assertThat(entries.get(0).y()).isEqualTo(entries.get(1).pointY(1.0), within(1e-9));
        assertThat(entries.get(1).y()).isEqualTo(entries.get(2).pointY(1.0), within(1e-9));
    }

    @Test
    void markerToMarkerStopsAtTheMarkersAndEntryBoundsDoesNot() throws Exception {
        Consumer<TimelineBuilder> entries = t -> t
                .spacing(14)
                .entry(TimelineMarker.dot(8, INK), e -> e.title("First").body("Body one."))
                .entry(TimelineMarker.dot(8, INK), e -> e.title("Second").body("Body two."));

        PlacedFragment bounded = rails(timeline(320, 300, entries)).get(0);
        PlacedFragment betweenMarkers = rails(timeline(320, 300, t -> {
            t.rail(rail -> rail.from(TimelineRailEnd.MARKER).to(TimelineRailEnd.MARKER));
            entries.accept(t);
        })).get(0);

        assertThat(betweenMarkers.y() + betweenMarkers.height())
                .as("nothing above the first marker")
                .isLessThan(bounded.y() + bounded.height());
        assertThat(betweenMarkers.y())
                .as("nothing below the last")
                .isGreaterThan(bounded.y());
        assertThat(betweenMarkers.x())
                .as("and the extent moved neither end sideways")
                .isEqualTo(bounded.x(), within(1e-9));
    }

    @Test
    void markerToMarkerRunsBetweenTheAnchorPointsItIsNamedFor() throws Exception {
        LayoutGraph graph = timeline(320, 300, t -> t
                .rail(rail -> rail.from(TimelineRailEnd.MARKER).to(TimelineRailEnd.MARKER))
                .entry(TimelineMarker.dot(8, INK), e -> e.title("First").body("Body one."))
                .entry(TimelineMarker.dot(8, INK), e -> e.title("Second").body("Body two.")));

        List<ResolvedLayoutAnchor> markers = markerAnchors(graph);
        PlacedFragment rail = rails(graph).get(0);
        assertThat(rail.y() + rail.height())
                .as("the first marker's anchor point, vertically its centre")
                .isEqualTo(markers.get(0).pointY(0.5), within(1e-9));
        assertThat(rail.y()).isEqualTo(markers.get(1).pointY(0.5), within(1e-9));
    }

    @Test
    void oneEntryWithMarkerToMarkerEmitsNoRailAtAll() throws Exception {
        // A rail of no length is not a shorter rail: nothing reaches a backend, rather
        // than a zero- or negative-height fragment for one to cope with.
        LayoutGraph graph = timeline(320, 300, t -> t
                .rail(rail -> rail.from(TimelineRailEnd.MARKER).to(TimelineRailEnd.MARKER))
                .entry(TimelineMarker.dot(8, INK), e -> e.title("Only").body("Body.")));

        assertThat(rails(graph)).isEmpty();
        assertThat(markerAnchors(graph)).as("the marker itself still renders").hasSize(1);
    }

    @Test
    void oneEntryWithEntryBoundsStillDrawsItsRail() throws Exception {
        // The other extent has no such degenerate case — an entry has height.
        assertThat(rails(timeline(320, 300, t -> t
                .entry(TimelineMarker.dot(8, INK), e -> e.title("Only").body("Body.")))))
                .hasSize(1);
    }

    // The timeline-bounds case went with the constant that named it. It was defined and not
    // implemented, threw when asked for, and was indistinguishable from the entries' bound on
    // one page — so with the ends chosen separately there is nothing for it to mean, and a
    // throwing constant is not API worth releasing.

    @Test
    void theFourEndCombinationsAreFourDifferentLines() throws Exception {
        // The whole point of choosing the ends separately: the two mixed lines are the ones
        // that could not be asked for before, and each shares exactly one end with each of
        // the symmetric pair.
        Consumer<TimelineBuilder> entries = t -> t
                .spacing(14)
                .entry(TimelineMarker.dot(8, INK), e -> e.title("First").body("Body one."))
                .entry(TimelineMarker.dot(8, INK), e -> e.title("Second").body("Body two."));

        PlacedFragment boundToBound = railOf(entries, TimelineRailEnd.ENTRY_BOUND,
                TimelineRailEnd.ENTRY_BOUND);
        PlacedFragment markerToMarker = railOf(entries, TimelineRailEnd.MARKER,
                TimelineRailEnd.MARKER);
        PlacedFragment markerToBound = railOf(entries, TimelineRailEnd.MARKER,
                TimelineRailEnd.ENTRY_BOUND);
        PlacedFragment boundToMarker = railOf(entries, TimelineRailEnd.ENTRY_BOUND,
                TimelineRailEnd.MARKER);

        // Each end is whichever of the two it was asked for, and nothing else changes it.
        assertThat(top(markerToBound))
                .as("marker -> bound starts where marker -> marker starts")
                .isEqualTo(top(markerToMarker), within(1e-9));
        assertThat(markerToBound.y())
                .as("and ends where bound -> bound ends")
                .isEqualTo(boundToBound.y(), within(1e-9));
        assertThat(top(boundToMarker))
                .as("bound -> marker starts where bound -> bound starts")
                .isEqualTo(top(boundToBound), within(1e-9));
        assertThat(boundToMarker.y())
                .as("and ends where marker -> marker ends")
                .isEqualTo(markerToMarker.y(), within(1e-9));

        // Four distinct lines, not two dressed up: the mixed pair is longer than
        // marker-to-marker and shorter than bound-to-bound at one end each.
        assertThat(markerToBound.height()).isGreaterThan(markerToMarker.height());
        assertThat(markerToBound.height()).isLessThan(boundToBound.height());
        assertThat(boundToMarker.height()).isGreaterThan(markerToMarker.height());
        assertThat(boundToMarker.height()).isLessThan(boundToBound.height());

        // And no end moved the line sideways.
        assertThat(List.of(markerToMarker.x(), markerToBound.x(), boundToMarker.x()))
                .allSatisfy(x -> assertThat(x).isEqualTo(boundToBound.x(), within(1e-9)));
    }

    @Test
    void eachEndTrimsOnlyItsOwnPageWhenTheTimelineCrossesOne() throws Exception {
        // Pagination, per end. The start trims the page the first marker is on and the end
        // trims the page the last is on; a page holding neither is bounded by its entries at
        // both ends whatever was asked for. marker -> bound is the case ProfessionalSidebar
        // wants: begin at the first dot, run to the foot of the last entry.
        Consumer<TimelineBuilder> entries = t -> {
            t.spacing(10).keepEntriesTogether();
            for (int i = 0; i < 7; i++) {
                String title = "Entry " + i;
                t.entry(TimelineMarker.dot(8, INK), e -> e
                        .title(title).body("One body line of this entry."));
            }
        };

        LayoutGraph boundToBound = scene(220, entries, TimelineRailEnd.ENTRY_BOUND,
                TimelineRailEnd.ENTRY_BOUND);
        LayoutGraph markerToBound = scene(220, entries, TimelineRailEnd.MARKER,
                TimelineRailEnd.ENTRY_BOUND);
        assertThat(boundToBound.totalPages()).isEqualTo(2);
        assertThat(markerToBound.totalPages()).isEqualTo(2);

        PlacedFragment firstOfBoth = railOnPage(boundToBound, 0);
        PlacedFragment firstOfMixed = railOnPage(markerToBound, 0);
        PlacedFragment lastOfBoth = railOnPage(boundToBound, 1);
        PlacedFragment lastOfMixed = railOnPage(markerToBound, 1);

        assertThat(top(firstOfMixed))
                .as("page 0 is trimmed to the first marker, because the start asked for it")
                .isLessThan(top(firstOfBoth));
        assertThat(firstOfMixed.y())
                .as("but its foot is the page's own entries, not an end")
                .isEqualTo(firstOfBoth.y(), within(1e-9));
        assertThat(top(lastOfMixed))
                .as("page 1 is not trimmed at the top: the first marker is not on it")
                .isEqualTo(top(lastOfBoth), within(1e-9));
        assertThat(lastOfMixed.y())
                .as("nor at the foot, because the end asked for the entries' bound")
                .isEqualTo(lastOfBoth.y(), within(1e-9));
    }

    // --- the marker anchor, which is a different question ----------------------

    @Test
    void theRailSitsOneGutterLeftOfTheMarkerWhateverTheMarkerSize() throws Exception {
        // The measured reason the anchor is an edge plus a constant rather than a centre:
        // a centre moves with the marker, an edge does not. Two timelines, markers of 6pt
        // and 24pt, and one rail x.
        double small = rails(timeline(320, 300, t -> t.gutter(8)
                .entry(TimelineMarker.dot(6, INK), e -> e.title("x")))).get(0).x();
        double large = rails(timeline(320, 300, t -> t.gutter(8)
                .entry(TimelineMarker.dot(24, INK), e -> e.title("x")))).get(0).x();

        assertThat(large).isEqualTo(small, within(1e-9));
    }

    @Test
    void markerOnRailPlacesEveryMarkerAnchorOnTheRail() throws Exception {
        // The contract in its own terms, read off resolved geometry rather than off the
        // spec that asked for it: for each entry, railX is that marker's own centre.
        LayoutGraph graph = timeline(360, 320, t -> t
                .markerOnRail()
                .axisWidth(28)
                .entry(TimelineMarker.dot(12, INK), e -> e.title("Dot").body("Body."))
                .entry(TimelineMarker.square(12, INK), e -> e.title("Square").body("Body."))
                .entry(TimelineMarker.numbered(3, 12, INK, DocumentColor.WHITE),
                        e -> e.title("Numbered").body("Body.")));

        double railX = rails(graph).get(0).x();
        List<ResolvedLayoutAnchor> markers = markerAnchors(graph);
        assertThat(markers).hasSize(3);
        assertThat(markers).allSatisfy(marker -> assertThat(railX)
                .as("relativeX 0.5, offsetX 0 — the marker's own centre")
                .isEqualTo(marker.x() + marker.width() / 2, within(1e-9)));
    }

    @Test
    void markerOnRailPutsMarkersOfEverySizeOnTheSameLine() throws Exception {
        // The case that found the defect. Markers were left-packed in the axis column, so
        // a 6pt and a 24pt marker had centres 9pt apart and only the first sat on the rail.
        // Placement now follows the anchor — a centre anchor centres the marker in its
        // column — so every centre is the column's centre, whatever the marker's size.
        for (Consumer<TimelineBuilder> axis : List.<Consumer<TimelineBuilder>>of(
                t -> t.axisWidth(28), t -> t.markerColumnWeight(0.18))) {
            LayoutGraph graph = timeline(360, 340, t -> {
                t.markerOnRail();
                axis.accept(t);
                t.entry(TimelineMarker.dot(6, INK), e -> e.title("Small").body("Body."))
                        .entry(TimelineMarker.numbered(2, 14, INK, DocumentColor.WHITE),
                                e -> e.title("Medium").body("Body."))
                        .entry(TimelineMarker.square(24, INK), e -> e.title("Large").body("Body."));
            });

            double railX = rails(graph).get(0).x();
            List<ResolvedLayoutAnchor> markers = markerAnchors(graph);
            assertThat(markers).hasSize(3);
            assertThat(markers.stream().map(ResolvedLayoutAnchor::width))
                    .as("the premise: three different sizes")
                    .containsExactly(6.0, 14.0, 24.0);
            assertThat(markers).allSatisfy(marker -> assertThat(railX)
                    .as("every marker's centre is the axis, fixed axis or weighted")
                    .isEqualTo(marker.x() + marker.width() / 2, within(1e-9)));
        }
    }

    @Test
    void theMarkerOnRailScenarioTheVisualBaselineDraws() throws Exception {
        // The geometry behind timeline-dsl/marker-on-rail: DATE | ● | CONTENT with markers
        // of three sizes. The baseline shows it; this says what it is.
        LayoutGraph graph = timeline(360, 210, t -> t
                .spacing(12)
                .markerOnRail()
                .axisWidth(28)
                .leadingColumn(DocumentRowColumn.fixed(54))
                .entry(e -> e.marker(TimelineMarker.dot(6, INK))
                        .leading(d -> d.addParagraph("2023")).title("Senior").body("Body."))
                .entry(e -> e.marker(TimelineMarker.numbered(2, 14, INK, DocumentColor.WHITE))
                        .leading(d -> d.addParagraph("2021")).title("Engineer").body("Body."))
                .entry(e -> e.marker(TimelineMarker.square(24, INK))
                        .leading(d -> d.addParagraph("2019")).title("Junior").body("Body.")));

        double railX = rails(graph).get(0).x();
        List<ResolvedLayoutAnchor> markers = markerAnchors(graph);
        assertThat(markers.stream().map(ResolvedLayoutAnchor::width))
                .as("three genuinely different markers")
                .containsExactly(6.0, 14.0, 24.0);
        assertThat(markers.stream().map(m -> m.pointX(0.5)).distinct())
                .as("one declared anchor x between them")
                .hasSize(1);
        assertThat(markers).allSatisfy(marker ->
                assertThat(railX).isEqualTo(marker.pointX(0.5), within(1e-9)));
        assertThat(railX)
                .as("and the dates are in their own column, left of the axis")
                .isGreaterThan(entryAnchors(graph).get(0).x() + 40.0);
    }

    @Test
    void theLeftEdgeAnchorStillLeavesMarkersWhereTheyHaveAlwaysBeen() throws Exception {
        // The other arm of the same question, and the one that must not move: markers of
        // different sizes share a left edge, which is why the default rail is an edge plus
        // a constant rather than a centre.
        LayoutGraph graph = timeline(360, 340, t -> t
                .axisWidth(28)
                .entry(TimelineMarker.dot(6, INK), e -> e.title("Small").body("Body."))
                .entry(TimelineMarker.square(24, INK), e -> e.title("Large").body("Body.")));

        List<ResolvedLayoutAnchor> markers = markerAnchors(graph);
        assertThat(markers.get(0).x())
                .as("left-packed, as before")
                .isEqualTo(markers.get(1).x(), within(1e-9));
        assertThat(rails(graph).get(0).x())
                .isEqualTo(markers.get(0).x() - 8.0, within(1e-9));
    }

    @Test
    void markerOnRailHoldsForEveryShapeAndForBothExtents() throws Exception {
        // The extent decides the two ends and must not touch the x. Same three markers,
        // both extents, one answer.
        for (TimelineRailEnd both : List.of(TimelineRailEnd.ENTRY_BOUND,
                TimelineRailEnd.MARKER)) {
            LayoutGraph graph = timeline(360, 320, t -> t
                    .markerOnRail()
                    .rail(rail -> rail.from(both).to(both))
                    .axisWidth(28)
                    .entry(TimelineMarker.dot(12, INK), e -> e.title("Dot").body("Body."))
                    .entry(TimelineMarker.square(12, INK), e -> e.title("Square").body("Body.")));

            double railX = rails(graph).get(0).x();
            assertThat(markerAnchors(graph)).allSatisfy(marker -> assertThat(railX)
                    .as("%s must not move the rail sideways", both)
                    .isEqualTo(marker.x() + marker.width() / 2, within(1e-9)));
        }
    }

    @Test
    void markerOnRailWithALeadingColumnStillPutsTheMarkerOnTheLine() throws Exception {
        LayoutGraph graph = timeline(400, 320, t -> t
                .markerOnRail()
                .axisWidth(28)
                .leadingColumn(DocumentRowColumn.fixed(56))
                .entry(e -> e.marker(TimelineMarker.dot(12, INK))
                        .leading(d -> d.addParagraph("2023")).title("First").body("Body."))
                .entry(e -> e.marker(TimelineMarker.dot(12, INK))
                        .leading(d -> d.addParagraph("2021")).title("Second").body("Body.")));

        double railX = rails(graph).get(0).x();
        assertThat(markerAnchors(graph)).allSatisfy(marker -> assertThat(railX)
                .isEqualTo(marker.x() + marker.width() / 2, within(1e-9)));
        assertThat(railX)
                .as("and the date column sits to the left of it, not pushing it out")
                .isGreaterThan(entryAnchors(graph).get(0).x() + 40.0);
    }

    @Test
    void markerOnRailWithMarkerToMarkerRunsBetweenTheMarkerCentres() throws Exception {
        LayoutGraph graph = timeline(360, 320, t -> t
                .markerOnRail()
                .rail(rail -> rail.from(TimelineRailEnd.MARKER).to(TimelineRailEnd.MARKER))
                .axisWidth(28)
                .entry(TimelineMarker.dot(12, INK), e -> e.title("First").body("Body."))
                .entry(TimelineMarker.dot(12, INK), e -> e.title("Second").body("Body.")));

        List<ResolvedLayoutAnchor> markers = markerAnchors(graph);
        PlacedFragment rail = rails(graph).get(0);
        assertThat(rail.y() + rail.height())
                .isEqualTo(markers.get(0).pointY(0.5), within(1e-9));
        assertThat(rail.y()).isEqualTo(markers.get(1).pointY(0.5), within(1e-9));
    }

    @Test
    void markerOnRailIsDrawnUnderTheMarkersItCrosses() throws Exception {
        // It matters more here than anywhere: the line now passes through the markers, so
        // whether it is over or under them is visible on the page.
        LayoutGraph graph = timeline(360, 320, t -> t
                .markerOnRail()
                .axisWidth(28)
                .entry(TimelineMarker.dot(14, INK), e -> e.title("First").body("Body."))
                .entry(TimelineMarker.dot(14, INK), e -> e.title("Second").body("Body.")));

        int lastRail = -1;
        int firstMarker = Integer.MAX_VALUE;
        for (int i = 0; i < graph.fragments().size(); i++) {
            PlacedFragment fragment = graph.fragments().get(i);
            if ("@timeline-rail".equals(fragment.path())) {
                lastRail = Math.max(lastRail, i);
            } else if (fragment.payload() != null
                    && fragment.payload().getClass().getSimpleName().contains("Ellipse")) {
                firstMarker = Math.min(firstMarker, i);
            }
        }
        assertThat(lastRail).isNotEqualTo(-1).isLessThan(firstMarker);
    }

    @Test
    void markerOnRailPutsTheLineThroughTheMarkerInstead() throws Exception {
        LayoutGraph graph = timeline(320, 300, t -> t
                .markerOnRail()
                .entry(TimelineMarker.dot(12, INK), e -> e.title("First").body("Body."))
                .entry(TimelineMarker.dot(12, INK), e -> e.title("Second").body("Body.")));

        ResolvedLayoutAnchor marker = markerAnchors(graph).get(0);
        assertThat(rails(graph).get(0).x())
                .as("through the marker's centre")
                .isEqualTo(marker.pointX(0.5), within(1e-9));
    }

    @Test
    void aLeadingColumnAddsContentLeftOfTheAxisAndDoesNotMoveTheRail() throws Exception {
        // LEADING | AXIS | CONTENT. The rail belongs to the axis, so a date column appears
        // to the left of it rather than pushing it out to the entry's boundary.
        LayoutGraph graph = timeline(360, 300, t -> t
                .leadingColumn(DocumentRowColumn.fixed(56))
                .entry(e -> e.marker(TimelineMarker.dot(8, INK))
                        .leading(d -> d.addParagraph("2023")).title("First").body("Body."))
                .entry(e -> e.marker(TimelineMarker.dot(8, INK))
                        .leading(d -> d.addParagraph("2021")).title("Second").body("Body.")));

        PlacedFragment rail = rails(graph).get(0);
        ResolvedLayoutAnchor marker = markerAnchors(graph).get(0);
        ResolvedLayoutAnchor entry = entryAnchors(graph).get(0);

        assertThat(rail.x())
                .as("one gutter left of the marker, as always")
                .isEqualTo(marker.x() - 8.0, within(1e-9));
        assertThat(rail.x())
                .as("which is well right of the entry's own edge, because the date is there")
                .isGreaterThan(entry.x() + 40.0);
    }

    // --- pages and draw order --------------------------------------------------

    @Test
    void aTimelineCrossingPagesGivesOneFragmentPerPageBoundedByThatPage() throws Exception {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < 26; i++) {
            body.append("Sentence ").append(i).append(" of a body long enough to run on and on. ");
        }
        LayoutGraph graph = timeline(320, 150, t -> t
                .spacing(14)
                .entry(TimelineMarker.dot(8, INK), e -> e.title("First").body(body.toString()))
                .entry(TimelineMarker.dot(8, INK), e -> e.title("Second").body("Short.")));

        assertThat(graph.totalPages()).isGreaterThan(2);
        List<PlacedFragment> rails = rails(graph);
        assertThat(rails).as("one logical rail, one fragment per page").hasSize(graph.totalPages());

        for (PlacedFragment rail : rails) {
            List<ResolvedLayoutAnchor> onPage = entryAnchors(graph).stream()
                    .filter(a -> a.pageIndex() == rail.pageIndex()).toList();
            double top = onPage.stream().mapToDouble(a -> a.pointY(1.0)).max().orElseThrow();
            double bottom = onPage.stream().mapToDouble(ResolvedLayoutAnchor::y).min().orElseThrow();
            assertThat(rail.y() + rail.height())
                    .as("page %d top", rail.pageIndex()).isEqualTo(top, within(1e-9));
            assertThat(rail.y())
                    .as("page %d bottom", rail.pageIndex()).isEqualTo(bottom, within(1e-9));
        }
        // Not one physical line pretending to span pages: every fragment is on its own page
        // and none of them reaches beyond it.
        assertThat(rails.stream().map(PlacedFragment::pageIndex).distinct())
                .hasSize(graph.totalPages());
    }

    @Test
    void aRailStopsAtTheLastEntryOnThePageAndNotAtTheBandBelowIt() throws Exception {
        // The case the test above cannot see. Its entry is splittable, so it genuinely
        // occupies the foot of the page and the rail is right to reach it. Here every entry
        // is held whole, and the one that will not fit moves — leaving a gap at the foot of
        // page 0 that nothing occupies. The rail has to stop where the entries stop.
        LayoutGraph graph = timeline(320, 220, t -> {
            t.spacing(10).keepEntriesTogether();
            for (int i = 0; i < 7; i++) {
                String title = "Entry " + i;
                t.entry(TimelineMarker.dot(8, INK), e -> e
                        .title(title).body("One body line of this entry."));
            }
        });

        assertThat(graph.totalPages()).isEqualTo(2);
        List<PlacedFragment> rails = rails(graph);
        assertThat(rails).as("one fragment per page").hasSize(2);

        PlacedFragment first = rails.stream()
                .filter(r -> r.pageIndex() == 0).findFirst().orElseThrow();
        List<ResolvedLayoutAnchor> onFirstPage = entryAnchors(graph).stream()
                .filter(a -> a.pageIndex() == 0).toList();
        double lastEntryFoot = onFirstPage.stream()
                .mapToDouble(ResolvedLayoutAnchor::y).min().orElseThrow();

        assertThat(first.y())
                .as("the rail ends at the last entry the page actually holds")
                .isEqualTo(lastEntryFoot, within(1e-9));

        // The assertion that tells an entry-derived extent from a band-derived one.
        // Comparing the rail against the anchors alone cannot: when an entry that moved
        // leaves a slice behind on the page it skipped, the rail follows it down and still
        // agrees with them. The page's own content band is the independent reference — the
        // margin this helper sets — and a gap has to remain between it and the rail's foot.
        assertThat(first.y())
                .as("and stops short of the band, which is where it used to run to")
                .isGreaterThan(PAGE_MARGIN + 1.0);

        // Nothing of the entry that moved is left on the page it skipped.
        assertThat(onFirstPage).as("only the entries that fit").hasSize(5);
        assertThat(entryAnchors(graph).stream().filter(a -> a.pageIndex() == 1).toList())
                .as("and the rest are on the page they moved to").hasSize(2);

        // The continuation page still opens at its first entry: an intermediate page's rail
        // is bounded by the entries on it at both ends, head as well as foot.
        PlacedFragment second = rails.stream()
                .filter(r -> r.pageIndex() == 1).findFirst().orElseThrow();
        double secondTop = entryAnchors(graph).stream().filter(a -> a.pageIndex() == 1)
                .mapToDouble(a -> a.pointY(1.0)).max().orElseThrow();
        assertThat(second.y() + second.height())
                .as("page 1 starts at its first entry")
                .isEqualTo(secondTop, within(1e-9));
    }

    @Test
    void theRailIsDrawnBeneathTheMarkersItPassesUnder() throws Exception {
        // There is no z in this engine — draw order is list order — so "under" is a
        // statement about the fragment list and has to be asserted there.
        LayoutGraph graph = timeline(320, 300, t -> t
                .entry(TimelineMarker.dot(10, INK), e -> e.title("First"))
                .entry(TimelineMarker.dot(10, INK), e -> e.title("Second")));

        int lastRail = -1;
        int firstMarker = Integer.MAX_VALUE;
        for (int i = 0; i < graph.fragments().size(); i++) {
            PlacedFragment fragment = graph.fragments().get(i);
            if ("@timeline-rail".equals(fragment.path())) {
                lastRail = Math.max(lastRail, i);
            } else if (fragment.payload() != null
                    && fragment.payload().getClass().getSimpleName().contains("Ellipse")) {
                firstMarker = Math.min(firstMarker, i);
            }
        }
        assertThat(lastRail).isNotEqualTo(-1);
        assertThat(lastRail)
                .as("every rail fragment precedes every marker, so a filled marker covers it")
                .isLessThan(firstMarker);
    }

    @Test
    void twoTimelinesOnAPageDrawTwoRails() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(320, 400).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow()
                    .addTimeline(t -> t.connector(RAIL, 1.5)
                            .entry(TimelineMarker.dot(8, INK), e -> e.title("A1").body("Body."))
                            .entry(TimelineMarker.dot(8, INK), e -> e.title("A2").body("Body.")))
                    .addTimeline(t -> t.connector(RAIL, 3.0)
                            .entry(TimelineMarker.dot(8, INK), e -> e.title("B1").body("Body."))
                            .entry(TimelineMarker.dot(8, INK), e -> e.title("B2").body("Body.")))
                    .build();

            List<PlacedFragment> rails = rails(session.layoutGraph());
            assertThat(rails).hasSize(2);
            assertThat(rails.get(0).width()).as("each with its own stroke").isEqualTo(1.5, within(1e-9));
            assertThat(rails.get(1).width()).isEqualTo(3.0, within(1e-9));
            assertThat(rails.get(0).y()).as("and its own extent").isGreaterThan(rails.get(1).y());
        }
    }

    // --- helpers ---------------------------------------------------------------

    private static List<PlacedFragment> rails(LayoutGraph graph) {
        return graph.fragments().stream().filter(f -> "@timeline-rail".equals(f.path())).toList();
    }

    /** A rail's top, which is the edge the start end decides. */
    private static double top(PlacedFragment rail) {
        return rail.y() + rail.height();
    }

    /** The one rail of a single-page scene with those two ends. */
    private static PlacedFragment railOf(Consumer<TimelineBuilder> entries,
                                         TimelineRailEnd start,
                                         TimelineRailEnd end) throws Exception {
        return rails(scene(300, entries, start, end)).get(0);
    }

    private static PlacedFragment railOnPage(LayoutGraph graph, int page) {
        return rails(graph).stream().filter(r -> r.pageIndex() == page)
                .findFirst().orElseThrow(() -> new AssertionError("no rail on page " + page));
    }

    private static LayoutGraph scene(double height,
                                     Consumer<TimelineBuilder> entries,
                                     TimelineRailEnd start,
                                     TimelineRailEnd end) throws Exception {
        return timeline(320, height, t -> {
            t.rail(rail -> rail.from(start).to(end));
            entries.accept(t);
        });
    }

    private static List<ResolvedLayoutAnchor> entryAnchors(LayoutGraph graph) {
        return ResolvedLayoutMetadata.from(graph).anchors().stream()
                .filter(a -> "ENTRY".equals(a.id().kind().toString())).toList();
    }

    private static List<ResolvedLayoutAnchor> markerAnchors(LayoutGraph graph) {
        return ResolvedLayoutMetadata.from(graph).anchors().stream()
                .filter(a -> "MARKER".equals(a.id().kind().toString())).toList();
    }

    private static LayoutGraph timeline(double width, double height,
                                        Consumer<TimelineBuilder> spec) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(width, height).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow().addTimeline(t -> {
                t.connector(RAIL, 1.5);
                spec.accept(t);
            }).build();
            return session.layoutGraph();
        }
    }
}
