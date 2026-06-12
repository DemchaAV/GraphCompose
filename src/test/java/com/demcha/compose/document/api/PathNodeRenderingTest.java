package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.PathNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.demcha.compose.document.style.DocumentPathSegment.close;
import static com.demcha.compose.document.style.DocumentPathSegment.cubicTo;
import static com.demcha.compose.document.style.DocumentPathSegment.moveTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end coverage for the {@link PathNode} primitive: atomic placement
 * with stable semantic paths, deterministic layout snapshot, and a valid PDF
 * render through the native curve-operator handler.
 */
class PathNodeRenderingTest {

    @Test
    void pathNodesPlaceAtomicallyAndSnapshotDeterministically() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(240, 200)
                .margin(DocumentInsets.of(12))
                .create()) {
            session.add(wave());
            session.add(blob());

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages()).isEqualTo(1);
            assertThat(graph.nodes()).extracting(PlacedNode::path)
                    .contains("Wave[0]", "Blob[1]");

            LayoutSnapshotAssertions.assertMatches(session, "document/path_primitive");

            byte[] pdf = session.toPdfBytes();
            assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        }
    }

    private static PathNode wave() {
        return new PathNode("Wave", 200, 70,
                List.of(moveTo(0.0, 0.5),
                        cubicTo(0.25, 1.0, 0.25, 0.0, 0.5, 0.5),
                        cubicTo(0.75, 1.0, 0.75, 0.0, 1.0, 0.5)),
                null,
                DocumentStroke.of(DocumentColor.rgb(20, 60, 120), 2.0),
                DocumentInsets.zero(), DocumentInsets.bottom(8));
    }

    private static PathNode blob() {
        return new PathNode("Blob", 90, 80,
                List.of(moveTo(0.5, 1.0),
                        cubicTo(1.1, 0.95, 0.95, 0.1, 0.5, 0.0),
                        cubicTo(0.05, 0.1, -0.1, 0.95, 0.5, 1.0),
                        close()),
                DocumentColor.rgb(235, 205, 160),
                DocumentStroke.of(DocumentColor.rgb(140, 90, 30), 1.2),
                DocumentInsets.zero(), DocumentInsets.zero());
    }
}
