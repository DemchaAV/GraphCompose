package com.demcha.compose;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the pure {@link BenchmarkVerdictTool#evaluate} core. These
 * drive synthetic current-speed reports so the verdict classification and the
 * hard-gate {@code regressed} flag are validated deterministically, without
 * running real benchmarks or invoking {@code System.exit}.
 */
class BenchmarkVerdictToolTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final BenchmarkVerdictTool.Thresholds GATE =
            new BenchmarkVerdictTool.Thresholds(10.0, 15.0, true);

    @Test
    void flagsAverageLatencyRegressionBeyondBand() throws Exception {
        JsonNode baseline = report(scenario("invoice-template", 10.0, 10.0, 30.0, 100.0));
        JsonNode candidate = report(scenario("invoice-template", 12.0, 11.0, 28.0, 100.0)); // +20% avg

        BenchmarkVerdictTool.VerdictReport report =
                BenchmarkVerdictTool.evaluate("base.json", "cand.json", baseline, candidate, GATE);

        assertThat(report.regressed()).isTrue();
        assertThat(report.overallVerdict()).isEqualTo("REGRESSED");
        assertThat(report.scenarios()).singleElement()
                .satisfies(row -> assertThat(row.verdict()).isEqualTo("REGRESSED"));
    }

    @Test
    void peakHeapOverBandIsAdvisoryNotGated() throws Exception {
        JsonNode baseline = report(scenario("cv-template", 10.0, 10.0, 40.0, 100.0));
        JsonNode candidate = report(scenario("cv-template", 10.3, 10.0, 40.0, 120.0)); // +3% avg, +20% heap

        BenchmarkVerdictTool.VerdictReport report =
                BenchmarkVerdictTool.evaluate("base.json", "cand.json", baseline, candidate, GATE);

        // Heap over band must NOT fail the gate — peakHeapMb is advisory only
        // (GC-timing noisy). The hard gate metric is average latency.
        assertThat(report.regressed()).isFalse();
        assertThat(report.scenarios().get(0).verdict()).isEqualTo("NEUTRAL");
        assertThat(report.scenarios().get(0).heapAdvisory()).isTrue();
    }

    @Test
    void marksClearSpeedupAsImproved() throws Exception {
        JsonNode baseline = report(scenario("proposal-template", 10.0, 12.0, 28.0, 150.0));
        JsonNode candidate = report(scenario("proposal-template", 8.0, 9.0, 36.0, 150.0)); // -20% avg

        BenchmarkVerdictTool.VerdictReport report =
                BenchmarkVerdictTool.evaluate("base.json", "cand.json", baseline, candidate, GATE);

        assertThat(report.regressed()).isFalse();
        assertThat(report.overallVerdict()).isEqualTo("IMPROVED");
        assertThat(report.scenarios().get(0).verdict()).isEqualTo("IMPROVED");
    }

    @Test
    void treatsWithinBandChangesAsNeutral() throws Exception {
        JsonNode baseline = report(scenario("engine-simple", 5.0, 6.0, 170.0, 40.0));
        JsonNode candidate = report(scenario("engine-simple", 5.2, 6.1, 168.0, 43.0)); // +4% avg, +7.5% heap

        BenchmarkVerdictTool.VerdictReport report =
                BenchmarkVerdictTool.evaluate("base.json", "cand.json", baseline, candidate, GATE);

        assertThat(report.regressed()).isFalse();
        assertThat(report.overallVerdict()).isEqualTo("NEUTRAL");
        assertThat(report.scenarios().get(0).verdict()).isEqualTo("NEUTRAL");
    }

    @Test
    void overallIsRegressedWhenAnyScenarioRegresses() throws Exception {
        JsonNode baseline = report(
                scenario("engine-simple", 5.0, 6.0, 170.0, 40.0),
                scenario("invoice-template", 10.0, 11.0, 28.0, 100.0));
        JsonNode candidate = report(
                scenario("engine-simple", 5.1, 6.1, 168.0, 41.0),   // neutral
                scenario("invoice-template", 13.0, 14.0, 22.0, 100.0)); // +30% avg -> regressed

        BenchmarkVerdictTool.VerdictReport report =
                BenchmarkVerdictTool.evaluate("base.json", "cand.json", baseline, candidate, GATE);

        assertThat(report.regressed()).isTrue();
        assertThat(report.overallVerdict()).isEqualTo("REGRESSED");
    }

    @Test
    void reportsMissingScenariosWithoutGating() throws Exception {
        JsonNode baseline = report(
                scenario("engine-simple", 5.0, 6.0, 170.0, 40.0),
                scenario("invoice-template", 10.0, 11.0, 28.0, 100.0));
        JsonNode candidate = report(scenario("engine-simple", 5.1, 6.1, 168.0, 41.0)); // invoice dropped

        BenchmarkVerdictTool.VerdictReport report =
                BenchmarkVerdictTool.evaluate("base.json", "cand.json", baseline, candidate, GATE);

        assertThat(report.missingScenarios()).containsExactly("invoice-template");
        assertThat(report.scenarios()).hasSize(1);
        assertThat(report.regressed()).isFalse();
    }

    @Test
    void regressedFlagReflectsStateIndependentOfGateFlag() throws Exception {
        JsonNode baseline = report(scenario("invoice-template", 10.0, 10.0, 30.0, 100.0));
        JsonNode candidate = report(scenario("invoice-template", 12.0, 11.0, 28.0, 100.0)); // +20% avg

        BenchmarkVerdictTool.Thresholds gateOff = new BenchmarkVerdictTool.Thresholds(10.0, 15.0, false);
        BenchmarkVerdictTool.VerdictReport report =
                BenchmarkVerdictTool.evaluate("base.json", "cand.json", baseline, candidate, gateOff);

        // The state is still "regressed"; only the build-failing decision (exit code) is gated.
        assertThat(report.regressed()).isTrue();
        assertThat(report.gateEnabled()).isFalse();
    }

    private static JsonNode report(String... latencyRows) throws Exception {
        String latency = String.join(",", latencyRows);
        String json = """
                {
                  "timestamp": "2026-06-08 12:00:00",
                  "profile": "full",
                  "latency": [%s],
                  "throughput": []
                }
                """.formatted(latency);
        return JSON.readTree(json);
    }

    private static String scenario(String name, double avgMs, double p95Ms, double docsPerSec, double peakHeapMb) {
        return """
                {
                  "scenario": "%s",
                  "description": "%s",
                  "avgMillis": %s,
                  "p50Millis": %s,
                  "p95Millis": %s,
                  "maxMillis": %s,
                  "docsPerSecond": %s,
                  "avgKilobytes": 1.0,
                  "peakHeapMb": %s
                }
                """.formatted(name, name, avgMs, avgMs, p95Ms, p95Ms, docsPerSec, peakHeapMb);
    }
}
