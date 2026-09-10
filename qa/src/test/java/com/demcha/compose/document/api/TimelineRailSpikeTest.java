package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.dsl.TimelineMarker;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

/**
 * PHASE 0a SPIKE — throwaway. Delete or promote before the PR.
 *
 * <p>Answers three questions the plan blocks on, by measurement rather than reading:
 * (1) can a post-layout pass find the timeline markers in a resolved {@code LayoutGraph},
 * (2) what coordinate convention do {@code PlacedNode} boxes and {@code PlacedFragment}
 * coordinates share, and (3) does the marker box move when the marker's size changes —
 * which is what a rail derived from anchors must be immune to.</p>
 */
class TimelineRailSpikeTest {

    private static final DocumentColor RAIL = DocumentColor.rgb(150, 158, 172);
    private static final DocumentColor INK = DocumentColor.rgb(20, 60, 160);

    @Test
    void dumpResolvedTimelineGeometry() throws Exception {
        System.out.println("=== markers at size 8 ===");
        dump(8.0);
        System.out.println();
        System.out.println("=== markers at size 20 (rail must not move) ===");
        dump(20.0);
    }

    private static void dump(double markerSize) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 260)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow()
                    .addTimeline(t -> t
                            .connector(RAIL, 1.5)
                            .spacing(14)
                            .entry(TimelineMarker.dot(markerSize, INK), entry("First"))
                            .entry(TimelineMarker.dot(markerSize, INK), entry("Second"))
                            .entry(TimelineMarker.dot(markerSize, INK), entry("Third")))
                    .build();

            LayoutGraph graph = session.layoutGraph();
            System.out.println("pages=" + graph.totalPages()
                    + " canvas=" + graph.canvas().width() + "x" + graph.canvas().height());

            System.out.println("-- nodes (kind, name, computed, placement, pages) --");
            for (PlacedNode n : graph.nodes()) {
                System.out.printf(
                        "   %-16s %-22s computed=(%7.2f,%7.2f) placement=(%7.2f,%7.2f %6.2fx%6.2f) p%d..%d%n",
                        n.nodeKind(), truncate(n.semanticName()),
                        n.computedX(), n.computedY(),
                        n.placementX(), n.placementY(), n.placementWidth(), n.placementHeight(),
                        n.startPage(), n.endPage());
            }

            System.out.println("-- fragments (payload, box) --");
            List<PlacedFragment> fragments = graph.fragments();
            for (PlacedFragment f : fragments) {
                String payload = f.payload() == null ? "null" : f.payload().getClass().getSimpleName();
                System.out.printf("   %-28s page=%d box=(%7.2f,%7.2f %6.2fx%6.2f) path=%s%n",
                        payload, f.pageIndex(), f.x(), f.y(), f.width(), f.height(), truncate(f.path()));
            }
        }
    }

    private static Consumer<com.demcha.compose.document.dsl.TimelineEntryBuilder> entry(String title) {
        return e -> e.title(title).meta("2024").body("Body text for " + title + ".");
    }

    private static String truncate(String s) {
        if (s == null) {
            return "-";
        }
        return s.length() <= 40 ? s : "…" + s.substring(s.length() - 39);
    }
}
