package com.demcha.compose.v2;

/**
 * Backend that consumes a resolved v2 layout graph.
 *
 * @param <R> render result type
 */
public interface FixedLayoutBackend<R> {
    String name();

    R render(LayoutGraph graph, FixedLayoutRenderContext context) throws Exception;
}
