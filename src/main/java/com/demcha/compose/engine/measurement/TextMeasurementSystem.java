package com.demcha.compose.engine.measurement;

import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.core.EntityManager;
import com.demcha.compose.engine.core.SystemECS;

/**
 * Engine-level text measurement contract used by builders and layout helpers.
 * <p>
 * Implementations bridge the shared engine to backend-specific font metrics
 * without forcing engine code to reach through the active rendering system.
 * </p>
 */
public interface TextMeasurementSystem extends SystemECS {

    /**
     * Vertical metrics for one resolved text style.
     *
     * @param ascent   distance from baseline to glyph top
     * @param descent  distance from baseline to glyph bottom
     * @param leading  extra line-leading applied by the backend font metrics
     */
    record LineMetrics(double ascent, double descent, double leading) {
        public double lineHeight() {
            return ascent + descent + leading;
        }

        public double baselineOffsetFromBottom() {
            return descent;
        }

        public double outerGap(LineMetrics base) {
            if (base == null) {
                return 0.0;
            }
            return Math.max(0.0, lineHeight() - base.lineHeight()) / 2.0;
        }
    }

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
     * Measures only the width of a text run while preserving the backend-specific
     * line metrics contract for the supplied style.
     *
     * @param style the text style used for measurement
     * @param text  the text run to measure
     * @return the measured width in document units
     */
    default double textWidth(TextStyle style, String text) {
        return measure(style, text).width();
    }

    /**
     * Resolves detailed line metrics for the supplied text style.
     *
     * @param style the style whose metrics should be resolved
     * @return backend-aware line metrics including ascent, descent, and leading
     */
    LineMetrics lineMetrics(TextStyle style);

    /**
     * Clears request/session-local measurement caches.
     *
     * <p>Implementations that keep shared immutable font metrics may keep those
     * values. Implementations must not keep user document text after this call.</p>
     */
    default void clearCaches() {
        // Stateless measurement systems do not need cleanup.
    }

    /**
     * Calculates the baseline-to-baseline line height for a specific text style.
     * <p>
     * This height is useful for vertical layout calculations, determining line spacing,
     * and calculating bounding boxes for multiline text elements.
     *
     * @param style The {@link TextStyle} defining the font and its size.
     * @return The overall vertical space required for a single line of text in this style.
     */
    default double lineHeight(TextStyle style) {
        return lineMetrics(style).lineHeight();
    }

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
