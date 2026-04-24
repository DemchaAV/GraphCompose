package com.demcha.compose.document.layout;

import java.util.List;
import java.util.Objects;

/**
 * Canonical resolved v2 layout graph.
 *
 * @param canvas physical page canvas used for the graph
 * @param totalPages number of pages required by the graph
 * @param nodes placed semantic nodes in deterministic order
 * @param fragments placed render fragments in deterministic order
 */
public record LayoutGraph(
        LayoutCanvas canvas,
        int totalPages,
        List<PlacedNode> nodes,
        List<PlacedFragment> fragments
) {
    /**
     * Validates required graph fields and freezes ordered node/fragment lists.
     */
    public LayoutGraph {
        Objects.requireNonNull(canvas, "canvas");
        nodes = List.copyOf(nodes);
        fragments = List.copyOf(fragments);
    }
}


