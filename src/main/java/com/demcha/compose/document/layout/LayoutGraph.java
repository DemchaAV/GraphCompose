package com.demcha.compose.document.layout;

import java.util.List;
import java.util.Objects;

/**
 * Canonical resolved v2 layout graph.
 */
public record LayoutGraph(
        LayoutCanvas canvas,
        int totalPages,
        List<PlacedNode> nodes,
        List<PlacedFragment> fragments
) {
    public LayoutGraph {
        Objects.requireNonNull(canvas, "canvas");
        nodes = List.copyOf(nodes);
        fragments = List.copyOf(fragments);
    }
}


