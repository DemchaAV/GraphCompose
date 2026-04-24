package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.DocumentNode;

import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;

/**
 * Shared prepare context passed to node definitions.
 */
public interface PrepareContext {
    <E extends DocumentNode> PreparedNode<E> prepare(E node, BoxConstraints constraints);

    FontLibrary fonts();

    TextMeasurementSystem textMeasurement();

    LayoutCanvas canvas();

    default boolean markdownEnabled() {
        return false;
    }
}



