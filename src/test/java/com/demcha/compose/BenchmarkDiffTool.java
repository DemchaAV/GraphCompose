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
            case ARCHITECTURE_COMPARISON -> validateArchitectureProfiles(baseline, candidate);
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
        if (suiteType == SuiteType.CURRENT_SPEED || suiteType == SuiteType.ARCHITECTURE_COMPARISON) {
            System.out.println("Baseline profile: " + baseline.path("profile").asText("?"));
            System.out.println("Candidate profile: " + candidate.path("profile").asText("?"));
        }
        System.out.println();

        BenchmarkReportWriter.BenchmarkArtifacts artifacts = BenchmarkReportWriter.prepare("diffs/" + suiteType.id);

        switch (suiteType) {
            case CURRENT_SPEED -> diffCurrentSpeed(input, baseline, candidate, artifacts);
            case COMPARATIVE -> diffComparative(input, baseline, candidate, artifacts);
            case ARCHITECTURE_COMPARISON -> diffArchitectureComparison(input, baseline, candidate, artifacts);
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

        System.out.println();
        System.out.println("Saved JSON diff report to " + jsonPath);
        System.out.println("Saved CSV diff reports to " + latencyCsv + " and " + throughputCsv);
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

    private void diffArchitectureComparison(DiffInput input,
                                            JsonNode baseline,
                                            JsonNode candidate,
                                            BenchmarkReportWriter.BenchmarkArtifacts artifacts) throws Exception {
        ArchitectureComparisonDiffReport report = buildArchitectureComparisonDiff(input, baseline, candidate);

        System.out.println("Architecture layout diff");
        System.out.printf("%-16s | %12s | %12s | %12s%n",
                "Scenario", "Time pct", "Alloc pct", "GC ms pct");
        System.out.println("-".repeat(61));
        for (ArchitectureLayoutDiff row : report.layout()) {
            System.out.printf("%-16s | %12s | %12s | %12s%n",
                    row.scenario(),
                    signedPercent(row.avgMillisDeltaPct()),
                    signedPercent(row.avgAllocatedMbDeltaPct()),
                    signedPercent(row.avgGcMillisDeltaPct()));
        }

        System.out.println();
        System.out.println("Architecture pdf diff");
        System.out.printf("%-16s | %12s | %12s | %12s | %12s%n",
                "Scenario", "Time pct", "KB pct", "Alloc pct", "GC ms pct");
        System.out.println("-".repeat(78));
        for (ArchitecturePdfDiff row : report.pdf()) {
            System.out.printf("%-16s | %12s | %12s | %12s | %12s%n",
                    row.scenario(),
                    signedPercent(row.avgMillisDeltaPct()),
                    signedPercent(row.avgKilobytesDeltaPct()),
                    signedPercent(row.avgAllocatedMbDeltaPct()),
                    signedPercent(row.avgGcMillisDeltaPct()));
        }

        System.out.println();
        System.out.println("Architecture stage diff");
        System.out.printf("%-16s | %-24s | %12s | %12s | %12s%n",
                "Scenario", "Stage", "Time pct", "Alloc pct", "GC ms pct");
        System.out.println("-".repeat(86));
        for (ArchitectureStageDiff row : report.stages()) {
            System.out.printf("%-16s | %-24s | %12s | %12s | %12s%n",
                    row.scenario(),
                    row.stage(),
                    signedPercent(row.avgMillisDeltaPct()),
                    signedPercent(row.avgAllocatedMbDeltaPct()),
                    signedPercent(row.avgGcMillisDeltaPct()));
        }

        Path jsonPath = artifacts.writeJson(report);
        Path layoutCsv = artifacts.writeCsv(
                "layout-diff",
                List.of("scenario", "baseline_avg_ms", "candidate_avg_ms", "avg_delta_pct",
                        "baseline_avg_allocated_mb", "candidate_avg_allocated_mb", "avg_allocated_delta_pct",
                        "baseline_avg_gc_ms", "candidate_avg_gc_ms", "avg_gc_ms_delta_pct"),
                report.layout().stream()
                        .map(row -> List.of(
                                row.scenario(),
                                format(row.baselineAvgMillis()),
                                format(row.candidateAvgMillis()),
                                format(row.avgMillisDeltaPct()),
                                format(row.baselineAvgAllocatedMb()),
                                format(row.candidateAvgAllocatedMb()),
                                format(row.avgAllocatedMbDeltaPct()),
                                format(row.baselineAvgGcMillis()),
                                format(row.candidateAvgGcMillis()),
                                format(row.avgGcMillisDeltaPct())))
                        .toList());
        Path pdfCsv = artifacts.writeCsv(
                "pdf-diff",
                List.of("scenario", "baseline_avg_ms", "candidate_avg_ms", "avg_delta_pct",
                        "baseline_avg_kb", "candidate_avg_kb", "avg_kb_delta_pct",
                        "baseline_avg_allocated_mb", "candidate_avg_allocated_mb", "avg_allocated_delta_pct",
                        "baseline_avg_gc_ms", "candidate_avg_gc_ms", "avg_gc_ms_delta_pct"),
                report.pdf().stream()
                        .map(row -> List.of(
                                row.scenario(),
                                format(row.baselineAvgMillis()),
                                format(row.candidateAvgMillis()),
                                format(row.avgMillisDeltaPct()),
                                format(row.baselineAvgKilobytes()),
                                format(row.candidateAvgKilobytes()),
                                format(row.avgKilobytesDeltaPct()),
                                format(row.baselineAvgAllocatedMb()),
                                format(row.candidateAvgAllocatedMb()),
                                format(row.avgAllocatedMbDeltaPct()),
                                format(row.baselineAvgGcMillis()),
                                format(row.candidateAvgGcMillis()),
                                format(row.avgGcMillisDeltaPct())))
                        .toList());
        Path stagesCsv = artifacts.writeCsv(
                "stages-diff",
                List.of("scenario", "stage", "baseline_avg_ms", "candidate_avg_ms", "avg_delta_pct",
                        "baseline_avg_allocated_mb", "candidate_avg_allocated_mb", "avg_allocated_delta_pct",
                        "baseline_avg_gc_ms", "candidate_avg_gc_ms", "avg_gc_ms_delta_pct"),
                report.stages().stream()
                        .map(row -> List.of(
                                row.scenario(),
                                row.stage(),
                                format(row.baselineAvgMillis()),
                                format(row.candidateAvgMillis()),
                                format(row.avgMillisDeltaPct()),
                                format(row.baselineAvgAllocatedMb()),
                                format(row.candidateAvgAllocatedMb()),
                                format(row.avgAllocatedMbDeltaPct()),
                                format(row.baselineAvgGcMillis()),
                                format(row.candidateAvgGcMillis()),
                                format(row.avgGcMillisDeltaPct())))
                        .toList());

        System.out.println();
        System.out.println("Saved JSON diff report to " + jsonPath);
        System.out.println("Saved CSV diff reports to " + layoutCsv + ", " + pdfCsv + " and " + stagesCsv);
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
                latencyDiffs,
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

    private ArchitectureComparisonDiffReport buildArchitectureComparisonDiff(DiffInput input, JsonNode baseline, JsonNode candidate) {
        Map<String, JsonNode> baselineLayout = indexBy(baseline.path("layout"), "scenario");
        Map<String, JsonNode> candidateLayout = indexBy(candidate.path("layout"), "scenario");
        List<ArchitectureLayoutDiff> layoutRows = intersectKeys(baselineLayout, candidateLayout).stream()
                .map(key -> {
                    JsonNode before = baselineLayout.get(key);
                    JsonNode after = candidateLayout.get(key);
                    return new ArchitectureLayoutDiff(
                            key,
                            before.path("avgMillis").asDouble(before.path("legacyAvgMillis").asDouble()),
                            after.path("avgMillis").asDouble(after.path("legacyAvgMillis").asDouble()),
                            percentDelta(before.path("legacyAvgMillis").asDouble(), after.path("legacyAvgMillis").asDouble()),
                            before.path("legacyAvgAllocatedMb").asDouble(),
                            after.path("legacyAvgAllocatedMb").asDouble(),
                            percentDelta(before.path("legacyAvgAllocatedMb").asDouble(), after.path("legacyAvgAllocatedMb").asDouble()),
                            before.path("legacyAvgGcMillis").asDouble(),
                            after.path("legacyAvgGcMillis").asDouble(),
                            percentDelta(before.path("legacyAvgGcMillis").asDouble(), after.path("legacyAvgGcMillis").asDouble()));
                })
                .toList();

        Map<String, JsonNode> baselinePdf = indexBy(baseline.path("pdf"), "scenario");
        Map<String, JsonNode> candidatePdf = indexBy(candidate.path("pdf"), "scenario");
        List<ArchitecturePdfDiff> pdfRows = intersectKeys(baselinePdf, candidatePdf).stream()
                .map(key -> {
                    JsonNode before = baselinePdf.get(key);
                    JsonNode after = candidatePdf.get(key);
                    return new ArchitecturePdfDiff(
                            key,
                            before.path("legacyAvgMillis").asDouble(),
                            after.path("legacyAvgMillis").asDouble(),
                            percentDelta(before.path("legacyAvgMillis").asDouble(), after.path("legacyAvgMillis").asDouble()),
                            before.path("legacyAvgKilobytes").asDouble(),
                            after.path("legacyAvgKilobytes").asDouble(),
                            percentDelta(before.path("legacyAvgKilobytes").asDouble(), after.path("legacyAvgKilobytes").asDouble()),
                            before.path("legacyAvgAllocatedMb").asDouble(),
                            after.path("legacyAvgAllocatedMb").asDouble(),
                            percentDelta(before.path("legacyAvgAllocatedMb").asDouble(), after.path("legacyAvgAllocatedMb").asDouble()),
                            before.path("legacyAvgGcMillis").asDouble(),
                            after.path("legacyAvgGcMillis").asDouble(),
                            percentDelta(before.path("legacyAvgGcMillis").asDouble(), after.path("legacyAvgGcMillis").asDouble()));
                })
                .toList();

        Map<String, JsonNode> baselineStages = indexArchitectureStages(baseline.path("stages"));
        Map<String, JsonNode> candidateStages = indexArchitectureStages(candidate.path("stages"));
        List<ArchitectureStageDiff> stageRows = intersectKeys(baselineStages, candidateStages).stream()
                .map(key -> {
                    JsonNode before = baselineStages.get(key);
                    JsonNode after = candidateStages.get(key);
                    return new ArchitectureStageDiff(
                            before.path("scenario").asText(),
                            before.path("stage").asText(),
                            before.path("legacyAvgMillis").asDouble(),
                            after.path("legacyAvgMillis").asDouble(),
                            percentDelta(before.path("legacyAvgMillis").asDouble(), after.path("legacyAvgMillis").asDouble()),
                            before.path("legacyAvgAllocatedMb").asDouble(),
                            after.path("legacyAvgAllocatedMb").asDouble(),
                            percentDelta(before.path("legacyAvgAllocatedMb").asDouble(), after.path("legacyAvgAllocatedMb").asDouble()),
                            before.path("legacyAvgGcMillis").asDouble(),
                            after.path("legacyAvgGcMillis").asDouble(),
                            percentDelta(before.path("legacyAvgGcMillis").asDouble(), after.path("legacyAvgGcMillis").asDouble()));
                })
                .toList();

        return new ArchitectureComparisonDiffReport(
                input.baselinePath().toString(),
                input.candidatePath().toString(),
                baseline.path("timestamp").asText(),
                candidate.path("timestamp").asText(),
                layoutRows,
                pdfRows,
                stageRows
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

    private static Map<String, JsonNode> indexArchitectureStages(JsonNode array) {
        Map<String, JsonNode> result = new TreeMap<>();
        for (JsonNode item : iterable(array)) {
            String key = item.path("scenario").asText() + "#" + item.path("stage").asText();
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
                  java ... com.demcha.compose.BenchmarkDiffTool architecture-comparison
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

        if (SuiteType.CURRENT_SPEED.id.equals(suiteName) || SuiteType.ARCHITECTURE_COMPARISON.id.equals(suiteName)) {
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
        if (node.has("layout") && node.has("pdf")) {
            return SuiteType.ARCHITECTURE_COMPARISON;
        }
        throw new IllegalArgumentException("Unknown benchmark report schema.");
    }

    private static void validateCurrentSpeedProfiles(JsonNode baseline, JsonNode candidate) {
        validateProfiles("Current-speed", baseline, candidate);
    }

    private static void validateArchitectureProfiles(JsonNode baseline, JsonNode candidate) {
        validateProfiles("Architecture-comparison", baseline, candidate);
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
        COMPARATIVE("comparative"),
        ARCHITECTURE_COMPARISON("architecture-comparison");

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

    private record CurrentSpeedDiffReport(String baselinePath,
                                          String candidatePath,
                                          String baselineTimestamp,
                                          String candidateTimestamp,
                                          List<CurrentSpeedLatencyDiff> latency,
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

    private record ArchitectureLayoutDiff(String scenario,
                                          double baselineAvgMillis,
                                          double candidateAvgMillis,
                                          double avgMillisDeltaPct,
                                          double baselineAvgAllocatedMb,
                                          double candidateAvgAllocatedMb,
                                          double avgAllocatedMbDeltaPct,
                                          double baselineAvgGcMillis,
                                          double candidateAvgGcMillis,
                                          double avgGcMillisDeltaPct) {
    }

    private record ArchitecturePdfDiff(String scenario,
                                       double baselineAvgMillis,
                                       double candidateAvgMillis,
                                       double avgMillisDeltaPct,
                                       double baselineAvgKilobytes,
                                       double candidateAvgKilobytes,
                                       double avgKilobytesDeltaPct,
                                       double baselineAvgAllocatedMb,
                                       double candidateAvgAllocatedMb,
                                       double avgAllocatedMbDeltaPct,
                                       double baselineAvgGcMillis,
                                       double candidateAvgGcMillis,
                                       double avgGcMillisDeltaPct) {
    }

    private record ArchitectureStageDiff(String scenario,
                                         String stage,
                                         double baselineAvgMillis,
                                         double candidateAvgMillis,
                                         double avgMillisDeltaPct,
                                         double baselineAvgAllocatedMb,
                                         double candidateAvgAllocatedMb,
                                         double avgAllocatedMbDeltaPct,
                                         double baselineAvgGcMillis,
                                         double candidateAvgGcMillis,
                                         double avgGcMillisDeltaPct) {
    }

    private record ArchitectureComparisonDiffReport(String baselinePath,
                                                    String candidatePath,
                                                    String baselineTimestamp,
                                                    String candidateTimestamp,
                                                    List<ArchitectureLayoutDiff> layout,
                                                    List<ArchitecturePdfDiff> pdf,
                                                    List<ArchitectureStageDiff> stages) {
    }
}
