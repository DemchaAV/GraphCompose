package com.demcha.compose.engine.components.content.text;

import com.demcha.compose.engine.core.EntityManager;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves per-line vertical metrics for block text.
 *
 * <p>The helper prefers line-local cached metrics when they are already present
 * and only falls back to the active {@link TextMeasurementSystem} for
 * uncached/manual lines. That keeps layout and page-breaking logic backend-
 * neutral while avoiding repeated measurement work during ordinary block-text
 * processing.</p>
 */
public final class BlockTextLineMetrics {

    private BlockTextLineMetrics() {
    }

    /**
     * Resolves line metrics from a single {@link TextStyle} using the active
     * measurement system.
     *
     * @param entityManager entity manager providing the measurement system
     * @param style         text style to measure, defaults to {@link TextStyle#DEFAULT_STYLE}
     * @return resolved line metrics (ascent, descent, leading)
     */
    public static TextMeasurementSystem.LineMetrics resolveStyleMetrics(EntityManager entityManager, TextStyle style) {
        TextStyle safeStyle = style == null ? TextStyle.DEFAULT_STYLE : style;
        return measurementSystem(entityManager).lineMetrics(safeStyle);
    }

    /**
     * Resolves line metrics for a single line, preferring cached values when present.
     *
     * @param entityManager entity manager providing the measurement system
     * @param line          line to resolve metrics for, may be {@code null}
     * @param fallbackStyle style to fall back to when the line or its bodies lack style info
     * @return resolved line metrics
     */
    public static TextMeasurementSystem.LineMetrics resolveLineMetrics(EntityManager entityManager,
                                                                       LineTextData line,
                                                                       TextStyle fallbackStyle) {
        if (line != null && line.hasCachedLineMetrics()) {
            return line.lineMetrics();
        }

        if (line == null) {
            return resolveStyleMetrics(entityManager, fallbackStyle);
        }

        return resolveBodiesMetrics(entityManager, line.bodies(), fallbackStyle);
    }

    /**
     * Resolves mixed-style line metrics from a list of text bodies by taking the
     * maximum ascent, descent, and leading across all non-null body styles.
     *
     * <p>This method always measures from scratch and does not use any cache.
     * It is called by {@link com.demcha.compose.engine.components.components_builders.BlockTextBuilder}
     * at line-creation time to populate the per-line cache.</p>
     *
     * @param entityManager entity manager providing the measurement system
     * @param bodies        text bodies to measure
     * @param fallbackStyle style to use when a body has no style
     * @return resolved line metrics
     */
    public static TextMeasurementSystem.LineMetrics resolveBodiesMetrics(EntityManager entityManager,
                                                                         List<TextDataBody> bodies,
                                                                         TextStyle fallbackStyle) {
        if (bodies == null || bodies.isEmpty()) {
            return resolveStyleMetrics(entityManager, fallbackStyle);
        }

        TextMeasurementSystem measurementSystem = measurementSystem(entityManager);
        double ascent = 0.0;
        double descent = 0.0;
        double leading = 0.0;
        boolean hasMetrics = false;

        for (TextDataBody body : bodies) {
            if (body == null) {
                continue;
            }

            TextStyle style = body.textStyle() == null ? fallbackStyle : body.textStyle();
            if (style == null) {
                continue;
            }

            TextMeasurementSystem.LineMetrics metrics = measurementSystem.lineMetrics(style);
            ascent = Math.max(ascent, metrics.ascent());
            descent = Math.max(descent, metrics.descent());
            leading = Math.max(leading, metrics.leading());
            hasMetrics = true;
        }

        if (!hasMetrics) {
            return resolveStyleMetrics(entityManager, fallbackStyle);
        }

        return new TextMeasurementSystem.LineMetrics(ascent, descent, leading);
    }

    /**
     * Batch-resolves line metrics for a list of lines.
     *
     * @param entityManager entity manager providing the measurement system
     * @param lines         lines to measure
     * @param fallbackStyle style to fall back to for lines without cached metrics
     * @return list of metrics in the same order as the input lines
     */
    public static List<TextMeasurementSystem.LineMetrics> resolveLineMetrics(EntityManager entityManager,
                                                                              List<LineTextData> lines,
                                                                              TextStyle fallbackStyle) {
        List<TextMeasurementSystem.LineMetrics> result = new ArrayList<>(lines.size());
        for (LineTextData line : lines) {
            result.add(resolveLineMetrics(entityManager, line, fallbackStyle));
        }
        return result;
    }

    /**
     * Computes the vertical gap between two adjacent lines, taking into account
     * outer gap contributions from both lines relative to the base metrics.
     *
     * @param previous metrics of the line above
     * @param next     metrics of the line below
     * @param base     baseline metrics (usually from the block's default style)
     * @param spacing  additional line spacing configured on the block
     * @return total inter-line gap in layout units
     */
    public static double interLineGap(TextMeasurementSystem.LineMetrics previous,
                                      TextMeasurementSystem.LineMetrics next,
                                      TextMeasurementSystem.LineMetrics base,
                                      double spacing) {
        return spacing + previous.outerGap(base) / 2.0 + next.outerGap(base) / 2.0;
    }

    private static TextMeasurementSystem measurementSystem(EntityManager entityManager) {
        return entityManager.getSystems()
                .getSystem(TextMeasurementSystem.class)
                .orElseThrow(() -> new IllegalStateException("TextMeasurementSystem is required to resolve block text metrics."));
    }
}
