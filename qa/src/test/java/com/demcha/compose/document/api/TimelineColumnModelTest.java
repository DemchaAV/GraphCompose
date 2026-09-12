package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.dsl.TimelineBuilder;
import com.demcha.compose.document.dsl.TimelineMarker;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * What a three-column timeline can and cannot be asked for.
 *
 * <p>A row spaces every pair of its columns by one number. With a leading column that made
 * three columns share two gaps of the same size, and the arithmetic then decided the leading
 * width for the author: writing {@code x0} for where the columns start, {@code L} for the
 * leading width and {@code A} for the axis, one gap {@code s} puts the rail at
 * {@code x0 + L + s + A/2} and the content at {@code x0 + L + 2s + A}, so
 * {@code L = (rail − x0) − (s + A/2)} whatever {@code A} and {@code s} are. A design that
 * states where its dates are, where its rail is and where its copy starts — which is what a
 * dated timeline is — could not have all three.</p>
 *
 * <p>The gaps are separate now, so {@code L} is the caller's. Unset, the leading gap is the
 * marker gap and the layout is the one-gap layout exactly, which is the first thing below.</p>
 */
class TimelineColumnModelTest {

    private static final DocumentColor RAIL = DocumentColor.rgb(150, 158, 172);
    private static final DocumentColor INK = DocumentColor.rgb(20, 40, 70);
    private static final double MARGIN = 20.0;
    private static final double AXIS = 10.0;

    @Test
    void unsetTheLeadingGapIsTheMarkerGapAndNothingMoved() throws Exception {
        // The compatibility half. x0 is the page margin plus the gutter, and with one gap
        // the two positions are the formulae above — so this pins the layout a three-column
        // timeline had before there were two gaps.
        double gutter = 6.0;
        double leading = 40.0;
        double gap = 9.0;
        LayoutGraph graph = timeline(t -> t
                .markerOnRail()
                .gutter(gutter)
                .leadingColumn(DocumentRowColumn.fixed(leading))
                .axisWidth(AXIS)
                .markerGap(gap));

        double x0 = MARGIN + gutter;
        assertThat(railX(graph))
                .as("rail at x0 + L + s + A/2")
                .isEqualTo(x0 + leading + gap + AXIS / 2.0, within(1e-9));
        assertThat(node(graph, "Copy").placementX())
                .as("content at x0 + L + 2s + A")
                .isEqualTo(x0 + leading + 2 * gap + AXIS, within(1e-9));
    }

    @Test
    void theLeadingWidthIsFreeOnceTheTwoGapsAre() throws Exception {
        // The capability, stated as the thing that was impossible: two different leading
        // widths, the same rail and the same content start. Only the sum L + leadingGap is
        // pinned by the geometry, so the author chooses how it is divided.
        double gutter = 6.0;
        double markerGap = 9.0;
        LayoutGraph narrow = timeline(t -> t
                .markerOnRail().gutter(gutter)
                .leadingColumn(DocumentRowColumn.fixed(40.0)).leadingGap(19.0)
                .axisWidth(AXIS).markerGap(markerGap));
        LayoutGraph wide = timeline(t -> t
                .markerOnRail().gutter(gutter)
                .leadingColumn(DocumentRowColumn.fixed(52.0)).leadingGap(7.0)
                .axisWidth(AXIS).markerGap(markerGap));

        assertThat(railX(wide))
                .as("a leading column 12pt wider, and the rail has not moved")
                .isEqualTo(railX(narrow), within(1e-9));
        assertThat(node(wide, "Copy").placementX())
                .as("nor has the content")
                .isEqualTo(node(narrow, "Copy").placementX(), within(1e-9));
        assertThat(node(wide, "Copy").placementWidth())
                .as("so the weighted content column measures the same too")
                .isEqualTo(node(narrow, "Copy").placementWidth(), within(1e-9));
    }

    @Test
    void theLeadingColumnIsTheWidthItsContentMeasuresAgainst() throws Exception {
        // The point of wanting L free: what goes in the leading column has L to fit in. The
        // same text in a column too narrow for it wraps to two lines and in a wide enough
        // one does not — and that is the difference between a date set on one line and a
        // date broken across two.
        Consumer<TimelineBuilder> common = t -> t
                .markerOnRail().gutter(6.0).axisWidth(AXIS).markerGap(9.0);
        LayoutGraph tight = timeline(t -> {
            common.accept(t);
            t.leadingColumn(DocumentRowColumn.fixed(26.0)).leadingGap(33.0);
        });
        LayoutGraph roomy = timeline(t -> {
            common.accept(t);
            t.leadingColumn(DocumentRowColumn.fixed(52.0)).leadingGap(7.0);
        });

        double oneLine = node(roomy, "Date").placementHeight();
        double twoLines = node(tight, "Date").placementHeight();
        assertThat(twoLines)
                .as("the narrow column wraps the date, the wide one does not")
                .isGreaterThan(oneLine * 1.5);
        assertThat(railX(roomy))
                .as("and widening the column to stop that moved neither the rail")
                .isEqualTo(railX(tight), within(1e-9));
        assertThat(node(roomy, "Copy").placementX())
                .as("nor the copy")
                .isEqualTo(node(tight, "Copy").placementX(), within(1e-9));
    }

    @Test
    void aTimelineWithNoLeadingColumnIsUntouched() throws Exception {
        // Two columns share one gap and always did. This is the released layout and the
        // leading gap has nothing to say about it.
        double gutter = 6.0;
        double gap = 9.0;
        LayoutGraph plain = timelineWithoutLeading(t -> t
                .markerOnRail().gutter(gutter).axisWidth(AXIS).markerGap(gap));
        LayoutGraph asked = timelineWithoutLeading(t -> t
                .markerOnRail().gutter(gutter).axisWidth(AXIS).markerGap(gap).leadingGap(40.0));

        double x0 = MARGIN + gutter;
        assertThat(railX(plain))
                .as("rail at x0 + A/2")
                .isEqualTo(x0 + AXIS / 2.0, within(1e-9));
        assertThat(railX(asked))
                .as("and a leading gap on a timeline with no leading column changes nothing")
                .isEqualTo(railX(plain), within(1e-9));
        assertThat(node(asked, "Copy").placementX())
                .isEqualTo(node(plain, "Copy").placementX(), within(1e-9));
    }

    @Test
    void aWeightedLeadingColumnStillResolvesAgainstTheRow() throws Exception {
        // Fixed is what a measured design uses, but the column takes a weight too and the
        // gaps must not stop it resolving: a weighted leading column plus two fixed gaps
        // plus a fixed axis plus a weighted content share the row.
        LayoutGraph graph = timeline(t -> t
                .markerOnRail().gutter(6.0)
                .leadingColumn(DocumentRowColumn.weight(0.25)).leadingGap(7.0)
                .axisWidth(AXIS).markerGap(9.0));

        PlacedNode copy = node(graph, "Copy");
        assertThat(node(graph, "Date").placementWidth())
                .as("the weighted leading column took a share of the row")
                .isGreaterThan(0.0);
        assertThat(copy.placementX())
                .as("and the content still starts right of the axis")
                .isGreaterThan(railX(graph) + AXIS / 2.0);
        assertThat(copy.placementWidth()).isGreaterThan(0.0);
    }

    // --- helpers ---------------------------------------------------------------

    /** The rail's x, which is where the marker anchor put it. */
    private static double railX(LayoutGraph graph) {
        return graph.fragments().stream()
                .filter(f -> "@timeline-rail".equals(f.path()))
                .mapToDouble(PlacedFragment::x)
                .findFirst().orElseThrow(() -> new AssertionError("no rail"));
    }

    private static PlacedNode node(LayoutGraph graph, String name) {
        return graph.nodes().stream()
                .filter(n -> name.equals(n.semanticName()))
                .findFirst().orElseThrow(() -> new AssertionError("no node named " + name));
    }

    /** The same two entries with nothing in a leading column, which there isn't one of. */
    private static LayoutGraph timelineWithoutLeading(Consumer<TimelineBuilder> spec)
            throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(320, 400).margin(DocumentInsets.of(MARGIN)).create()) {
            session.pageFlow().addTimeline(t -> {
                t.connector(RAIL, 1.5);
                spec.accept(t);
                t.entry(TimelineMarker.dot(8, INK), e -> e
                        .content(column -> column.addParagraph(p -> p
                                .name("Copy").text("The copy beside the marker.")
                                .margin(DocumentInsets.zero()))));
                t.entry(TimelineMarker.dot(8, INK), e -> e
                        .content(column -> column.addParagraph(p -> p
                                .name("Copy2").text("More copy beside a marker.")
                                .margin(DocumentInsets.zero()))));
            }).build();
            return session.layoutGraph();
        }
    }

    /** Two entries, each with a named date in the leading column and named copy beside. */
    private static LayoutGraph timeline(Consumer<TimelineBuilder> spec) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(320, 400).margin(DocumentInsets.of(MARGIN)).create()) {
            session.pageFlow().addTimeline(t -> {
                t.connector(RAIL, 1.5);
                spec.accept(t);
                t.entry(TimelineMarker.dot(8, INK), e -> e
                        .leading(column -> column.addParagraph(p -> p
                                .name("Date").text("2022 - Present")
                                .margin(DocumentInsets.zero())))
                        .content(column -> column.addParagraph(p -> p
                                .name("Copy").text("The copy beside the marker.")
                                .margin(DocumentInsets.zero()))));
                t.entry(TimelineMarker.dot(8, INK), e -> e
                        .leading(column -> column.addParagraph(p -> p
                                .name("Date2").text("2020 - 2022")
                                .margin(DocumentInsets.zero())))
                        .content(column -> column.addParagraph(p -> p
                                .name("Copy2").text("More copy beside a marker.")
                                .margin(DocumentInsets.zero()))));
            }).build();
            return session.layoutGraph();
        }
    }
}
