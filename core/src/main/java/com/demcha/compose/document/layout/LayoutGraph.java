package com.demcha.compose.document.layout;

import java.util.List;
import java.util.Objects;

/**
 * Canonical resolved v2 layout graph.
 *
 * @param canvas       physical page canvas used for the graph
 * @param totalPages   number of pages required by the graph
 * @param nodes        placed semantic nodes in deterministic order
 * @param fragments    placed render fragments in deterministic order
 * @param pageGeometry the margin each page was laid out against — the canvas's on every page
 *                     unless the document sets per-page margins
 * @since 2.5.0 carries {@code pageGeometry}
 */
public record LayoutGraph(
        LayoutCanvas canvas,
        int totalPages,
        List<PlacedNode> nodes,
        List<PlacedFragment> fragments,
        PageGeometry pageGeometry
) {
    /**
     * Validates required graph fields and freezes ordered node/fragment lists. A graph given no
     * page geometry has the canvas's margin on every page.
     */
    public LayoutGraph {
        Objects.requireNonNull(canvas, "canvas");
        nodes = List.copyOf(nodes);
        fragments = List.copyOf(fragments);
        pageGeometry = pageGeometry == null ? PageGeometry.of(canvas, List.of()) : pageGeometry;
    }

    /**
     * A graph laid out against the canvas's margin on every page.
     *
     * @param canvas     physical page canvas used for the graph
     * @param totalPages number of pages required by the graph
     * @param nodes      placed semantic nodes in deterministic order
     * @param fragments  placed render fragments in deterministic order
     */
    public LayoutGraph(LayoutCanvas canvas, int totalPages, List<PlacedNode> nodes, List<PlacedFragment> fragments) {
        this(canvas, totalPages, nodes, fragments, null);
    }

    /**
     * This layout with other fragments: what a pass that draws over a laid-out document gives
     * back, its pages, nodes and page geometry the same.
     *
     * @param fragments the fragments in their paint order
     * @return the layout with them
     * @since 2.5.0
     */
    public LayoutGraph withFragments(List<PlacedFragment> fragments) {
        return new LayoutGraph(canvas, totalPages, nodes, fragments, pageGeometry);
    }
}
