package com.demcha.compose.engine.core;

import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Unit coverage for the dedicated text-measurement service slot on
 * {@link SystemRegistry}. This seam is what lets {@code TextMeasurementSystem}
 * stay decoupled from {@link SystemECS}: the measurement system is a service
 * provider exposed on demand, not a {@code process()}-driven ECS system, so it is
 * held out-of-band from the {@code systems} map.
 */
class SystemRegistryTest {

    @Test
    void textMeasurementIsEmptyWhenNoneRegistered() {
        assertThat(new SystemRegistry().textMeasurement()).isEmpty();
    }

    @Test
    void registerTextMeasurementExposesTheSameInstance() {
        SystemRegistry registry = new SystemRegistry();
        TextMeasurementSystem service = new StubMeasurement();

        registry.registerTextMeasurement(service);

        assertThat(registry.textMeasurement()).containsSame(service);
    }

    @Test
    void registerTextMeasurementRejectsNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> new SystemRegistry().registerTextMeasurement(null))
                .withMessageContaining("textMeasurement");
    }

    @Test
    void measurementServiceIsNotEnrolledAsAProcessDrivenSystem() {
        SystemRegistry registry = new SystemRegistry();

        registry.registerTextMeasurement(new StubMeasurement());

        // It must stay out of the SystemECS map so the processSystems() loop never
        // calls process() on it — that is the whole point of the dedicated slot.
        assertThat(registry.getStream().toList()).isEmpty();
    }

    private static final class StubMeasurement implements TextMeasurementSystem {
        @Override
        public ContentSize measure(TextStyle style, String text) {
            return new ContentSize(0.0, 0.0);
        }

        @Override
        public LineMetrics lineMetrics(TextStyle style) {
            return new LineMetrics(0.0, 0.0, 0.0);
        }
    }
}
