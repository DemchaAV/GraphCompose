package com.demcha.compose;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Aggregates repeated benchmark runs into one median report per suite.
 *
 * <p>The resulting JSON keeps the same schema as the original benchmark where
 * possible, so it can be diffed by {@link BenchmarkDiffTool}. The tool is meant
 * for local benchmark sessions where a few repeated runs are needed to reduce
 * machine noise before comparing results.</p>
 */
public final class BenchmarkMedianTool {

    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws Exception {
        BenchmarkSupport.configureQuietLogging();
        Input input = resolveInput(args);
        new BenchmarkMedianTool().run(input);
    }

    private void run(Input input) throws Exception {
        List<ReportFile> reportFiles = new ArrayList<>();
        for (Path reportPath : input.reportPaths()) {
            reportFiles.add(new ReportFile(reportPath, JSON.readTree(Files.readAllBytes(reportPath))));
        }

        SuiteType suiteType = detectSuiteType(reportFiles.getFirst().report());
        for (ReportFile reportFile : reportFiles) {
            SuiteType currentType = detectSuiteType(reportFile.report());
            if (currentType != suiteType) {
                throw new IllegalArgumentException(
                        "Benchmark report types do not match: " + suiteType.id() + " vs " + currentType.id());
            }
        }

        switch (suiteType) {
            case CURRENT_SPEED -> aggregateCurrentSpeed(reportFiles);
            case COMPARATIVE -> aggregateComparative(reportFiles);
        }
    }

    private void aggregateCurrentSpeed(List<ReportFile> reportFiles) throws Exception {
        JsonNode firstReport = reportFiles.getFirst().report();
        String profile = firstReport.path("profile").asText("full");
        for (ReportFile reportFile : reportFiles) {
            String currentProfile = reportFile.report().path("profile").asText("full");
            if (!profile.equals(currentProfile)) {
                throw new IllegalArgumentException(
                        "Current-speed profiles do not match across repeated runs: expected '"
                                + profile + "' but found '" + currentProfile + "' in " + reportFile.path());
            }
        }

        int warmupIterations = requireIntConsistency(reportFiles, "warmupIterations");
        int measurementIterations = requireIntConsistency(reportFiles, "measurementIterations");
        int docsPerThread = requireIntConsistency(reportFiles, "docsPerThread");
        List<Integer> threadCounts = requireIntegerArrayConsistency(reportFiles, "threadCounts");

        List<CurrentSpeedLatencyMedianRow> latencyRows = aggregateCurrentSpeedLatency(reportFiles);
        List<CurrentSpeedThroughputMedianRow> throughputRows = aggregateCurrentSpeedThroughput(reportFiles);

        long totalBytesMedian = Math.round(median(
                reportFiles.stream()
                        .mapToDouble(reportFile -> reportFile.report().path("totalBytes").asLong())
                        .toArray()));

        CurrentSpeedMedianReport report = new CurrentSpeedMedianReport(
                LocalDateTime.now().format(TIMESTAMP_FORMAT),
                profile,
                warmupIterations,
                measurementIterations,
                docsPerThread,
                threadCounts,
                latencyRows,
                throughputRows,
                totalBytesMedian,
                "median",
                reportFiles.size(),
                reportFiles.stream().map(reportFile -> reportFile.path().toString()).toList());

        BenchmarkReportWriter.BenchmarkArtifacts artifacts =
                BenchmarkReportWriter.prepare("aggregates/current-speed/" + profile);
        Path jsonPath = artifacts.writeJson(report);
        Path latencyCsv = artifacts.writeCsv(
                "latency",
                List.of("scenario", "description", "avg_ms", "p50_ms", "p95_ms", "max_ms", "docs_per_sec", "avg_kb", "peak_heap_mb"),
                latencyRows.stream()
                        .map(row -> List.of(
                                row.scenario(),
                                row.description(),
                                format(row.avgMillis()),
                                format(row.p50Millis()),
                                format(row.p95Millis()),
                                format(row.maxMillis()),
                                format(row.docsPerSecond()),
                                format(row.avgKilobytes()),
                                format(row.peakHeapMb())))
                        .toList());
        Path throughputCsv = artifacts.writeCsv(
                "throughput",
                List.of("scenario", "threads", "total_docs", "docs_per_sec", "avg_doc_ms"),
                throughputRows.stream()
                        .map(row -> List.of(
                                row.scenario(),
                                Integer.toString(row.threads()),
                                Integer.toString(row.totalDocs()),
                                format(row.docsPerSecond()),
                                format(row.avgMillisPerDoc())))
                        .toList());

        System.out.println("Median benchmark report");
        System.out.println("Suite: current-speed");
        System.out.println("Profile: " + profile);
        System.out.println("Source runs: " + reportFiles.size());
        System.out.println("Saved JSON median report to " + jsonPath);
        System.out.println("Saved CSV median reports to " + latencyCsv + " and " + throughputCsv);
    }

    private List<CurrentSpeedLatencyMedianRow> aggregateCurrentSpeedLatency(List<ReportFile> reportFiles) {
        List<JsonNode> firstRows = iterable(reportFiles.getFirst().report().path("latency"));
        Map<String, JsonNode> firstByScenario = indexBy(firstRows, "scenario");

        for (ReportFile reportFile : reportFiles) {
            Map<String, JsonNode> currentByScenario = indexBy(iterable(reportFile.report().path("latency")), "scenario");
            if (!firstByScenario.keySet().equals(currentByScenario.keySet())) {
                throw new IllegalArgumentException("Current-speed latency scenarios do not match across repeated runs");
            }
        }

        return firstByScenario.keySet().stream()
                .map(scenario -> {
                    List<JsonNode> rows = reportFiles.stream()
                            .map(reportFile -> indexBy(iterable(reportFile.report().path("latency")), "scenario").get(scenario))
                            .toList();
                    JsonNode exemplar = rows.getFirst();
                    return new CurrentSpeedLatencyMedianRow(
                            scenario,
                            exemplar.path("description").asText(""),
                            median(rows, "avgMillis"),
                            median(rows, "p50Millis"),
                            median(rows, "p95Millis"),
                            median(rows, "maxMillis"),
                            median(rows, "docsPerSecond"),
                            median(rows, "avgKilobytes"),
                            median(rows, "peakHeapMb"));
                })
                .toList();
    }

    private List<CurrentSpeedThroughputMedianRow> aggregateCurrentSpeedThroughput(List<ReportFile> reportFiles) {
        List<JsonNode> firstRows = iterable(reportFiles.getFirst().report().path("throughput"));
        Map<String, JsonNode> firstByScenario = indexThroughput(firstRows);

        for (ReportFile reportFile : reportFiles) {
            Map<String, JsonNode> currentByScenario = indexThroughput(iterable(reportFile.report().path("throughput")));
            if (!firstByScenario.keySet().equals(currentByScenario.keySet())) {
                throw new IllegalArgumentException("Current-speed throughput scenarios do not match across repeated runs");
            }
        }

        return firstByScenario.keySet().stream()
                .map(key -> {
                    List<JsonNode> rows = reportFiles.stream()
                            .map(reportFile -> indexThroughput(iterable(reportFile.report().path("throughput"))).get(key))
                            .toList();
                    JsonNode exemplar = rows.getFirst();
                    return new CurrentSpeedThroughputMedianRow(
                            exemplar.path("scenario").asText(),
                            exemplar.path("threads").asInt(),
                            exemplar.path("totalDocs").asInt(),
                            median(rows, "docsPerSecond"),
                            median(rows, "avgMillisPerDoc"));
                })
                .toList();
    }

    private void aggregateComparative(List<ReportFile> reportFiles) throws Exception {
        int warmupIterations = requireIntConsistency(reportFiles, "warmupIterations");
        int measurementIterations = requireIntConsistency(reportFiles, "measurementIterations");

        List<JsonNode> firstRows = iterable(reportFiles.getFirst().report().path("libraries"));
        Map<String, JsonNode> firstByLibrary = indexBy(firstRows, "library");
        for (ReportFile reportFile : reportFiles) {
            Map<String, JsonNode> currentByLibrary = indexBy(iterable(reportFile.report().path("libraries")), "library");
            if (!firstByLibrary.keySet().equals(currentByLibrary.keySet())) {
                throw new IllegalArgumentException("Comparative libraries do not match across repeated runs");
            }
        }

        List<ComparativeMedianRow> rows = firstByLibrary.keySet().stream()
                .map(library -> {
                    List<JsonNode> libraryRows = reportFiles.stream()
                            .map(reportFile -> indexBy(iterable(reportFile.report().path("libraries")), "library").get(library))
                            .toList();
                    return new ComparativeMedianRow(
                            library,
                            median(libraryRows, "avgTimeMs"),
                            median(libraryRows, "avgHeapMb"));
                })
                .toList();

        ComparativeMedianReport report = new ComparativeMedianReport(
                LocalDateTime.now().format(TIMESTAMP_FORMAT),
                warmupIterations,
                measurementIterations,
                rows,
                "median",
                reportFiles.size(),
                reportFiles.stream().map(reportFile -> reportFile.path().toString()).toList());

        BenchmarkReportWriter.BenchmarkArtifacts artifacts = BenchmarkReportWriter.prepare("aggregates/comparative");
        Path jsonPath = artifacts.writeJson(report);
        Path csvPath = artifacts.writeCsv(
                "libraries",
                List.of("library", "avg_time_ms", "avg_heap_mb"),
                rows.stream()
                        .map(row -> List.of(
                                row.library(),
                                format(row.avgTimeMs()),
                                format(row.avgHeapMb())))
                        .toList());

        System.out.println("Median benchmark report");
        System.out.println("Suite: comparative");
        System.out.println("Source runs: " + reportFiles.size());
        System.out.println("Saved JSON median report to " + jsonPath);
        System.out.println("Saved CSV median report to " + csvPath);
    }

    private static int requireIntConsistency(List<ReportFile> reportFiles, String fieldName) {
        int expected = reportFiles.getFirst().report().path(fieldName).asInt();
        for (ReportFile reportFile : reportFiles) {
            int current = reportFile.report().path(fieldName).asInt();
            if (current != expected) {
                throw new IllegalArgumentException(
                        "Benchmark field '" + fieldName + "' does not match across repeated runs: expected "
                                + expected + " but found " + current + " in " + reportFile.path());
            }
        }
        return expected;
    }

    private static List<Integer> requireIntegerArrayConsistency(List<ReportFile> reportFiles, String fieldName) {
        List<Integer> expected = iterable(reportFiles.getFirst().report().path(fieldName)).stream()
                .map(JsonNode::asInt)
                .toList();
        for (ReportFile reportFile : reportFiles) {
            List<Integer> current = iterable(reportFile.report().path(fieldName)).stream()
                    .map(JsonNode::asInt)
                    .toList();
            if (!expected.equals(current)) {
                throw new IllegalArgumentException(
                        "Benchmark field '" + fieldName + "' does not match across repeated runs: expected "
                                + expected + " but found " + current + " in " + reportFile.path());
            }
        }
        return expected;
    }

    private static double median(List<JsonNode> rows, String fieldName) {
        return median(rows.stream().mapToDouble(row -> row.path(fieldName).asDouble()).toArray());
    }

    static double median(double[] values) {
        double[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);
        int middle = sorted.length / 2;
        if (sorted.length % 2 == 1) {
            return sorted[middle];
        }
        return (sorted[middle - 1] + sorted[middle]) / 2.0;
    }

    private static Map<String, JsonNode> indexBy(List<JsonNode> rows, String fieldName) {
        return rows.stream()
                .collect(Collectors.toMap(
                        row -> row.path(fieldName).asText(),
                        row -> row,
                        (left, right) -> left,
                        TreeMap::new));
    }

    private static Map<String, JsonNode> indexThroughput(List<JsonNode> rows) {
        return rows.stream()
                .collect(Collectors.toMap(
                        row -> row.path("scenario").asText() + "#" + row.path("threads").asInt(),
                        row -> row,
                        (left, right) -> left,
                        TreeMap::new));
    }

    private static List<JsonNode> iterable(JsonNode array) {
        List<JsonNode> rows = new ArrayList<>();
        array.forEach(rows::add);
        return rows;
    }

    private static Input resolveInput(String[] args) {
        if (args.length < 3) {
            throw new IllegalArgumentException("""
                    Usage:
                      java ... com.demcha.compose.BenchmarkMedianTool current-speed <run1.json> <run2.json> [...]
                      java ... com.demcha.compose.BenchmarkMedianTool comparative <run1.json> <run2.json> [...]
                    """);
        }

        SuiteType suiteType = SuiteType.from(args[0]);
        List<Path> reportPaths = Arrays.stream(args)
                .skip(1)
                .map(Path::of)
                .sorted(Comparator.comparing(Path::getFileName))
                .toList();
        return new Input(suiteType, reportPaths);
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

    private static String format(double value) {
        return "%.2f".formatted(value);
    }

    private record Input(SuiteType suiteType, List<Path> reportPaths) {
    }

    private record ReportFile(Path path, JsonNode report) {
    }

    private enum SuiteType {
        CURRENT_SPEED("current-speed"),
        COMPARATIVE("comparative");

        private final String id;

        SuiteType(String id) {
            this.id = id;
        }

        String id() {
            return id;
        }

        static SuiteType from(String raw) {
            for (SuiteType value : values()) {
                if (value.id.equalsIgnoreCase(raw)) {
                    return value;
                }
            }
            throw new IllegalArgumentException("Unknown benchmark suite: " + raw);
        }
    }

    private record CurrentSpeedLatencyMedianRow(String scenario,
                                                String description,
                                                double avgMillis,
                                                double p50Millis,
                                                double p95Millis,
                                                double maxMillis,
                                                double docsPerSecond,
                                                double avgKilobytes,
                                                double peakHeapMb) {
    }

    private record CurrentSpeedThroughputMedianRow(String scenario,
                                                   int threads,
                                                   int totalDocs,
                                                   double docsPerSecond,
                                                   double avgMillisPerDoc) {
    }

    private record CurrentSpeedMedianReport(String timestamp,
                                            String profile,
                                            int warmupIterations,
                                            int measurementIterations,
                                            int docsPerThread,
                                            List<Integer> threadCounts,
                                            List<CurrentSpeedLatencyMedianRow> latency,
                                            List<CurrentSpeedThroughputMedianRow> throughput,
                                            long totalBytes,
                                            String aggregation,
                                            int sourceCount,
                                            List<String> sourceRuns) {
    }

    private record ComparativeMedianRow(String library, double avgTimeMs, double avgHeapMb) {
    }

    private record ComparativeMedianReport(String timestamp,
                                           int warmupIterations,
                                           int measurementIterations,
                                           List<ComparativeMedianRow> libraries,
                                           String aggregation,
                                           int sourceCount,
                                           List<String> sourceRuns) {
    }
}
