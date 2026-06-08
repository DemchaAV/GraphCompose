package com.demcha.compose;

import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CountingTextMeasurementSystem}. They use a trivial fake
 * delegate (no PDFBox) so the counting/forwarding contract is verified
 * deterministically and fast.
 */
class CountingTextMeasurementSystemTest {

    private static final TextStyle STYLE = TextStyle.DEFAULT_STYLE;

    @Test
    void countsWidthRequestsDistinctKeysAndCharacters() {
        CountingTextMeasurementSystem counter = new CountingTextMeasurementSystem(new FakeMeasurement());

        double abWidth = counter.textWidth(STYLE, "ab");
        counter.textWidth(STYLE, "ab");   // repeat -> same key
        counter.textWidth(STYLE, "abc");
        counter.measure(STYLE, "ab");     // measure shares the "ab" key
        counter.lineMetrics(STYLE);
        counter.lineHeight(STYLE);

        CountingTextMeasurementSystem.Counts counts = counter.snapshot();

        assertThat(abWidth).isEqualTo(2.0); // delegate pass-through (fake width == length)
        assertThat(counts.textWidthCalls()).isEqualTo(3);
        assertThat(counts.measureCalls()).isEqualTo(1);
        assertThat(counts.widthRequests()).isEqualTo(4);
        assertThat(counts.distinctWidthRequests()).isEqualTo(2); // "ab", "abc"
        assertThat(counts.summedRequestChars()).isEqualTo(9);    // 2 + 2 + 3 + 2
        assertThat(counts.maxRequestChars()).isEqualTo(3);
        assertThat(counts.repeatRatePct()).isEqualTo(50.0);      // 1 - 2/4
        assertThat(counts.lineMetricsCalls()).isEqualTo(1);
        assertThat(counts.lineHeightCalls()).isEqualTo(1);
    }

    @Test
    void emptySnapshotHasNoRequests() {
        CountingTextMeasurementSystem counter = new CountingTextMeasurementSystem(new FakeMeasurement());

        CountingTextMeasurementSystem.Counts counts = counter.snapshot();

        assertThat(counts.widthRequests()).isZero();
        assertThat(counts.distinctWidthRequests()).isZero();
        assertThat(counts.repeatRatePct()).isZero();
        assertThat(counts.summedRequestChars()).isZero();
    }

    @Test
    void treatsNullTextAsEmptyWithoutFailing() {
        CountingTextMeasurementSystem counter = new CountingTextMeasurementSystem(new FakeMeasurement());

        counter.textWidth(STYLE, null);

        CountingTextMeasurementSystem.Counts counts = counter.snapshot();
        assertThat(counts.widthRequests()).isEqualTo(1);
        assertThat(counts.summedRequestChars()).isZero();
        assertThat(counts.distinctWidthRequests()).isEqualTo(1);
    }

    /** Minimal delegate: width == text length, fixed line metrics. */
    private static final class FakeMeasurement implements TextMeasurementSystem {
        @Override
        public ContentSize measure(TextStyle style, String text) {
            int length = text == null ? 0 : text.length();
            return new ContentSize(length, 10.0);
        }

        @Override
        public LineMetrics lineMetrics(TextStyle style) {
            return new LineMetrics(8.0, 2.0, 0.0);
        }
    }
}
