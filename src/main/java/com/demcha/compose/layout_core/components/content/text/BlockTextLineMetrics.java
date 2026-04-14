package com.demcha.compose.layout_core.components.content.text;

import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.system.interfaces.TextMeasurementSystem;

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

    public static TextMeasurementSystem.LineMetrics resolveStyleMetrics(EntityManager entityManager, TextStyle style) {
        TextStyle safeStyle = style == null ? TextStyle.DEFAULT_STYLE : style;
        return measurementSystem(entityManager).lineMetrics(safeStyle);
    }

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

    public static List<TextMeasurementSystem.LineMetrics> resolveLineMetrics(EntityManager entityManager,
                                                                              List<LineTextData> lines,
                                                                              TextStyle fallbackStyle) {
        List<TextMeasurementSystem.LineMetrics> result = new ArrayList<>(lines.size());
        for (LineTextData line : lines) {
            result.add(resolveLineMetrics(entityManager, line, fallbackStyle));
        }
        return result;
    }

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
