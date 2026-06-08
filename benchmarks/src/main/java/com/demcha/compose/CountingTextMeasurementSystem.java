package com.demcha.compose;

import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A {@link TextMeasurementSystem} decorator that forwards every call to a real
 * delegate while counting how the layout engine asks for text measurements.
 *
 * <p>It exists to make the algorithmic findings of the performance audit
 * (F1 greedy wrap re-measuring growing prefixes, F2 quadratic long-token
 * breaking, F3 table re-measurement) <em>deterministically</em> observable.
 * Wall-clock timing hides these under JIT/GC noise; measurement-request counts
 * and summed argument characters do not.</p>
 *
 * <p>The decorator records, per pass:</p>
 * <ul>
 *     <li>the number of width-bearing requests ({@code textWidth} + {@code measure})</li>
 *     <li>the number of <em>distinct</em> {@code (style, text)} requests — the
 *         caller-side proxy for how well the delegate's width cache can hit;
 *         a low repeat rate means the layout keeps asking for one-shot strings
 *         (the F1/F2 smell)</li>
 *     <li>the summed and maximum argument length in characters — the proxy for
 *         the {@code O(chars)} work each uncached measurement performs</li>
 *     <li>{@code lineMetrics}/{@code lineHeight} call counts (style-only, no text)</li>
 * </ul>
 *
 * <p>Not thread-safe: drive it from a single layout pass, like the real
 * measurement system.</p>
 *
 * @author Artem Demchyshyn
 */
public final class CountingTextMeasurementSystem implements TextMeasurementSystem {

    private final TextMeasurementSystem delegate;

    private long textWidthCalls;
    private long measureCalls;
    private long lineMetricsCalls;
    private long lineHeightCalls;
    private long summedRequestChars;
    private long maxRequestChars;
    private final Set<RequestKey> distinctRequests = new HashSet<>();

    /**
     * Wraps a real measurement system.
     *
     * @param delegate the measurement system to forward to (e.g. the session's
     *                 {@code FontLibraryTextMeasurementSystem})
     */
    public CountingTextMeasurementSystem(TextMeasurementSystem delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public ContentSize measure(TextStyle style, String text) {
        measureCalls++;
        record(style, text);
        return delegate.measure(style, text);
    }

    @Override
    public double textWidth(TextStyle style, String text) {
        textWidthCalls++;
        record(style, text);
        return delegate.textWidth(style, text);
    }

    @Override
    public LineMetrics lineMetrics(TextStyle style) {
        lineMetricsCalls++;
        return delegate.lineMetrics(style);
    }

    @Override
    public double lineHeight(TextStyle style) {
        lineHeightCalls++;
        return delegate.lineHeight(style);
    }

    @Override
    public void clearCaches() {
        delegate.clearCaches();
    }

    private void record(TextStyle style, String text) {
        String safe = text == null ? "" : text;
        int length = safe.length();
        summedRequestChars += length;
        if (length > maxRequestChars) {
            maxRequestChars = length;
        }
        distinctRequests.add(new RequestKey(style, safe));
    }

    /**
     * Captures the counts accumulated so far.
     *
     * @return an immutable snapshot of the measurement-request counters
     */
    public Counts snapshot() {
        long widthRequests = textWidthCalls + measureCalls;
        long distinct = distinctRequests.size();
        double repeatRatePct = widthRequests == 0
                ? 0.0
                : (1.0 - ((double) distinct / (double) widthRequests)) * 100.0;
        return new Counts(
                textWidthCalls,
                measureCalls,
                widthRequests,
                distinct,
                repeatRatePct,
                summedRequestChars,
                maxRequestChars,
                lineMetricsCalls,
                lineHeightCalls);
    }

    /**
     * Immutable snapshot of measurement-request counters.
     *
     * @param textWidthCalls         direct {@code textWidth(style, text)} calls
     * @param measureCalls           {@code measure(style, text)} calls
     * @param widthRequests          {@code textWidthCalls + measureCalls}
     * @param distinctWidthRequests  distinct {@code (style, text)} requests
     * @param repeatRatePct          {@code (1 - distinct/total) * 100}; higher
     *                               means more cache-friendly (fewer one-shot
     *                               strings)
     * @param summedRequestChars     total characters across all width requests
     * @param maxRequestChars        longest single argument measured
     * @param lineMetricsCalls       {@code lineMetrics(style)} calls
     * @param lineHeightCalls        {@code lineHeight(style)} calls
     */
    public record Counts(long textWidthCalls,
                         long measureCalls,
                         long widthRequests,
                         long distinctWidthRequests,
                         double repeatRatePct,
                         long summedRequestChars,
                         long maxRequestChars,
                         long lineMetricsCalls,
                         long lineHeightCalls) {
    }

    private record RequestKey(TextStyle style, String text) {
    }
}
