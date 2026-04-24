package com.demcha.compose.document.debug.snapshot;

import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedNode;

import com.demcha.compose.engine.debug.LayoutCanvasSnapshot;
import com.demcha.compose.engine.debug.LayoutInsetsSnapshot;
import com.demcha.compose.engine.debug.LayoutNodeSnapshot;
import com.demcha.compose.engine.debug.LayoutSnapshot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Converts the canonical v2 layout graph into the existing snapshot format.
 */
public final class LayoutGraphSnapshotExtractor {
    public static final String FORMAT_VERSION = "2.0";

    private LayoutGraphSnapshotExtractor() {
    }

    public static LayoutSnapshot extract(LayoutGraph graph) {
        Objects.requireNonNull(graph, "graph");
        return new LayoutSnapshot(
                FORMAT_VERSION,
                new LayoutCanvasSnapshot(
                        normalize(graph.canvas().width()),
                        normalize(graph.canvas().height()),
                        normalize(graph.canvas().innerWidth()),
                        normalize(graph.canvas().innerHeight()),
                        LayoutInsetsSnapshot.from(graph.canvas().margin())),
                graph.totalPages(),
                graph.nodes().stream()
                        .map(LayoutGraphSnapshotExtractor::toNodeSnapshot)
                        .toList());
    }

    private static LayoutNodeSnapshot toNodeSnapshot(PlacedNode node) {
        return new LayoutNodeSnapshot(
                node.path(),
                node.semanticName(),
                node.nodeKind(),
                node.parentPath(),
                node.childIndex(),
                node.depth(),
                node.layer(),
                normalize(node.computedX()),
                normalize(node.computedY()),
                normalize(node.placementX()),
                normalize(node.placementY()),
                normalize(node.placementWidth()),
                normalize(node.placementHeight()),
                node.startPage(),
                node.endPage(),
                normalize(node.contentWidth()),
                normalize(node.contentHeight()),
                LayoutInsetsSnapshot.from(node.margin()),
                LayoutInsetsSnapshot.from(node.padding()));
    }

    static double normalize(double value) {
        if (Math.abs(value) < 0.0005d) {
            return 0.0d;
        }
        return BigDecimal.valueOf(value)
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();
    }
}



