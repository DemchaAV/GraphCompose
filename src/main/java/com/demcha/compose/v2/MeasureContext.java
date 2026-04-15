package com.demcha.compose.v2;

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
