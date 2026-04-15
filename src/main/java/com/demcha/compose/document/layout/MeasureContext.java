package com.demcha.compose.document.layout;

import com.demcha.compose.document.model.node.DocumentNode;

import com.demcha.compose.font_library.FontLibrary;
import com.demcha.compose.layout_core.system.interfaces.TextMeasurementSystem;

/**
 * Shared measurement context passed to node definitions.
 */
public interface MeasureContext {
    MeasureResult measure(DocumentNode node, BoxConstraints constraints);

    FontLibrary fonts();

    TextMeasurementSystem textMeasurement();

    LayoutCanvas canvas();
}



