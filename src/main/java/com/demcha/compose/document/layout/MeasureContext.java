package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.DocumentNode;

import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;

/**
 * Shared measurement context passed to node definitions.
 */
public interface MeasureContext {
    /**
     * Measures a semantic node under the supplied constraints.
     *
     * @param node semantic node
     * @param constraints available layout constraints
     * @return measured box size
     */
    MeasureResult measure(DocumentNode node, BoxConstraints constraints);

    /**
     * Returns the font library for this measurement pass.
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
}



