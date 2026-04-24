package com.demcha.compose.document.layout;

import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;

/**
 * Shared fragment emission context passed to node definitions.
 */
public interface FragmentContext {
    /**
     * Returns the backend font library available during fragment creation.
     *
     * @return font library
     */
    FontLibrary fonts();

    /**
     * Returns the text measurement service for this layout pass.
     *
     * @return text measurement service
     */
    TextMeasurementSystem textMeasurement();

    /**
     * Returns page and margin information for the current document.
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


