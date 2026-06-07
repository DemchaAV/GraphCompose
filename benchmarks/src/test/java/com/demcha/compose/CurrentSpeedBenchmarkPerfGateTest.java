package com.demcha.compose;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CurrentSpeedBenchmark#evaluatePerformanceGate} — the
 * absolute smoke-profile performance gate that actually fails a benchmark run.
 *
 * <p>The gate is driven with synthetic {@code LatencyRow} values so the
 * pass/fail decision is deterministic and independent of real measurement
 * (which varies by machine and would make these tests flaky). engine-simple
 * smoke thresholds are avg {@code 8.0} ms / peak heap {@code 96.0} MB.</p>
 */
class CurrentSpeedBenchmarkPerfGateTest {

    private static final String ENGINE_SIMPLE = "engine-simple";
    private static final String GATE_AVG =
            "graphcompose.benchmark.gate." + ENGINE_SIMPLE + ".maxAvgMillis";
    private static final String GATE_HEAP =
            "graphcompose.benchmark.gate." + ENGINE_SIMPLE + ".maxPeakHeapMb";

    @AfterEach
    void clearGateOverrides() {
        System.clearProperty(GATE_AVG);
        System.clearProperty(GATE_HEAP);
    }

    @Test
    void passesWhenEveryScenarioIsWithinSmokeThresholds() {
        CurrentSpeedBenchmark.PerformanceGateResult result =
                CurrentSpeedBenchmark.evaluatePerformanceGate(
                        CurrentSpeedBenchmark.BenchmarkProfile.SMOKE,
                        List.of(latency(ENGINE_SIMPLE, 1.0, 24.0)));

        assertThat(result.passed()).isTrue();
        assertThat(result.message()).contains("passed");
    }

    @Test
    void failsWhenAverageLatencyExceedsThreshold() {
        CurrentSpeedBenchmark.PerformanceGateResult result =
                CurrentSpeedBenchmark.evaluatePerformanceGate(
                        CurrentSpeedBenchmark.BenchmarkProfile.SMOKE,
                        List.of(latency(ENGINE_SIMPLE, 50.0, 24.0))); // 50 > 8

        assertThat(result.passed()).isFalse();
        assertThat(result.message()).contains(ENGINE_SIMPLE + " avg");
    }

    @Test
    void failsWhenPeakHeapExceedsThreshold() {
        CurrentSpeedBenchmark.PerformanceGateResult result =
                CurrentSpeedBenchmark.evaluatePerformanceGate(
                        CurrentSpeedBenchmark.BenchmarkProfile.SMOKE,
                        List.of(latency(ENGINE_SIMPLE, 1.0, 999.0))); // 999 > 96

        assertThat(result.passed()).isFalse();
        assertThat(result.message()).contains("peak heap");
    }

    @Test
    void reportsEveryFailingScenarioWhenMultipleBreach() {
        CurrentSpeedBenchmark.PerformanceGateResult result =
                CurrentSpeedBenchmark.evaluatePerformanceGate(
                        CurrentSpeedBenchmark.BenchmarkProfile.SMOKE,
                        List.of(
                                latency(ENGINE_SIMPLE, 50.0, 24.0),      // avg breach
                                latency("cv-template", 1.0, 24.0),       // ok (avg 25 / heap 192)
                                latency("invoice-template", 1.0, 999.0)));// heap breach (heap 384)

        assertThat(result.passed()).isFalse();
        assertThat(result.message())
                .contains(ENGINE_SIMPLE + " avg")
                .contains("invoice-template peak heap")
                .doesNotContain("cv-template");
    }

    @Test
    void skipsGateForNonSmokeProfiles() {
        CurrentSpeedBenchmark.PerformanceGateResult result =
                CurrentSpeedBenchmark.evaluatePerformanceGate(
                        CurrentSpeedBenchmark.BenchmarkProfile.FULL,
                        List.of(latency(ENGINE_SIMPLE, 9999.0, 9999.0)));

        assertThat(result.passed()).isTrue();
        assertThat(result.message()).contains("skipped");
    }

    @Test
    void ignoresScenariosWithoutAConfiguredThreshold() {
        CurrentSpeedBenchmark.PerformanceGateResult result =
                CurrentSpeedBenchmark.evaluatePerformanceGate(
                        CurrentSpeedBenchmark.BenchmarkProfile.SMOKE,
                        List.of(latency("scenario-without-threshold", 9999.0, 9999.0)));

        assertThat(result.passed()).isTrue();
        assertThat(result.message()).contains("passed");
    }

    @Test
    void honorsSystemPropertyThresholdOverride() {
        // Tighten engine-simple to 2.0 ms: a 5.0 ms row passes the default
        // 8.0 ms threshold but must fail under the override.
        System.setProperty(GATE_AVG, "2.0");

        CurrentSpeedBenchmark.PerformanceGateResult result =
                CurrentSpeedBenchmark.evaluatePerformanceGate(
                        CurrentSpeedBenchmark.BenchmarkProfile.SMOKE,
                        List.of(latency(ENGINE_SIMPLE, 5.0, 24.0)));

        assertThat(result.passed()).isFalse();
        assertThat(result.message()).contains(ENGINE_SIMPLE + " avg");
    }

    /**
     * Builds a latency row where only {@code scenario}, {@code avgMillis} and
     * {@code peakHeapMb} matter to the gate; the rest are filler.
     */
    private static CurrentSpeedBenchmark.LatencyRow latency(String scenario, double avgMillis, double peakHeapMb) {
        return new CurrentSpeedBenchmark.LatencyRow(
                scenario,
                "test row",
                avgMillis, // avgMillis
                0.0,       // p50Millis
                0.0,       // p95Millis
                0.0,       // maxMillis
                0.0,       // docsPerSecond
                0.0,       // avgKilobytes
                peakHeapMb // peakHeapMb
        );
    }
}
