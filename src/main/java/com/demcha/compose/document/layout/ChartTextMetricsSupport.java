package com.demcha.compose.document.layout;

import com.demcha.compose.document.chart.ChartTextMetrics;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;

import java.util.Objects;

/**
 * Bridges the public {@link ChartTextMetrics} seam to the engine
 * {@link TextMeasurementSystem}.
 *
 * <p>Lives in {@code document.layout} so it can reach the package-private
 * {@link DocumentNodeAdapters#toTextStyle(DocumentTextStyle)} style adapter; the
 * chart resolver only ever sees the narrow {@link ChartTextMetrics} interface and
 * therefore stays a pure function decoupled from the engine.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public final class ChartTextMetricsSupport implements ChartTextMetrics {

    private final TextMeasurementSystem measurement;

    /**
     * Wraps a measurement system.
     *
     * @param measurement engine text measurement service
     */
    public ChartTextMetricsSupport(TextMeasurementSystem measurement) {
        this.measurement = Objects.requireNonNull(measurement, "measurement");
    }

    @Override
    public double width(DocumentTextStyle style, String text) {
        TextStyle engineStyle = DocumentNodeAdapters.toTextStyle(style);
        return measurement.measure(engineStyle, text == null ? "" : text).width();
    }

    @Override
    public double lineHeight(DocumentTextStyle style) {
        TextStyle engineStyle = DocumentNodeAdapters.toTextStyle(style);
        return measurement.lineMetrics(engineStyle).lineHeight();
    }

    @Override
    public double descent(DocumentTextStyle style) {
        TextStyle engineStyle = DocumentNodeAdapters.toTextStyle(style);
        return measurement.lineMetrics(engineStyle).baselineOffsetFromBottom();
    }
}
