package com.demcha.compose.document.layout;

import com.demcha.compose.font_library.FontLibrary;
import com.demcha.compose.layout_core.system.interfaces.TextMeasurementSystem;

/**
 * Shared fragment emission context passed to node definitions.
 */
public interface FragmentContext {
    FontLibrary fonts();

    TextMeasurementSystem textMeasurement();

    LayoutCanvas canvas();
}


