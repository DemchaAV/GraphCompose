package com.demcha.compose.layout_core.components.content.text;

import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.system.interfaces.TextMeasurementSystem;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves per-line vertical metrics for block text so layout can react to
 * mixed markdown styles such as headings inside one block.
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
        if (line == null || line.bodies().isEmpty()) {
            return resolveStyleMetrics(entityManager, fallbackStyle);
        }

        double ascent = 0.0;
        double descent = 0.0;
        double leading = 0.0;
        boolean hasMetrics = false;

        for (TextDataBody body : line.bodies()) {
            if (body == null) {
                continue;
            }

            TextStyle style = body.textStyle() == null ? fallbackStyle : body.textStyle();
            if (style == null) {
                continue;
            }

            TextMeasurementSystem.LineMetrics metrics = resolveStyleMetrics(entityManager, style);
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
