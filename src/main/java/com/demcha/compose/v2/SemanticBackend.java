package com.demcha.compose.v2;

/**
 * Backend that consumes the semantic v2 document graph directly.
 *
 * @param <R> export result type
 */
public interface SemanticBackend<R> {
    String name();

    R export(DocumentGraph graph, SemanticExportContext context) throws Exception;
}
