package com.demcha.compose.v2;

import com.demcha.compose.font_library.FontLibrary;
import com.demcha.compose.layout_core.system.interfaces.TextMeasurementSystem;

/**
 * Shared prepare context passed to node definitions.
 */
public interface PrepareContext {
    <E extends DocumentNode> PreparedNode<E> prepare(E node, BoxConstraints constraints);

    FontLibrary fonts();

    TextMeasurementSystem textMeasurement();

    LayoutCanvas canvas();
}
