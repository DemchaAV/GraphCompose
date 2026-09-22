package com.demcha.compose.document.backend.semantic;

import com.demcha.compose.document.layout.DocumentGraph;

/**
 * Backend that consumes the semantic v2 document graph directly.
 *
 * @param <R> export result type
 */
public interface SemanticBackend<R> {

    /**
     * Returns the backend identifier used for diagnostics and manifests.
     *
     * @return stable backend name
     */
    String name();

    /**
     * Whether this backend needs the compiled layout alongside the semantic graph.
     *
     * <p>A semantic backend walks the authored tree and needs no geometry, which is why
     * the default is {@code false} and why the session does not compile a layout for one.
     * Compiling one runs measurement and pagination over the whole document — work that
     * grows with the document and that an export ignoring geometry has no use for.</p>
     *
     * <p>It does <em>not</em> save the render-module dependency, and this flag should not
     * be described as if it did: {@code DocumentSession} resolves a
     * {@code FontMetricsProvider} in its constructor, only
     * {@code graph-compose-render-pdf} registers one, and a session cannot be created
     * without it whatever a backend later asks for.</p>
     *
     * <p>A backend that answers {@code true} is handed the same session's compiled layout
     * in {@link SemanticExportContext#layoutGraph()}. It is for reading what the engine
     * already worked out — a resolved width, a settled page count — not for placing
     * content at coordinates; a backend that wants coordinates is a fixed-layout backend
     * and should implement that contract instead.</p>
     *
     * @return true to be given a resolved layout; false to be given the graph alone
     * @since 2.5.0
     */
    @com.demcha.compose.document.api.Beta
    default boolean requiresResolvedLayout() {
        return false;
    }

    /**
     * Exports the semantic document graph without running a fixed-layout renderer.
     *
     * @param graph semantic document graph
     * @param context document-wide export configuration
     * @return backend-specific export result
     * @throws Exception if export fails
     */
    R export(DocumentGraph graph, SemanticExportContext context) throws Exception;
}



