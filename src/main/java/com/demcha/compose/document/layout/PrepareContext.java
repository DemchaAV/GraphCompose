package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.DocumentNode;

import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;

/**
 * Shared prepare context passed to node definitions.
 */
public interface PrepareContext {
    /**
     * Prepares a semantic node for later layout under the supplied constraints.
     *
     * @param node semantic node
     * @param constraints available layout constraints
     * @param <E> semantic node type
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
}



