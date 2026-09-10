package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.dsl.TimelineBuilder;
import com.demcha.compose.document.dsl.TimelineMarker;
import com.demcha.compose.document.dsl.TimelineRailExtent;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.ResolvedLayoutAnchor;
import com.demcha.compose.document.layout.ResolvedLayoutMetadata;
import com.demcha.compose.document.node.EllipseNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * The invariants the finished visual model rests on, and the geometry behind each scene.
 *
 * <p>{@link TimelineRailGeometryTest} builds the rail up a piece at a time — what the extent
 * decides, what the anchor decides, that neither touches the other. This one asks the
 * questions that only exist once all of it is assembled and drawn: does the line hold one x
 * for a whole timeline, does a date of any length leave it alone, does a page ever receive a
 * fragment reaching outside its own band, and is a marker made of three shapes really
 * indistinguishable from a plain one.</p>
 *
 * <p>Several of these carry a pixel baseline too, and where they do the comment names it.
 * The division is deliberate: a baseline shows that something is <em>painted</em>, and in
 * what order, and earns its bytes only when the shape itself is the claim; an exact
 * coordinate is a number, and a number belongs in an assertion.</p>
 *
 * <p>No layout snapshot appears here, and that is measured rather than preferred: a snapshot
 * records {@code nodes}, and the rail is a {@code PlacedFragment}. Grep either committed
 * timeline snapshot for {@code timeline-rail} and the count is zero — a snapshot cannot see
 * the rail at all, so re-recording one would prove nothing about it.</p>
 */
class TimelineVisualScenarioGeometryTest {

    private static final DocumentColor RAIL = DocumentColor.rgb(150, 158, 172);
    private static final DocumentColor INK = DocumentColor.rgb(20, 40, 70);
    private static final double MARGIN = 18.0;

    // --- one rail, one x -------------------------------------------------------

    @Test
    void aDateOfAnyLengthLeavesTheRailExactlyWhereTheAxisPutIt() throws Exception {
        // Scenario 3, as an equality rather than as "to the left of". The column width is
        // the caller's declaration and is meant to move the axis; the content poured into
        // that column is not, and a date that wraps to a second line must not shift the
        // line the whole timeline hangs on.
        double shortDates = railX(leadingScene("2023", "2021"));
        double longDates = railX(leadingScene("September 2023 - present", "January 2021"));

        assertThat(longDates)
                .as("the rail belongs to the axis, and the axis does not read the dates")
                .isEqualTo(shortDates, within(1e-9));
    }

    @Test
    void oneTimelineHasOneRailXHoweverManyPagesItCrossesAndWhicheverExtent() throws Exception {
        // The rail is one logical line. Across pages it arrives as several fragments, and
        // "one line" means those fragments agree about x — including under MARKER_TO_MARKER,
        // which changes both ends on the outer pages and must change nothing else.
        for (TimelineRailExtent extent : List.of(TimelineRailExtent.ENTRY_BOUNDS,
                TimelineRailExtent.MARKER_TO_MARKER)) {
            LayoutGraph graph = paginated(extent);

            assertThat(graph.totalPages())
                    .as("the premise: it does cross pages")
                    .isGreaterThanOrEqualTo(3);
            assertThat(rails(graph))
                    .as("%s: one fragment per page", extent)
                    .hasSize(graph.totalPages());
            assertThat(rails(graph).stream().map(PlacedFragment::x).distinct())
                    .as("%s: and one x between them", extent)
                    .hasSize(1);
        }
    }

    @Test
    void noRailFragmentReachesOutsideThePageItIsOn() throws Exception {
        // Scenario 7's hard invariant. A rail derived from anchors could in principle be
        // handed a box belonging to another page and paint into the margin; every fragment
        // is checked against the band its own page actually has.
        LayoutGraph graph = paginated(TimelineRailExtent.ENTRY_BOUNDS);
        double width = graph.canvas().width();
        double height = graph.canvas().height();
        assertThat(graph.canvas().innerHeight())
                .as("the premise: the band is the page less its margins")
                .isEqualTo(height - 2 * MARGIN, within(1e-9));

        assertThat(rails(graph)).allSatisfy(rail -> {
            assertThat(rail.y()).as("bottom").isGreaterThanOrEqualTo(MARGIN - 1e-9);
            assertThat(rail.y() + rail.height())
                    .as("top").isLessThanOrEqualTo(height - MARGIN + 1e-9);
            assertThat(rail.x()).as("left").isGreaterThanOrEqualTo(MARGIN - 1e-9);
            assertThat(rail.x() + rail.width())
                    .as("right").isLessThanOrEqualTo(width - MARGIN + 1e-9);
        });
    }

    // --- the two extents, on one scene ------------------------------------------

    @Test
    void theTwoExtentsDifferInWhereTheyStopAndInNothingElse() throws Exception {
        // The pair of baselines timeline-dsl/entry-bounds and timeline-dsl/marker-to-marker
        // draw: the same page, the same three entries, one argument different. Scenarios 5
        // and 6, asserted against each other so that "shorter" is a comparison and not an
        // impression.
        PlacedFragment bounded = rails(extentScene(TimelineRailExtent.ENTRY_BOUNDS)).get(0);
        LayoutGraph trimmed = extentScene(TimelineRailExtent.MARKER_TO_MARKER);
        PlacedFragment betweenMarkers = rails(trimmed).get(0);
        List<ResolvedLayoutAnchor> markers = markerAnchors(trimmed);

        assertThat(betweenMarkers.x()).as("same x").isEqualTo(bounded.x(), within(1e-9));
        assertThat(betweenMarkers.width())
                .as("same stroke").isEqualTo(bounded.width(), within(1e-9));
        assertThat(betweenMarkers.y() + betweenMarkers.height())
                .as("it begins at the first marker's own centre")
                .isEqualTo(markers.get(0).pointY(0.5), within(1e-9))
                .isLessThan(bounded.y() + bounded.height());
        assertThat(betweenMarkers.y())
                .as("and ends at the last one's")
                .isEqualTo(markers.get(2).pointY(0.5), within(1e-9))
                .isGreaterThan(bounded.y());
    }

    @Test
    void entryBoundsCoversTheSpacingBetweenEntriesOfVeryDifferentHeights() throws Exception {
        // Scenario 6. Three entries whose heights differ by more than a factor of two, with
        // 16pt of spacing between them — and one unbroken line, because an entry's spacing
        // is padding inside its own slice rather than a gap between two of them. If it were
        // a gap, this is the scene where a rail assembled per entry would show it.
        LayoutGraph graph = extentScene(TimelineRailExtent.ENTRY_BOUNDS);
        List<ResolvedLayoutAnchor> entries = entryAnchors(graph);
        PlacedFragment rail = rails(graph).get(0);

        assertThat(entries).hasSize(3);
        assertThat(entries.get(0).height())
                .as("the premise: the first entry is much the tallest")
                .isGreaterThan(2 * entries.get(1).height());
        assertThat(entries.get(0).y())
                .as("no gap between the first slice and the second")
                .isEqualTo(entries.get(1).pointY(1.0), within(1e-9));
        assertThat(entries.get(1).y())
                .isEqualTo(entries.get(2).pointY(1.0), within(1e-9));
        assertThat(rail.height())
                .as("so one fragment spans all three, and the spacing between them")
                .isEqualTo(entries.get(0).pointY(1.0) - entries.get(2).y(), within(1e-9));
    }

    // --- pagination -------------------------------------------------------------

    @Test
    void markerToMarkerAcrossPagesStartsAtTheFirstMarkerAndEndsAtTheLast() throws Exception {
        // Scenario 8, where the extent and pagination meet — and neither had been asked
        // about the other. The first page is trimmed at the top, the last at the bottom,
        // and the page between them carries no marker at all, so it is trimmed at neither
        // end and runs the whole band its entry occupies.
        LayoutGraph graph = paginated(TimelineRailExtent.MARKER_TO_MARKER);
        List<PlacedFragment> rails = rails(graph);
        List<ResolvedLayoutAnchor> markers = markerAnchors(graph);
        List<ResolvedLayoutAnchor> entries = entryAnchors(graph);

        int lastPage = graph.totalPages() - 1;
        assertThat(markers).hasSize(2);
        assertThat(graph.totalPages()).as("the premise: at least one page between them")
                .isGreaterThanOrEqualTo(3);
        assertThat(markers.get(0).pageIndex()).as("first marker, first page").isZero();
        assertThat(markers.get(1).pageIndex()).as("last marker, last page").isEqualTo(lastPage);
        assertThat(markers.stream().map(ResolvedLayoutAnchor::pageIndex))
                .as("the premise: the middle page has no marker on it")
                .doesNotContain(1);

        assertThat(rails.get(0).y() + rails.get(0).height())
                .as("page 0 begins at the first marker")
                .isEqualTo(markers.get(0).pointY(0.5), within(1e-9));
        assertThat(rails.get(lastPage).y())
                .as("the last page ends at the last marker")
                .isEqualTo(markers.get(1).pointY(0.5), within(1e-9));

        List<ResolvedLayoutAnchor> middle = entries.stream()
                .filter(a -> a.pageIndex() == 1).toList();
        assertThat(middle).as("and something of the first entry is on the middle page").isNotEmpty();
        assertThat(rails.get(1).y() + rails.get(1).height())
                .as("which is trimmed at neither end")
                .isEqualTo(middle.stream().mapToDouble(a -> a.pointY(1.0)).max().orElseThrow(),
                        within(1e-9));
        assertThat(rails.get(1).y())
                .isEqualTo(middle.stream().mapToDouble(ResolvedLayoutAnchor::y).min().orElseThrow(),
                        within(1e-9));
    }

    // --- markers of any construction --------------------------------------------

    @Test
    void aHollowMarkerIsAnchoredByItsBoxAndHasTheRailDrawnUnderIt() throws Exception {
        // Scenario 9, and the geometry half of what timeline-dsl/outlined-marker draws. A
        // ring filled with the page's own colour reads as a clean break in the line — but
        // only because the line is painted first. Draw order is list order in this engine,
        // so "under" is a statement about the fragment list and is asserted there.
        LayoutGraph graph = outlinedScene();
        List<ResolvedLayoutAnchor> markers = markerAnchors(graph);
        double railX = rails(graph).get(0).x();

        assertThat(markers.stream().map(ResolvedLayoutAnchor::width))
                .as("three rings, three sizes, one declared box each")
                .containsExactly(10.0, 14.0, 20.0);
        assertThat(markers).allSatisfy(marker -> assertThat(railX)
                .as("each ring centred on the line that runs through it")
                .isEqualTo(marker.pointX(0.5), within(1e-9)));

        assertThat(lastRailIndex(graph))
                .as("the rail is painted before the rings, so each ring's fill covers it")
                .isNotEqualTo(-1)
                .isLessThan(firstEllipseIndex(graph));
    }

    @Test
    void aMarkerOfThreeShapesRailsExactlyAsAPlainOneOfTheSameBox() throws Exception {
        // Scenario 10, stated as an identity. The timeline is told a box and handed a
        // recipe; what the recipe draws inside that box is none of its business, and the
        // proof is that swapping a dot for three stacked ellipses of the same declared size
        // moves neither the anchor nor the rail by anything at all.
        LayoutGraph plain = customMarkerScene(TimelineMarker.dot(16, INK));
        LayoutGraph composed = customMarkerScene(TimelineMarker.custom(16, 16, column ->
                column.addLayerStack(stack -> stack
                        .back(circle(16, INK, null))
                        .center(circle(10, DocumentColor.WHITE, null))
                        .center(circle(4, INK, null)))));

        assertThat(rails(composed).get(0).x())
                .as("same rail")
                .isEqualTo(rails(plain).get(0).x(), within(1e-9));

        ResolvedLayoutAnchor plainMarker = markerAnchors(plain).get(0);
        ResolvedLayoutAnchor composedMarker = markerAnchors(composed).get(0);
        assertThat(composedMarker.x()).as("same x").isEqualTo(plainMarker.x(), within(1e-9));
        assertThat(composedMarker.y()).as("same y").isEqualTo(plainMarker.y(), within(1e-9));
        assertThat(composedMarker.width()).isEqualTo(plainMarker.width(), within(1e-9));
        assertThat(composedMarker.height()).isEqualTo(plainMarker.height(), within(1e-9));

        assertThat(composed.fragments().size())
                .as("the premise: it really is drawn out of more pieces")
                .isGreaterThan(plain.fragments().size());
    }

    // --- two timelines -----------------------------------------------------------

    @Test
    void twoTimelinesWithDifferentAnchorsDoNotShareARail() throws Exception {
        // The last invariant, sharpened: two timelines on one page, one on each anchor.
        // The owners are separate, so the rails are — and each rail answers to its own
        // markers, which is the assertion an owner mix-up would fail.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(320, 420).margin(DocumentInsets.of(MARGIN)).create()) {
            session.pageFlow()
                    .addTimeline(t -> t.connector(RAIL, 1.5).axisWidth(28).gutter(8)
                            .entry(TimelineMarker.dot(8, INK), e -> e.title("Left edge").body("Body."))
                            .entry(TimelineMarker.dot(8, INK), e -> e.title("Left edge too").body("Body.")))
                    .addTimeline(t -> t.connector(RAIL, 1.5).axisWidth(28).markerOnRail()
                            .entry(TimelineMarker.dot(20, INK), e -> e.title("On the rail").body("Body."))
                            .entry(TimelineMarker.dot(20, INK), e -> e.title("On it too").body("Body.")))
                    .build();
            LayoutGraph graph = session.layoutGraph();

            List<PlacedFragment> rails = rails(graph);
            assertThat(rails).hasSize(2);
            assertThat(rails.get(1).x())
                    .as("the anchored one sits well right of the one drawn beside its markers")
                    .isGreaterThan(rails.get(0).x() + 8.0);

            List<ResolvedLayoutAnchor> markers = markerAnchors(graph);
            assertThat(markers).hasSize(4);
            assertThat(rails.get(0).x())
                    .as("the first rail answers to the first timeline's markers")
                    .isEqualTo(markers.get(0).x() - 8.0, within(1e-9));
            assertThat(rails.get(1).x())
                    .as("and the second to the second's")
                    .isEqualTo(markers.get(2).pointX(0.5), within(1e-9));
        }
    }

    // --- scenes ------------------------------------------------------------------

    /** The scene both extent baselines draw; they differ by this argument and nothing else. */
    private static LayoutGraph extentScene(TimelineRailExtent extent) throws Exception {
        return timeline(300, 250, t -> t
                .spacing(16)
                .rail(rail -> rail.extent(extent))
                .entry(TimelineMarker.dot(9, INK), e -> e
                        .title("Tall entry").meta("2023 - Present")
                        .body("A body long enough to run to three lines on a page this "
                              + "narrow, so that this entry is plainly the tallest here."))
                .entry(TimelineMarker.dot(9, INK), e -> e.title("One line only"))
                .entry(TimelineMarker.dot(9, INK), e -> e
                        .title("Middling").body("Two lines, more or less.")));
    }

    /** The scene timeline-dsl/paginated-marker-to-marker draws: two entries, three pages. */
    private static LayoutGraph paginated(TimelineRailExtent extent) throws Exception {
        return timeline(300, 150, t -> t
                .spacing(14)
                .markerOnRail()
                .axisWidth(24)
                .rail(rail -> rail.extent(extent))
                .entry(TimelineMarker.dot(10, INK), e -> e.title("Runs on").body(longBody()))
                .entry(TimelineMarker.dot(10, INK), e -> e.title("And ends here")));
    }

    /** The scene timeline-dsl/outlined-marker draws: rings filled with the page's own colour. */
    private static LayoutGraph outlinedScene() throws Exception {
        DocumentStroke ring = DocumentStroke.of(INK, 1.2);
        return timeline(300, 210, t -> t
                .spacing(12)
                .markerOnRail()
                .axisWidth(26)
                .entry(TimelineMarker.circle(10, DocumentColor.WHITE, ring), e -> e
                        .title("Hollow").body("The line stops inside the ring."))
                .entry(TimelineMarker.circle(14, DocumentColor.WHITE, ring), e -> e
                        .title("Hollow, larger").body("And starts again below it."))
                .entry(TimelineMarker.circle(20, DocumentColor.WHITE, ring), e -> e
                        .title("Hollow, larger still").body("Whatever the diameter.")));
    }

    private static LayoutGraph customMarkerScene(TimelineMarker marker) throws Exception {
        return timeline(320, 260, t -> t
                .markerOnRail()
                .axisWidth(24)
                .entry(marker, e -> e.title("Whatever it is made of").body("Body."))
                .entry(TimelineMarker.dot(8, INK), e -> e.title("Second").body("Body.")));
    }

    private static Consumer<TimelineBuilder> leadingScene(String first, String second) {
        return t -> t
                .leadingColumn(DocumentRowColumn.fixed(60))
                .entry(e -> e.marker(TimelineMarker.dot(8, INK))
                        .leading(d -> d.addParagraph(first)).title("First").body("Body."))
                .entry(e -> e.marker(TimelineMarker.dot(8, INK))
                        .leading(d -> d.addParagraph(second)).title("Second").body("Body."));
    }

    private static String longBody() {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            body.append("Sentence ").append(i).append(" of a body that keeps going. ");
        }
        return body.toString();
    }

    // --- helpers ------------------------------------------------------------------

    private static int lastRailIndex(LayoutGraph graph) {
        int last = -1;
        for (int i = 0; i < graph.fragments().size(); i++) {
            if ("@timeline-rail".equals(graph.fragments().get(i).path())) {
                last = i;
            }
        }
        return last;
    }

    private static int firstEllipseIndex(LayoutGraph graph) {
        for (int i = 0; i < graph.fragments().size(); i++) {
            PlacedFragment fragment = graph.fragments().get(i);
            if (fragment.payload() != null
                && fragment.payload().getClass().getSimpleName().contains("Ellipse")) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    private static EllipseNode circle(double size, DocumentColor fill, DocumentStroke stroke) {
        return new EllipseNode("marker", size, size, fill, stroke, null, null, null, null);
    }

    private static double railX(Consumer<TimelineBuilder> spec) throws Exception {
        return rails(timeline(360, 300, spec)).get(0).x();
    }

    private static List<PlacedFragment> rails(LayoutGraph graph) {
        return graph.fragments().stream().filter(f -> "@timeline-rail".equals(f.path())).toList();
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
                .pageSize(width, height).margin(DocumentInsets.of(MARGIN)).create()) {
            session.pageFlow().addTimeline(t -> {
                t.connector(RAIL, 1.5);
                spec.accept(t);
            }).build();
            return session.layoutGraph();
        }
    }
}
