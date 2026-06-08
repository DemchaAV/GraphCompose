package com.demcha.compose;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Compares a candidate {@code current-speed} benchmark report against a
 * committed baseline and emits a per-scenario verdict
 * ({@code IMPROVED} / {@code NEUTRAL} / {@code REGRESSED}).
 *
 * <p>This is the regression gate of the per-change performance workflow
 * described in {@code docs/operations/perf-change-workflow.md}. Unlike
 * {@link BenchmarkDiffTool}, which only prints signed deltas between two
 * arbitrary runs, this tool classifies each delta against a noise band and
 * fails the build (non-zero exit) when any scenario regresses beyond the band
 * on a <em>gate metric</em> (average latency or peak heap). It is meant to be
 * pointed at a stable, committed baseline (see {@code baselines/}) rather than
 * at the previous ephemeral run under {@code target/}.</p>
 *
 * <p>Usage:</p>
 * <ul>
 *     <li>{@code java ... BenchmarkVerdictTool <baseline.json> <candidate.json>}</li>
 * </ul>
 *
 * <p>Both reports must share the same {@code current-speed} profile
 * ({@code smoke} or {@code full}); a {@code smoke} report and a {@code full}
 * report are different experiments and are rejected.</p>
 *
 * <p>Thresholds and gate behaviour are configurable via system properties
 * (all percentages):</p>
 * <ul>
 *     <li>{@code -Dgraphcompose.benchmark.verdict.avgBandPct} (default {@code 10.0})</li>
 *     <li>{@code -Dgraphcompose.benchmark.verdict.heapBandPct} (default {@code 15.0})</li>
 *     <li>{@code -Dgraphcompose.benchmark.verdict.gate} (default {@code true})</li>
 * </ul>
 *
 * <p>Exit codes: {@code 0} when the gate passes (or is disabled), {@code 1}
 * when the gate is enabled and at least one scenario regressed, {@code 2} on
 * usage or profile-compatibility errors.</p>
 *
 * @author Artem Demchyshyn
 */
public final class BenchmarkVerdictTool {

    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String AVG_BAND_PROPERTY = "graphcompose.benchmark.verdict.avgBandPct";
    private static final String HEAP_BAND_PROPERTY = "graphcompose.benchmark.verdict.heapBandPct";
    private static final String GATE_PROPERTY = "graphcompose.benchmark.verdict.gate";

    private static final double DEFAULT_AVG_BAND_PCT = 10.0;
    private static final double DEFAULT_HEAP_BAND_PCT = 15.0;

    private BenchmarkVerdictTool() {
    }

    /**
     * CLI entry point. Reads the baseline and candidate reports, prints the
     * verdict table, writes JSON/CSV verdict artifacts under
     * {@code target/benchmarks/verdicts/current-speed/}, and exits non-zero
     * when the regression gate is enabled and at least one scenario regressed.
     *
     * @param args {@code <baseline.json> <candidate.json>}
     * @throws Exception if a report cannot be read or written
     */
    public static void main(String[] args) throws Exception {
        BenchmarkSupport.configureQuietLogging();
        if (args.length != 2) {
            System.err.println("""
                    Usage:
                      java ... com.demcha.compose.BenchmarkVerdictTool <baseline.json> <candidate.json>
                    """);
            System.exit(2);
            return;
        }

        Path baselinePath = Path.of(args[0]);
        Path candidatePath = Path.of(args[1]);
        JsonNode baseline = JSON.readTree(Files.readAllBytes(baselinePath));
        JsonNode candidate = JSON.readTree(Files.readAllBytes(candidatePath));

        if (!isCurrentSpeed(baseline) || !isCurrentSpeed(candidate)) {
            System.err.println("BenchmarkVerdictTool only supports current-speed reports (latency + throughput).");
            System.exit(2);
            return;
        }

        String baselineProfile = baseline.path("profile").asText("");
        String candidateProfile = candidate.path("profile").asText("");
        if (!baselineProfile.equals(candidateProfile)) {
            System.err.println("Profiles do not match: baseline='" + baselineProfile
                    + "', candidate='" + candidateProfile + "'. Compare runs from the same profile only.");
            System.exit(2);
            return;
        }

        Thresholds thresholds = Thresholds.fromSystemProperties();
        VerdictReport report = evaluate(baselinePath.toString(), candidatePath.toString(), baseline, candidate, thresholds);

        print(report);
        write(report);

        if (thresholds.gateEnabled() && report.regressed()) {
            System.out.println();
            System.out.println("PERFORMANCE GATE FAILED: at least one scenario regressed beyond the noise band.");
            System.exit(1);
        }
    }

    /**
     * Pure, side-effect-free evaluation core used by both {@link #main(String[])}
     * and the unit test. Computes the per-scenario verdict for every scenario
     * present in both reports and the overall verdict.
     *
     * @param baselinePath  display path of the baseline report
     * @param candidatePath display path of the candidate report
     * @param baseline      parsed baseline current-speed report
     * @param candidate     parsed candidate current-speed report
     * @param thresholds    noise bands and gate flag
     * @return the computed verdict report
     */
    static VerdictReport evaluate(String baselinePath,
                                  String candidatePath,
                                  JsonNode baseline,
                                  JsonNode candidate,
                                  Thresholds thresholds) {
        Map<String, JsonNode> baselineByScenario = indexBy(baseline.path("latency"));
        Map<String, JsonNode> candidateByScenario = indexBy(candidate.path("latency"));

        List<ScenarioVerdict> scenarios = new ArrayList<>();
        List<String> missingScenarios = new ArrayList<>();
        boolean anyRegressed = false;
        boolean anyImproved = false;

        for (Map.Entry<String, JsonNode> entry : baselineByScenario.entrySet()) {
            String scenario = entry.getKey();
            JsonNode before = entry.getValue();
            JsonNode after = candidateByScenario.get(scenario);
            if (after == null) {
                missingScenarios.add(scenario);
                continue;
            }

            double baselineAvg = before.path("avgMillis").asDouble();
            double candidateAvg = after.path("avgMillis").asDouble();
            double avgDeltaPct = percentDelta(baselineAvg, candidateAvg);
            double p95DeltaPct = percentDelta(before.path("p95Millis").asDouble(), after.path("p95Millis").asDouble());
            double docsDeltaPct = percentDelta(before.path("docsPerSecond").asDouble(), after.path("docsPerSecond").asDouble());
            double baselineHeap = before.path("peakHeapMb").asDouble();
            double candidateHeap = after.path("peakHeapMb").asDouble();
            double heapDeltaPct = percentDelta(baselineHeap, candidateHeap);

            // Gate metrics: average latency and peak heap (both lower-is-better).
            Verdict verdict;
            if (avgDeltaPct > thresholds.avgBandPct() || heapDeltaPct > thresholds.heapBandPct()) {
                verdict = Verdict.REGRESSED;
                anyRegressed = true;
            } else if (avgDeltaPct < -thresholds.avgBandPct()) {
                verdict = Verdict.IMPROVED;
                anyImproved = true;
            } else {
                verdict = Verdict.NEUTRAL;
            }

            scenarios.add(new ScenarioVerdict(
                    scenario,
                    before.path("description").asText(after.path("description").asText("")),
                    baselineAvg,
                    candidateAvg,
                    avgDeltaPct,
                    p95DeltaPct,
                    docsDeltaPct,
                    baselineHeap,
                    candidateHeap,
                    heapDeltaPct,
                    verdict.name()));
        }

        Verdict overall = anyRegressed
                ? Verdict.REGRESSED
                : (anyImproved ? Verdict.IMPROVED : Verdict.NEUTRAL);

        return new VerdictReport(
                baselinePath,
                candidatePath,
                candidate.path("profile").asText(""),
                baseline.path("timestamp").asText(""),
                candidate.path("timestamp").asText(""),
                thresholds.avgBandPct(),
                thresholds.heapBandPct(),
                thresholds.gateEnabled(),
                overall.name(),
                anyRegressed,
                scenarios,
                missingScenarios);
    }

    private static void print(VerdictReport report) {
        System.out.println("Benchmark verdict (vs committed baseline)");
        System.out.println("Timestamp: " + LocalDateTime.now().format(TIMESTAMP_FORMAT));
        System.out.println("Profile: " + report.profile());
        System.out.println("Baseline: " + report.baselinePath() + " (" + report.baselineTimestamp() + ")");
        System.out.println("Candidate: " + report.candidatePath() + " (" + report.candidateTimestamp() + ")");
        System.out.println("Bands: avg +/-" + format(report.avgBandPct()) + "%, peakHeap +/-"
                + format(report.heapBandPct()) + "% | gate: " + (report.gateEnabled() ? "enabled" : "disabled"));
        System.out.println();
        System.out.printf("%-18s | %10s | %10s | %10s | %10s | %-10s%n",
                "Scenario", "Avg pct", "p95 pct", "Docs/s pct", "Heap pct", "Verdict");
        System.out.println("-".repeat(82));
        for (ScenarioVerdict row : report.scenarios()) {
            System.out.printf("%-18s | %10s | %10s | %10s | %10s | %-10s%n",
                    row.scenario(),
                    signedPercent(row.avgDeltaPct()),
                    signedPercent(row.p95DeltaPct()),
                    signedPercent(row.docsPerSecondDeltaPct()),
                    signedPercent(row.peakHeapDeltaPct()),
                    row.verdict());
        }
        if (!report.missingScenarios().isEmpty()) {
            System.out.println();
            System.out.println("WARNING: baseline scenarios missing from candidate (not gated): "
                    + String.join(", ", report.missingScenarios()));
        }
        System.out.println();
        System.out.println("Overall verdict: " + report.overallVerdict());
    }

    private static void write(VerdictReport report) throws Exception {
        BenchmarkReportWriter.BenchmarkArtifacts artifacts = BenchmarkReportWriter.prepare("verdicts/current-speed");
        Path jsonPath = artifacts.writeJson(report);
        Path csvPath = artifacts.writeCsv(
                "verdict",
                List.of("scenario", "baseline_avg_ms", "candidate_avg_ms", "avg_delta_pct",
                        "p95_delta_pct", "docs_per_sec_delta_pct",
                        "baseline_peak_heap_mb", "candidate_peak_heap_mb", "peak_heap_delta_pct", "verdict"),
                report.scenarios().stream()
                        .map(row -> List.of(
                                row.scenario(),
                                format(row.baselineAvgMs()),
                                format(row.candidateAvgMs()),
                                format(row.avgDeltaPct()),
                                format(row.p95DeltaPct()),
                                format(row.docsPerSecondDeltaPct()),
                                format(row.baselinePeakHeapMb()),
                                format(row.candidatePeakHeapMb()),
                                format(row.peakHeapDeltaPct()),
                                row.verdict()))
                        .toList());
        System.out.println("Saved JSON verdict report to " + jsonPath);
        System.out.println("Saved CSV verdict report to " + csvPath);
    }

    private static boolean isCurrentSpeed(JsonNode node) {
        return node.has("latency") && node.has("throughput");
    }

    private static Map<String, JsonNode> indexBy(JsonNode latencyArray) {
        Map<String, JsonNode> result = new TreeMap<>();
        latencyArray.forEach(item -> result.put(item.path("scenario").asText(), item));
        return result;
    }

    private static double percentDelta(double baseline, double candidate) {
        if (Double.compare(baseline, 0.0) == 0) {
            return candidate == 0.0 ? 0.0 : 100.0;
        }
        return ((candidate - baseline) / baseline) * 100.0;
    }

    private static String signedPercent(double value) {
        return "%+.2f%%".formatted(value);
    }

    private static String format(double value) {
        return "%.2f".formatted(value);
    }

    /**
     * Noise bands (percent) and the gate flag for a verdict evaluation.
     *
     * @param avgBandPct  band for average latency; a candidate slower than this
     *                    fraction of the baseline regresses
     * @param heapBandPct band for peak heap delta
     * @param gateEnabled whether a regression should fail the build (non-zero exit)
     */
    record Thresholds(double avgBandPct, double heapBandPct, boolean gateEnabled) {

        static Thresholds fromSystemProperties() {
            return new Thresholds(
                    doubleProperty(AVG_BAND_PROPERTY, DEFAULT_AVG_BAND_PCT),
                    doubleProperty(HEAP_BAND_PROPERTY, DEFAULT_HEAP_BAND_PCT),
                    Boolean.parseBoolean(System.getProperty(GATE_PROPERTY, "true")));
        }

        private static double doubleProperty(String key, double fallback) {
            String raw = System.getProperty(key);
            if (raw == null || raw.isBlank()) {
                return fallback;
            }
            try {
                return Double.parseDouble(raw.trim());
            } catch (NumberFormatException ex) {
                return fallback;
            }
        }
    }

    /** Verdict classification for one scenario or for the report as a whole. */
    enum Verdict {
        IMPROVED,
        NEUTRAL,
        REGRESSED
    }

    /** Per-scenario verdict row. */
    record ScenarioVerdict(String scenario,
                           String description,
                           double baselineAvgMs,
                           double candidateAvgMs,
                           double avgDeltaPct,
                           double p95DeltaPct,
                           double docsPerSecondDeltaPct,
                           double baselinePeakHeapMb,
                           double candidatePeakHeapMb,
                           double peakHeapDeltaPct,
                           String verdict) {
    }

    /** Full verdict report, serialized to JSON/CSV. */
    record VerdictReport(String baselinePath,
                         String candidatePath,
                         String profile,
                         String baselineTimestamp,
                         String candidateTimestamp,
                         double avgBandPct,
                         double heapBandPct,
                         boolean gateEnabled,
                         String overallVerdict,
                         boolean regressed,
                         List<ScenarioVerdict> scenarios,
                         List<String> missingScenarios) {
    }
}
