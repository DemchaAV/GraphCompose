package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.dsl.TimelineMarker;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

/**
 * PHASE 0b SPIKE — throwaway. Delete or promote before the PR.
 *
 * <p>No horizontal composite flows across pages: {@code Axis.HORIZONTAL} routes to
 * {@code compileHorizontalRow} and {@code Axis.STACK} to the stacked compiler, both
 * atomic; only the vertical path reaches {@code compileComposite}, which flows.</p>
 *
 * <p>So a single three-column container cannot hold splittable content. The question
 * this probe answers is whether the shape already in use works instead: an entry that is
 * a vertical section (flows) whose marker sits in an atomic header row, with the body as
 * siblings underneath. If the entry section spans two pages while its header row stays
 * whole on one, the structure is viable and the rework does not need a new container.</p>
 */
class TimelineEntrySplitProbeTest {

    private static final DocumentColor RAIL = DocumentColor.rgb(150, 158, 172);
    private static final DocumentColor INK = DocumentColor.rgb(20, 60, 160);

    @Test
    void doesOneLongEntryContinueOntoTheNextPage() throws Exception {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            body.append("Body sentence number ").append(i)
                .append(" carrying enough words to take a whole line of its own. ");
        }

        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 160)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow()
                    .addTimeline(t -> t
                            .connector(RAIL, 1.5)
                            .entry(TimelineMarker.dot(8, INK), e -> e
                                    .title("Senior Engineer")
                                    .meta("2023 — Present")
                                    .body(body.toString())))
                    .build();

            LayoutGraph graph = session.layoutGraph();
            System.out.println("PROBE pages=" + graph.totalPages());
            for (PlacedNode n : graph.nodes()) {
                System.out.printf("PROBE %-14s p%d..%d  h=%7.2f%n",
                        n.nodeKind(), n.startPage(), n.endPage(), n.placementHeight());
            }
        }
    }
}
