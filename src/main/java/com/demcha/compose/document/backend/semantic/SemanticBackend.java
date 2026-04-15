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
     * Exports the semantic document graph without running a fixed-layout renderer.
     *
     * @param graph semantic document graph
     * @param context document-wide export configuration
     * @return backend-specific export result
     * @throws Exception if export fails
     */
    R export(DocumentGraph graph, SemanticExportContext context) throws Exception;
}



