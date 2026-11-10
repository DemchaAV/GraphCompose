package com.demcha.compose.document.backend.semantic;

import com.demcha.compose.document.api.Beta;
import com.demcha.compose.document.layout.DocumentGraph;

import java.util.Objects;

/**
 * One section of a multi-section semantic export: the section's authored graph and the
 * context it would be exported with on its own — its canvas, fonts, output options and, for
 * a backend that asks for it, its resolved layout.
 *
 * <p>A {@link com.demcha.compose.document.api.MultiSectionDocument} is several sessions, each
 * with its own page size, margins and chrome. A fixed-layout backend concatenates their pages;
 * a semantic backend receives one of these per section and writes them into one document, a
 * section of the output per section of the input. See
 * {@link SemanticBackend#exportSections(java.util.List)}.</p>
 *
 * <p><b>Experimental</b> ({@code @Beta}) — see {@code docs/api-stability.md}.</p>
 *
 * @param graph   the section's authored document graph
 * @param context the section's export context
 * @since 2.5.0
 */
@Beta
public record SemanticSection(DocumentGraph graph, SemanticExportContext context) {

    /**
     * Validates that both parts are present.
     */
    public SemanticSection {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(context, "context");
    }
}
