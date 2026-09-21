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
     * That matters beyond the wasted work: compiling a layout measures text, and
     * measurement needs a font runtime the core does not ship — so resolving it for every
     * semantic export would make a render backend a hard requirement of exports that do
     * not render.</p>
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



