package com.demcha.compose.layout_core.system.interfaces;

import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.core.EntityManager;

/**
 * Engine-level text measurement contract used by builders and layout helpers.
 * <p>
 * Implementations bridge the shared engine to backend-specific font metrics
 * without forcing engine code to reach through the active rendering system.
 * </p>
 */
public interface TextMeasurementSystem extends SystemECS {

    /**
     * Measures the dimensions (width and height) of a given text string
     * when rendered with a specific style.
     *
     * @param style The {@link TextStyle} defining the font, size, and decoration.
     * @param text  The actual text string to be measured.
     * @return A {@link ContentSize} object representing the calculated width and height of the text.
     */
    ContentSize measure(TextStyle style, String text);

    /**
     * Calculates the baseline-to-baseline line height for a specific text style.
     * <p>
     * This height is useful for vertical layout calculations, determining line spacing,
     * and calculating bounding boxes for multiline text elements.
     *
     * @param style The {@link TextStyle} defining the font and its size.
     * @return The overall vertical space required for a single line of text in this style.
     */
    double lineHeight(TextStyle style);

    /**
     * A no-op implementation of the standard ECS system processing method.
     * <p>
     * Measurement systems act as service providers that expose metrics to builders
     * and layout calculations on demand. They do not actively mutate or participate
     * in the runtime processing of entities within the main game loop / pipeline.
     *
     * @param entityManager The active entity manager. Ignored by this system.
     */
    @Override
    default void process(EntityManager entityManager) {
        // Measurement systems expose services to builders/layout helpers and do
        // not participate in the runtime processing pipeline.
    }
}
