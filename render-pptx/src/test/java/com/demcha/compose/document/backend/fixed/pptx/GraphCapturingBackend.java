package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.document.backend.fixed.FixedLayoutBackend;
import com.demcha.compose.document.backend.fixed.FixedLayoutRenderContext;
import com.demcha.compose.document.layout.LayoutGraph;

/**
 * Test backend that returns the resolved {@link LayoutGraph} instead of
 * rendering it — the geometry tests compare the PPTX output against exactly
 * the graph the backend consumed, with no snapshot indirection.
 */
final class GraphCapturingBackend implements FixedLayoutBackend<LayoutGraph> {

    @Override
    public String name() {
        return "graph-capture";
    }

    @Override
    public LayoutGraph render(LayoutGraph graph, FixedLayoutRenderContext context) {
        return graph;
    }
}
