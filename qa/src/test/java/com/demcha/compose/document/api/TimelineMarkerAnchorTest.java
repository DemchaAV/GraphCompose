package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.dsl.TimelineBuilder;
import com.demcha.compose.document.dsl.TimelineMarker;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.ResolvedLayoutAnchor;
import com.demcha.compose.document.layout.ResolvedLayoutMetadata;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Every timeline marker now reports where it landed.
 *
 * <p>This is the first use of the resolved-layout seam: each marker is wrapped so the
 * finished graph carries one anchor per marker, and a later phase computes the rail from
 * those rather than from a section border. What is asserted here is what that later phase
 * will depend on — one anchor per marker, on the page the marker is actually on, at the
 * marker's own box and not its column's, and independent of how many shapes the marker
 * took to draw.</p>
 *
 * <p>The anchors are read straight out of the compiled graph. Nothing registers a pass yet;
 * a pass is what Phase 9 adds, and it will read exactly this.</p>
 */
class TimelineMarkerAnchorTest {

    private static final DocumentColor RAIL = DocumentColor.rgb(150, 158, 172);
    private static final DocumentColor INK = DocumentColor.rgb(20, 40, 70);

    @Test
    void everyMarkerLeavesOneAnchorInTheFinishedGraph() throws Exception {
        LayoutGraph graph = timeline(360, t -> t
                .entry(TimelineMarker.dot(8, INK), e -> e.title("First"))
                .entry(TimelineMarker.numbered(2, 14, INK, DocumentColor.WHITE), e -> e.title("Second"))
                .entry(TimelineMarker.square(10, INK), e -> e.title("Third")));

        List<ResolvedLayoutAnchor> anchors = markerAnchors(graph);
        assertThat(anchors).hasSize(3);
        assertThat(anchors.stream().map(a -> a.id().index()))
                .as("indexed by entry, in document order")
                .containsExactly(0, 1, 2);
        assertThat(anchors.stream().map(a -> a.id().groupKey()).distinct())
                .as("one owner for the timeline")
                .hasSize(1);
    }

    @Test
    void anAnchorIsTheMarkersOwnBoxAndNotItsColumns() throws Exception {
        // The distinction the seam exists for. The marker column is 20pt wide here and the
        // marker is 8pt; a rail taking the column would sit 6pt off the ink.
        LayoutGraph graph = timeline(360, t -> t
                .axisWidth(20)
                .entry(TimelineMarker.dot(8, INK), e -> e.title("Only")));

        ResolvedLayoutAnchor anchor = markerAnchors(graph).get(0);
        PlacedFragment ellipse = graph.fragments().stream()
                .filter(f -> f.payload() != null
                        && f.payload().getClass().getSimpleName().contains("Ellipse"))
                .findFirst().orElseThrow();

        assertThat(anchor.width()).isEqualTo(8.0, within(1e-9));
        assertThat(anchor.height()).isEqualTo(8.0, within(1e-9));
        assertThat(anchor.x()).isEqualTo(ellipse.x(), within(1e-9));
        assertThat(anchor.y()).isEqualTo(ellipse.y(), within(1e-9));
    }

    @Test
    void aMarkerOfThreeShapesLeavesTheSameOneAnchorAsAMarkerOfOne() throws Exception {
        // The binding contract: the number of fragments a marker draws must not be
        // visible to whatever anchors on it.
        ResolvedLayoutAnchor single = onlyAnchor(TimelineMarker.dot(16, INK));
        ResolvedLayoutAnchor composed = onlyAnchor(TimelineMarker.custom(16, 16, column ->
                column.addLayerStack(stack -> stack
                        .back(circle(16, INK))
                        .center(circle(10, DocumentColor.WHITE))
                        .center(circle(4, INK)))));

        assertThat(composed.x()).isEqualTo(single.x(), within(1e-9));
        assertThat(composed.y()).isEqualTo(single.y(), within(1e-9));
        assertThat(composed.width()).isEqualTo(single.width(), within(1e-9));
        assertThat(composed.height()).isEqualTo(single.height(), within(1e-9));
    }

    @Test
    void anAnchorsLeftEdgeHoldsStillWhileItsCentreMovesWithTheMarker() throws Exception {
        // Measured, and it is the whole reason the rail is anchored by a fraction plus an
        // offset rather than by "the centre". Markers of different sizes share a left edge
        // — they are left-packed in their column — so the centre of a 6pt marker and of a
        // 24pt one are 9pt apart. A rail that wanted to pass through both centres could
        // not; a rail placed at the left edge plus a constant can.
        LayoutGraph graph = timeline(360, t -> t
                .axisWidth(28)
                .entry(TimelineMarker.dot(6, INK), e -> e.title("Small"))
                .entry(TimelineMarker.dot(24, INK), e -> e.title("Large")));

        List<ResolvedLayoutAnchor> anchors = markerAnchors(graph);
        assertThat(anchors.get(0).x())
                .as("same left edge whatever the marker's size")
                .isEqualTo(anchors.get(1).x(), within(1e-9));
        assertThat(anchors.get(1).pointX(0.5) - anchors.get(0).pointX(0.5))
                .as("while the centres are half the size difference apart")
                .isEqualTo(9.0, within(1e-9));
    }

    @Test
    void anAnchorReportsThePageItsMarkerIsActuallyOn() throws Exception {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < 14; i++) {
            body.append("Sentence ").append(i).append(" of a body long enough to run on. ");
        }
        LayoutGraph graph = timeline(320, 170, t -> t
                .entry(TimelineMarker.dot(8, INK), e -> e.title("First").body(body.toString()))
                .entry(TimelineMarker.dot(8, INK), e -> e.title("Second").body("Short.")));

        assertThat(graph.totalPages()).isGreaterThan(1);
        List<ResolvedLayoutAnchor> anchors = markerAnchors(graph);
        assertThat(anchors).hasSize(2);
        assertThat(anchors.get(0).pageIndex()).isZero();
        assertThat(anchors.get(1).pageIndex())
                .as("the second marker is pushed onto the next page and says so")
                .isEqualTo(1);
    }

    @Test
    void twoTimelinesOnOnePageKeepTheirMarkersApart() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(360, 400).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow()
                    .addTimeline(t -> t.connector(RAIL, 1.5)
                            .entry(TimelineMarker.dot(8, INK), e -> e.title("A1"))
                            .entry(TimelineMarker.dot(8, INK), e -> e.title("A2")))
                    .addTimeline(t -> t.connector(RAIL, 1.5)
                            .entry(TimelineMarker.dot(8, INK), e -> e.title("B1")))
                    .build();

            List<ResolvedLayoutAnchor> anchors =
                    markerAnchors(session.layoutGraph());
            assertThat(anchors).hasSize(3);

            Object first = anchors.get(0).id().groupKey();
            Object second = anchors.get(2).id().groupKey();
            assertThat(first).isNotSameAs(second);
            assertThat(ResolvedLayoutMetadata.from(session.layoutGraph())
                    .anchors(first, anchors.get(0).id().kind()))
                    .as("asking for one timeline's owner returns that timeline's markers only")
                    .hasSize(2);
        }
    }

    private static com.demcha.compose.document.node.EllipseNode circle(double size, DocumentColor fill) {
        return new com.demcha.compose.document.node.EllipseNode(
                "marker", size, size, fill, null, null, null, null, null);
    }

    /**
     * The marker anchors only.
     *
     * <p>A timeline anchors its entries as well as its markers — the rail needs each
     * entry's extent on each page — so a test about markers has to say which it means.</p>
     */
    private static List<ResolvedLayoutAnchor> markerAnchors(LayoutGraph graph) {
        return ResolvedLayoutMetadata.from(graph).anchors().stream()
                .filter(a -> "MARKER".equals(a.id().kind().toString()))
                .toList();
    }

    private static ResolvedLayoutAnchor onlyAnchor(TimelineMarker marker) throws Exception {
        LayoutGraph graph = timeline(360, t -> t
                .axisWidth(20)
                .entry(marker, e -> e.title("Only")));
        return markerAnchors(graph).get(0);
    }

    private static LayoutGraph timeline(double pageWidth, Consumer<TimelineBuilder> spec) throws Exception {
        return timeline(pageWidth, 400, spec);
    }

    private static LayoutGraph timeline(double pageWidth, double pageHeight,
                                        Consumer<TimelineBuilder> spec) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(pageWidth, pageHeight)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow().addTimeline(t -> {
                t.connector(RAIL, 1.5);
                spec.accept(t);
            }).build();
            return session.layoutGraph();
        }
    }
}
