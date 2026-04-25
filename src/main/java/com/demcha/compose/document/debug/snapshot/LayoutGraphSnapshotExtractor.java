package com.demcha.compose.document.debug.snapshot;

import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.snapshot.LayoutCanvasSnapshot;
import com.demcha.compose.document.snapshot.LayoutInsetsSnapshot;
import com.demcha.compose.document.snapshot.LayoutNodeSnapshot;
import com.demcha.compose.document.snapshot.LayoutSnapshot;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Converts the canonical v2 layout graph into the existing snapshot format.
 */
public final class LayoutGraphSnapshotExtractor {
    /**
     * Snapshot format version emitted by the canonical graph extractor.
     */
    public static final String FORMAT_VERSION = "2.0";

    private LayoutGraphSnapshotExtractor() {
    }

    /**
     * Converts a canonical layout graph into a stable snapshot model.
     *
     * @param graph resolved layout graph
     * @return layout snapshot
     */
    public static LayoutSnapshot extract(LayoutGraph graph) {
        Objects.requireNonNull(graph, "graph");
        return new LayoutSnapshot(
                FORMAT_VERSION,
                new LayoutCanvasSnapshot(
                        normalize(graph.canvas().width()),
                        normalize(graph.canvas().height()),
                        normalize(graph.canvas().innerWidth()),
                        normalize(graph.canvas().innerHeight()),
                        from(graph.canvas().margin())),
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
                from(node.margin()),
                from(node.padding()));
    }

    private static LayoutInsetsSnapshot from(Margin margin) {
        Margin safeMargin = margin == null ? Margin.zero() : margin;
        return new LayoutInsetsSnapshot(
                normalize(safeMargin.top()),
                normalize(safeMargin.right()),
                normalize(safeMargin.bottom()),
                normalize(safeMargin.left()));
    }

    private static LayoutInsetsSnapshot from(Padding padding) {
        Padding safePadding = padding == null ? Padding.zero() : padding;
        return new LayoutInsetsSnapshot(
                normalize(safePadding.top()),
                normalize(safePadding.right()),
                normalize(safePadding.bottom()),
                normalize(safePadding.left()));
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



