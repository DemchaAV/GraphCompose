package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.DocumentNode;

import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;

/**
 * Shared measurement context passed to node definitions.
 */
public interface MeasureContext {
    MeasureResult measure(DocumentNode node, BoxConstraints constraints);

    FontLibrary fonts();

    TextMeasurementSystem textMeasurement();

    LayoutCanvas canvas();
}



