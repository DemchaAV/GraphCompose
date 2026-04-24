package com.demcha.compose.document.layout;

import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;

/**
 * Shared fragment emission context passed to node definitions.
 */
public interface FragmentContext {
    FontLibrary fonts();

    TextMeasurementSystem textMeasurement();

    LayoutCanvas canvas();

    default boolean markdownEnabled() {
        return false;
    }
}


