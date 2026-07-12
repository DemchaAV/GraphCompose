package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;
import com.demcha.compose.font.FontLibrary;

import java.util.OptionalInt;

/**
 * Shared prepare context passed to node definitions.
 */
public interface PrepareContext {
    /**
     * Prepares a semantic node for later layout under the supplied constraints.
     *
     * @param node        semantic node
     * @param constraints available layout constraints
     * @param <E>         semantic node type
     * @return prepared node with reusable measurement/layout payload
     */
    <E extends DocumentNode> PreparedNode<E> prepare(E node, BoxConstraints constraints);

    /**
     * Returns the font library for this prepare pass.
     *
     * @return font library
     */
    FontLibrary fonts();

    /**
     * Returns the text measurement service for this pass.
     *
     * @return text measurement service
     */
    TextMeasurementSystem textMeasurement();

    /**
     * Returns page and margin information for this pass.
     *
     * @return layout canvas
     */
    LayoutCanvas canvas();

    /**
     * Indicates whether paragraph text should be parsed as markdown.
     *
     * @return true when markdown parsing is enabled
     */
    default boolean markdownEnabled() {
        return false;
    }

    /**
     * Resolves the 1-based page number a named {@code anchor(...)} lands on, when
     * the pass has page numbers available (the second pass of a page-reference /
     * table-of-contents resolve). Empty on the first pass and for unknown anchors,
     * so a page-reference node renders a placeholder until it is resolved.
     *
     * @param anchor anchor name to resolve
     * @return the 1-based page number, or empty when unresolved
     */
    default OptionalInt resolvedPage(String anchor) {
        return OptionalInt.empty();
    }

    /**
     * Per-page geometry resolver for documents with per-page margin overrides, or
     * {@code null} when the document uses a single document-wide margin (the common
     * case, laid out exactly as before).
     *
     * @return the page-geometry resolver, or {@code null}
     */
    default PageGeometry pageGeometry() {
        return null;
    }

    /**
     * The page a node begins on, carried over from the previous layout pass so this
     * pass can measure the node at that page's content width (the per-page-margin
     * fixed point). Returns {@code fallback} on the first pass and for documents
     * without per-page margins.
     *
     * @param path     stable semantic path of the node
     * @param fallback value to use when no assignment is carried over
     * @return the 0-based start page assigned in the previous pass, or {@code fallback}
     */
    default int assignedStartPage(String path, int fallback) {
        return fallback;
    }
}



