package com.demcha.compose;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Diffs two benchmark JSON reports and prints human-readable deltas.
 *
 * <p>Usage:</p>
 * <ul>
 *     <li>{@code java ... com.demcha.compose.BenchmarkDiffTool current-speed}</li>
 *     <li>{@code java ... com.demcha.compose.BenchmarkDiffTool comparative}</li>
 *     <li>{@code java ... com.demcha.compose.BenchmarkDiffTool before.json after.json}</li>
 * </ul>
 *
 * <p>The suite-name shortcut compares the two newest {@code run-*.json} files
 * from {@code target/benchmarks/<suite>/}.</p>
 */
public final class BenchmarkDiffTool {

    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws Exception {
        BenchmarkSupport.configureQuietLogging();
        DiffInput input = resolveInput(args);
        new BenchmarkDiffTool().run(input);
    }

    private void run(DiffInput input) throws Exception {
        JsonNode baseline = JSON.readTree(Files.readAllBytes(input.baselinePath()));
        JsonNode candidate = JSON.readTree(Files.readAllBytes(input.candidatePath()));

        SuiteType suiteType = detectSuiteType(baseline, candidate);
        switch (suiteType) {
            case CURRENT_SPEED -> validateCurrentSpeedProfiles(baseline, candidate);
            case COMPARATIVE -> {
            }
        }

        System.out.println("Benchmark diff");
        System.out.println("Timestamp: " + LocalDateTime.now().format(TIMESTAMP_FORMAT));
        System.out.println("Suite: " + suiteType.id);
        System.out.println("Baseline: " + input.baselinePath());
        System.out.println("Candidate: " + input.candidatePath());
        System.out.println("Baseline timestamp: " + baseline.path("timestamp").asText("?"));
        System.out.println("Candidate timestamp: " + candidate.path("timestamp").asText("?"));
        if (suiteType == SuiteType.CURRENT_SPEED) {
            System.out.println("Baseline profile: " + baseline.path("profile").asText("?"));
            System.out.println("Candidate profile: " + candidate.path("profile").asText("?"));
        }
        System.out.println();

        BenchmarkReportWriter.BenchmarkArtifacts artifacts = BenchmarkReportWriter.prepare("diffs/" + suiteType.id);

        switch (suiteType) {
            case CURRENT_SPEED -> diffCurrentSpeed(input, baseline, candidate, artifacts);
            case COMPARATIVE -> diffComparative(input, baseline, candidate, artifacts);
        }
    }

    private void diffCurrentSpeed(DiffInput input,
                                  JsonNode baseline,
                                  JsonNode candidate,
                                  BenchmarkReportWriter.BenchmarkArtifacts artifacts) throws Exception {
        CurrentSpeedDiffReport report = buildCurrentSpeedDiff(input, baseline, candidate);

        System.out.println("Latency diff");
        System.out.printf("%-18s | %10s | %10s | %10s | %10s | %10s%n",
                "Scenario", "Avg pct", "p95 pct", "Docs/s pct", "KB pct", "Heap pct");
        System.out.println("-".repeat(85));
        for (CurrentSpeedLatencyDiff row : report.latency()) {
            System.out.printf("%-18s | %10s | %10s | %10s | %10s | %10s%n",
                    row.scenario(),
                    signedPercent(row.avgMillisDeltaPct()),
                    signedPercent(row.p95MillisDeltaPct()),
                    signedPercent(row.docsPerSecondDeltaPct()),
                    signedPercent(row.avgKilobytesDeltaPct()),
                    signedPercent(row.peakHeapMbDeltaPct()));
        }

        if (!report.addedScenarios().isEmpty() || !report.removedScenarios().isEmpty()) {
            System.out.println();
            System.out.println("Scenario set changes");
            System.out.println("  Added in candidate:    "
                    + (report.addedScenarios().isEmpty() ? "(none)" : String.join(", ", report.addedScenarios())));
            System.out.println("  Removed from baseline: "
                    + (report.removedScenarios().isEmpty() ? "(none)" : String.join(", ", report.removedScenarios())));
        }

        if (!report.stages().isEmpty()) {
            System.out.println();
            System.out.println("Stage diff (pct delta per stage)");
            System.out.printf("%-18s | %12s | %12s | %12s | %12s%n",
                    "Scenario", "Compose pct", "Layout pct", "Render pct", "Total pct");
            System.out.println("-".repeat(78));
            for (StageDiff row : report.stages()) {
                System.out.printf("%-18s | %12s | %12s | %12s | %12s%n",
                        row.scenario(),
                        signedPercent(row.composeDeltaPct()),
                        signedPercent(row.layoutDeltaPct()),
                        signedPercent(row.renderDeltaPct()),
                        signedPercent(row.totalDeltaPct()));
            }
        }

        System.out.println();
        System.out.println("Throughput diff");
        System.out.printf("%-18s | %8s | %12s | %14s%n",
                "Scenario", "Threads", "Docs/s pct", "Avg doc ms pct");
        System.out.println("-".repeat(64));
        for (CurrentSpeedThroughputDiff row : report.throughput()) {
            System.out.printf("%-18s | %8d | %12s | %14s%n",
                    row.scenario(),
                    row.threads(),
                    signedPercent(row.docsPerSecondDeltaPct()),
                    signedPercent(row.avgMillisPerDocDeltaPct()));
        }

        Path jsonPath = artifacts.writeJson(report);
        Path latencyCsv = artifacts.writeCsv(
                "latency-diff",
                List.of("scenario", "baseline_avg_ms", "candidate_avg_ms", "avg_delta_pct", "baseline_p95_ms", "candidate_p95_ms", "p95_delta_pct", "baseline_docs_per_sec", "candidate_docs_per_sec", "docs_per_sec_delta_pct", "baseline_avg_kb", "candidate_avg_kb", "avg_kb_delta_pct", "baseline_peak_heap_mb", "candidate_peak_heap_mb", "peak_heap_delta_pct"),
                report.latency().stream()
                        .map(row -> List.of(
                                row.scenario(),
                                format(row.baselineAvgMillis()),
                                format(row.candidateAvgMillis()),
                                format(row.avgMillisDeltaPct()),
                                format(row.baselineP95Millis()),
                                format(row.candidateP95Millis()),
                                format(row.p95MillisDeltaPct()),
                                format(row.baselineDocsPerSecond()),
                                format(row.candidateDocsPerSecond()),
                                format(row.docsPerSecondDeltaPct()),
                                format(row.baselineAvgKilobytes()),
                                format(row.candidateAvgKilobytes()),
                                format(row.avgKilobytesDeltaPct()),
                                format(row.baselinePeakHeapMb()),
                                format(row.candidatePeakHeapMb()),
                                format(row.peakHeapMbDeltaPct())))
                        .toList());
        Path throughputCsv = artifacts.writeCsv(
                "throughput-diff",
                List.of("scenario", "threads", "baseline_docs_per_sec", "candidate_docs_per_sec", "docs_per_sec_delta_pct", "baseline_avg_doc_ms", "candidate_avg_doc_ms", "avg_doc_ms_delta_pct"),
                report.throughput().stream()
                        .map(row -> List.of(
                                row.scenario(),
                                Integer.toString(row.threads()),
                                format(row.baselineDocsPerSecond()),
                                format(row.candidateDocsPerSecond()),
                                format(row.docsPerSecondDeltaPct()),
                                format(row.baselineAvgMillisPerDoc()),
                                format(row.candidateAvgMillisPerDoc()),
                                format(row.avgMillisPerDocDeltaPct())))
                        .toList());
        Path stagesCsv = artifacts.writeCsv(
                "stages-diff",
                List.of("scenario", "baseline_compose_ms", "candidate_compose_ms", "compose_delta_pct", "baseline_layout_ms", "candidate_layout_ms", "layout_delta_pct", "baseline_render_ms", "candidate_render_ms", "render_delta_pct", "baseline_total_ms", "candidate_total_ms", "total_delta_pct"),
                report.stages().stream()
                        .map(row -> List.of(
                                row.scenario(),
                                format(row.baselineComposeMillis()),
                                format(row.candidateComposeMillis()),
                                format(row.composeDeltaPct()),
                                format(row.baselineLayoutMillis()),
                                format(row.candidateLayoutMillis()),
                                format(row.layoutDeltaPct()),
                                format(row.baselineRenderMillis()),
                                format(row.candidateRenderMillis()),
                                format(row.renderDeltaPct()),
                                format(row.baselineTotalMillis()),
                                format(row.candidateTotalMillis()),
                                format(row.totalDeltaPct())))
                        .toList());

        System.out.println();
        System.out.println("Saved JSON diff report to " + jsonPath);
        System.out.println("Saved CSV diff reports to " + latencyCsv + ", " + throughputCsv + ", and " + stagesCsv);
    }

    private void diffComparative(DiffInput input,
                                 JsonNode baseline,
                                 JsonNode candidate,
                                 BenchmarkReportWriter.BenchmarkArtifacts artifacts) throws Exception {
        ComparativeDiffReport report = buildComparativeDiff(input, baseline, candidate);

        System.out.println("Comparative diff");
        System.out.printf("%-20s | %12s | %12s%n",
                "Library", "Time pct", "Heap pct");
        System.out.println("-".repeat(52));
        for (ComparativeLibraryDiff row : report.libraries()) {
            System.out.printf("%-20s | %12s | %12s%n",
                    row.library(),
                    signedPercent(row.avgTimeDeltaPct()),
                    signedPercent(row.avgHeapDeltaPct()));
        }

        Path jsonPath = artifacts.writeJson(report);
        Path csvPath = artifacts.writeCsv(
                "libraries-diff",
                List.of("library", "baseline_avg_time_ms", "candidate_avg_time_ms", "avg_time_delta_pct", "baseline_avg_heap_mb", "candidate_avg_heap_mb", "avg_heap_delta_pct"),
                report.libraries().stream()
                        .map(row -> List.of(
                                row.library(),
                                format(row.baselineAvgTimeMs()),
                                format(row.candidateAvgTimeMs()),
                                format(row.avgTimeDeltaPct()),
                                format(row.baselineAvgHeapMb()),
                                format(row.candidateAvgHeapMb()),
                                format(row.avgHeapDeltaPct())))
                        .toList());

        System.out.println();
        System.out.println("Saved JSON diff report to " + jsonPath);
        System.out.println("Saved CSV diff report to " + csvPath);
    }

    private CurrentSpeedDiffReport buildCurrentSpeedDiff(DiffInput input, JsonNode baseline, JsonNode candidate) {
        Map<String, JsonNode> baselineLatency = indexBy(baseline.path("latency"), "scenario");
        Map<String, JsonNode> candidateLatency = indexBy(candidate.path("latency"), "scenario");
        List<CurrentSpeedLatencyDiff> latencyDiffs = intersectKeys(baselineLatency, candidateLatency).stream()
                .map(key -> {
                    JsonNode before = baselineLatency.get(key);
                    JsonNode after = candidateLatency.get(key);
                    return new CurrentSpeedLatencyDiff(
                            key,
                            before.path("description").asText(after.path("description").asText("")),
                            before.path("avgMillis").asDouble(),
                            after.path("avgMillis").asDouble(),
                            percentDelta(before.path("avgMillis").asDouble(), after.path("avgMillis").asDouble()),
                            before.path("p95Millis").asDouble(),
                            after.path("p95Millis").asDouble(),
                            percentDelta(before.path("p95Millis").asDouble(), after.path("p95Millis").asDouble()),
                            before.path("docsPerSecond").asDouble(),
                            after.path("docsPerSecond").asDouble(),
                            percentDelta(before.path("docsPerSecond").asDouble(), after.path("docsPerSecond").asDouble()),
                            before.path("avgKilobytes").asDouble(),
                            after.path("avgKilobytes").asDouble(),
                            percentDelta(before.path("avgKilobytes").asDouble(), after.path("avgKilobytes").asDouble()),
                            before.path("peakHeapMb").asDouble(),
                            after.path("peakHeapMb").asDouble(),
                            percentDelta(before.path("peakHeapMb").asDouble(), after.path("peakHeapMb").asDouble()));
                })
                .toList();

        Map<String, JsonNode> baselineStages = indexBy(baseline.path("stages"), "scenario");
        Map<String, JsonNode> candidateStages = indexBy(candidate.path("stages"), "scenario");
        List<StageDiff> stageDiffs = intersectKeys(baselineStages, candidateStages).stream()
                .map(key -> {
                    JsonNode before = baselineStages.get(key);
                    JsonNode after = candidateStages.get(key);
                    return new StageDiff(
                            key,
                            before.path("composeMillis").asDouble(),
                            after.path("composeMillis").asDouble(),
                            percentDelta(before.path("composeMillis").asDouble(), after.path("composeMillis").asDouble()),
                            before.path("layoutMillis").asDouble(),
                            after.path("layoutMillis").asDouble(),
                            percentDelta(before.path("layoutMillis").asDouble(), after.path("layoutMillis").asDouble()),
                            before.path("renderMillis").asDouble(),
                            after.path("renderMillis").asDouble(),
                            percentDelta(before.path("renderMillis").asDouble(), after.path("renderMillis").asDouble()),
                            before.path("totalMillis").asDouble(),
                            after.path("totalMillis").asDouble(),
                            percentDelta(before.path("totalMillis").asDouble(), after.path("totalMillis").asDouble()));
                })
                .toList();

        Map<String, JsonNode> baselineThroughput = indexThroughput(baseline.path("throughput"));
        Map<String, JsonNode> candidateThroughput = indexThroughput(candidate.path("throughput"));
        List<CurrentSpeedThroughputDiff> throughputDiffs = intersectKeys(baselineThroughput, candidateThroughput).stream()
                .map(key -> {
                    JsonNode before = baselineThroughput.get(key);
                    JsonNode after = candidateThroughput.get(key);
                    return new CurrentSpeedThroughputDiff(
                            before.path("scenario").asText(),
                            before.path("threads").asInt(),
                            before.path("docsPerSecond").asDouble(),
                            after.path("docsPerSecond").asDouble(),
                            percentDelta(before.path("docsPerSecond").asDouble(), after.path("docsPerSecond").asDouble()),
                            before.path("avgMillisPerDoc").asDouble(),
                            after.path("avgMillisPerDoc").asDouble(),
                            percentDelta(before.path("avgMillisPerDoc").asDouble(), after.path("avgMillisPerDoc").asDouble()));
                })
                .toList();

        return new CurrentSpeedDiffReport(
                input.baselinePath().toString(),
                input.candidatePath().toString(),
                baseline.path("timestamp").asText(),
                candidate.path("timestamp").asText(),
                addedKeys(baselineLatency, candidateLatency),
                removedKeys(baselineLatency, candidateLatency),
                latencyDiffs,
                stageDiffs,
                throughputDiffs
        );
    }

    private ComparativeDiffReport buildComparativeDiff(DiffInput input, JsonNode baseline, JsonNode candidate) {
        Map<String, JsonNode> baselineLibraries = indexBy(baseline.path("libraries"), "library");
        Map<String, JsonNode> candidateLibraries = indexBy(candidate.path("libraries"), "library");

        List<ComparativeLibraryDiff> rows = intersectKeys(baselineLibraries, candidateLibraries).stream()
                .map(key -> {
                    JsonNode before = baselineLibraries.get(key);
                    JsonNode after = candidateLibraries.get(key);
                    return new ComparativeLibraryDiff(
                            key,
                            before.path("avgTimeMs").asDouble(),
                            after.path("avgTimeMs").asDouble(),
                            percentDelta(before.path("avgTimeMs").asDouble(), after.path("avgTimeMs").asDouble()),
                            before.path("avgHeapMb").asDouble(),
                            after.path("avgHeapMb").asDouble(),
                            percentDelta(before.path("avgHeapMb").asDouble(), after.path("avgHeapMb").asDouble()));
                })
                .toList();

        return new ComparativeDiffReport(
                input.baselinePath().toString(),
                input.candidatePath().toString(),
                baseline.path("timestamp").asText(),
                candidate.path("timestamp").asText(),
                rows
        );
    }

    private static Map<String, JsonNode> indexBy(JsonNode array, String keyField) {
        Map<String, JsonNode> result = new TreeMap<>();
        for (JsonNode item : iterable(array)) {
            result.put(item.path(keyField).asText(), item);
        }
        return result;
    }

    private static Map<String, JsonNode> indexThroughput(JsonNode array) {
        Map<String, JsonNode> result = new TreeMap<>();
        for (JsonNode item : iterable(array)) {
            String key = item.path("scenario").asText() + "#" + item.path("threads").asInt();
            result.put(key, item);
        }
        return result;
    }

    private static List<String> intersectKeys(Map<String, JsonNode> left, Map<String, JsonNode> right) {
        return left.keySet().stream()
                .filter(right::containsKey)
                .sorted()
                .toList();
    }

    /** Keys present in {@code candidate} but not {@code baseline} (new scenarios). */
    private static List<String> addedKeys(Map<String, JsonNode> baseline, Map<String, JsonNode> candidate) {
        return candidate.keySet().stream().filter(key -> !baseline.containsKey(key)).sorted().toList();
    }

    /** Keys present in {@code baseline} but not {@code candidate} (dropped scenarios). */
    private static List<String> removedKeys(Map<String, JsonNode> baseline, Map<String, JsonNode> candidate) {
        return baseline.keySet().stream().filter(key -> !candidate.containsKey(key)).sorted().toList();
    }

    private static Iterable<JsonNode> iterable(JsonNode array) {
        return () -> new Iterator<>() {
            private final Iterator<JsonNode> delegate = array.iterator();

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public JsonNode next() {
                return delegate.next();
            }
        };
    }

    private static DiffInput resolveInput(String[] args) throws IOException {
        if (args.length == 1) {
            return resolveLatestRuns(args[0]);
        }
        if (args.length == 2) {
            return new DiffInput(Path.of(args[0]), Path.of(args[1]));
        }
        throw new IllegalArgumentException("""
                Usage:
                  java ... com.demcha.compose.BenchmarkDiffTool current-speed
                  java ... com.demcha.compose.BenchmarkDiffTool comparative
                  java ... com.demcha.compose.BenchmarkDiffTool <baseline.json> <candidate.json>
                """);
    }

    static DiffInput resolveLatestRuns(String suiteName) throws IOException {
        ResolvedRunPair pair = resolveLatestRunPaths(benchmarkRoot(), suiteName);
        return new DiffInput(pair.baselinePath(), pair.candidatePath());
    }

    static ResolvedRunPair resolveLatestRunPaths(Path benchmarkRoot, String suiteName) throws IOException {
        Path suiteDir = benchmarkRoot.resolve(suiteName);
        if (!Files.isDirectory(suiteDir)) {
            throw new IllegalArgumentException("Benchmark suite directory not found: " + suiteDir);
        }

        List<Path> runs = Files.list(suiteDir)
                .filter(path -> path.getFileName().toString().startsWith("run-"))
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .sorted(Comparator.comparing(Path::getFileName))
                .toList();

        if (runs.size() < 2) {
            throw new IllegalArgumentException("Need at least two run-*.json files in " + suiteDir + " to diff latest runs.");
        }

        if (SuiteType.CURRENT_SPEED.id.equals(suiteName)) {
            Path latestRun = runs.get(runs.size() - 1);
            String latestProfile = benchmarkProfile(latestRun);
            List<Path> comparableRuns = runs.stream()
                    .filter(path -> latestProfile.equals(benchmarkProfile(path)))
                    .toList();

            if (comparableRuns.size() < 2) {
                throw new IllegalArgumentException(
                        "Need at least two " + suiteName + " run-*.json files with profile '" + latestProfile + "' in "
                                + suiteDir + " to diff latest runs.");
            }

            return new ResolvedRunPair(
                    comparableRuns.get(comparableRuns.size() - 2),
                    comparableRuns.get(comparableRuns.size() - 1));
        }

        return new ResolvedRunPair(runs.get(runs.size() - 2), runs.get(runs.size() - 1));
    }

    private static Path benchmarkRoot() {
        return Path.of(System.getProperty("graphcompose.benchmark.root", Path.of("target", "benchmarks").toString()));
    }

    private SuiteType detectSuiteType(JsonNode baseline, JsonNode candidate) {
        SuiteType baselineType = detectSuiteType(baseline);
        SuiteType candidateType = detectSuiteType(candidate);
        if (baselineType != candidateType) {
            throw new IllegalArgumentException("Benchmark report types do not match: " + baselineType.id + " vs " + candidateType.id);
        }
        return baselineType;
    }

    private SuiteType detectSuiteType(JsonNode node) {
        if (node.has("latency") && node.has("throughput")) {
            return SuiteType.CURRENT_SPEED;
        }
        if (node.has("libraries")) {
            return SuiteType.COMPARATIVE;
        }
        throw new IllegalArgumentException("Unknown benchmark report schema.");
    }

    private static void validateCurrentSpeedProfiles(JsonNode baseline, JsonNode candidate) {
        validateProfiles("Current-speed", baseline, candidate);
    }

    private static void validateProfiles(String label, JsonNode baseline, JsonNode candidate) {
        String baselineProfile = baseline.path("profile").asText("");
        String candidateProfile = candidate.path("profile").asText("");
        if (!baselineProfile.equals(candidateProfile)) {
            throw new IllegalArgumentException(
                    label + " benchmark profiles do not match: baseline='"
                            + baselineProfile
                            + "', candidate='"
                            + candidateProfile
                            + "'. Compare runs from the same profile only.");
        }
    }

    private static String benchmarkProfile(Path reportPath) {
        try {
            JsonNode report = JSON.readTree(Files.readAllBytes(reportPath));
            return report.path("profile").asText("full");
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to read benchmark report: " + reportPath, ex);
        }
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

    private enum SuiteType {
        CURRENT_SPEED("current-speed"),
        COMPARATIVE("comparative");

        private final String id;

        SuiteType(String id) {
            this.id = id;
        }
    }

    private record DiffInput(Path baselinePath, Path candidatePath) {
    }

    record ResolvedRunPair(Path baselinePath, Path candidatePath) {
    }

    private record CurrentSpeedLatencyDiff(String scenario,
                                           String description,
                                           double baselineAvgMillis,
                                           double candidateAvgMillis,
                                           double avgMillisDeltaPct,
                                           double baselineP95Millis,
                                           double candidateP95Millis,
                                           double p95MillisDeltaPct,
                                           double baselineDocsPerSecond,
                                           double candidateDocsPerSecond,
                                           double docsPerSecondDeltaPct,
                                           double baselineAvgKilobytes,
                                           double candidateAvgKilobytes,
                                           double avgKilobytesDeltaPct,
                                           double baselinePeakHeapMb,
                                           double candidatePeakHeapMb,
                                           double peakHeapMbDeltaPct) {
    }

    private record CurrentSpeedThroughputDiff(String scenario,
                                              int threads,
                                              double baselineDocsPerSecond,
                                              double candidateDocsPerSecond,
                                              double docsPerSecondDeltaPct,
                                              double baselineAvgMillisPerDoc,
                                              double candidateAvgMillisPerDoc,
                                              double avgMillisPerDocDeltaPct) {
    }

    private record StageDiff(String scenario,
                             double baselineComposeMillis,
                             double candidateComposeMillis,
                             double composeDeltaPct,
                             double baselineLayoutMillis,
                             double candidateLayoutMillis,
                             double layoutDeltaPct,
                             double baselineRenderMillis,
                             double candidateRenderMillis,
                             double renderDeltaPct,
                             double baselineTotalMillis,
                             double candidateTotalMillis,
                             double totalDeltaPct) {
    }

    private record CurrentSpeedDiffReport(String baselinePath,
                                          String candidatePath,
                                          String baselineTimestamp,
                                          String candidateTimestamp,
                                          List<String> addedScenarios,
                                          List<String> removedScenarios,
                                          List<CurrentSpeedLatencyDiff> latency,
                                          List<StageDiff> stages,
                                          List<CurrentSpeedThroughputDiff> throughput) {
    }

    private record ComparativeLibraryDiff(String library,
                                          double baselineAvgTimeMs,
                                          double candidateAvgTimeMs,
                                          double avgTimeDeltaPct,
                                          double baselineAvgHeapMb,
                                          double candidateAvgHeapMb,
                                          double avgHeapDeltaPct) {
    }

    private record ComparativeDiffReport(String baselinePath,
                                         String candidatePath,
                                         String baselineTimestamp,
                                         String candidateTimestamp,
                                         List<ComparativeLibraryDiff> libraries) {
    }

}
