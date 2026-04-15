package com.demcha.compose.document.backend.fixed;

import com.demcha.compose.document.layout.LayoutGraph;

/**
 * Backend that consumes a resolved v2 layout graph.
 *
 * @param <R> render result type
 */
public interface FixedLayoutBackend<R> {

    /**
     * Returns the backend identifier used for diagnostics and manifests.
     *
     * @return stable backend name
     */
    String name();

    /**
     * Renders one resolved layout graph.
     *
     * @param graph resolved graph emitted by the semantic compiler
     * @param context render-pass configuration and output target
     * @return backend-specific render result
     * @throws Exception if rendering fails
     */
    R render(LayoutGraph graph, FixedLayoutRenderContext context) throws Exception;
}



