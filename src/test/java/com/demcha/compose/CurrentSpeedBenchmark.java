package com.demcha.compose;

import com.demcha.compose.layout_core.components.components_builders.ComponentBuilder;
import com.demcha.compose.layout_core.components.content.header_footer.HeaderFooterConfig;
import com.demcha.compose.layout_core.components.content.metadata.DocumentMetadata;
import com.demcha.compose.layout_core.components.content.text.TextDecoration;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.content.watermark.WatermarkConfig;
import com.demcha.compose.layout_core.components.content.watermark.WatermarkLayer;
import com.demcha.compose.layout_core.components.content.watermark.WatermarkPosition;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.ComponentColor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.mock.InvoiceDataFixtures;
import com.demcha.mock.MainPageCVMock;
import com.demcha.mock.ProposalDataFixtures;
import com.demcha.templates.api.MainPageCvDTO;
import com.demcha.templates.builtins.CvTemplateV1;
import com.demcha.templates.builtins.InvoiceTemplateV1;
import com.demcha.templates.builtins.ProposalTemplateV1;
import com.demcha.templates.data.InvoiceData;
import com.demcha.templates.data.MainPageCV;
import com.demcha.templates.data.ProposalData;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Focused local benchmark harness for current GraphCompose performance.
 *
 * <p>This suite is intentionally manual. It is meant to answer "how fast is the
 * library right now?" without adding flaky timing assertions to the normal test
 * suite.</p>
 *
 * <p>Default scenarios cover:</p>
 * <ul>
 *     <li>a small engine-level one-page document</li>
 *     <li>the built-in invoice template</li>
 *     <li>the built-in CV template</li>
 *     <li>a longer multi-page proposal template</li>
 *     <li>a feature-rich document with QR/barcode, watermark, page break, and footer</li>
 * </ul>
 */
public final class CurrentSpeedBenchmark {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int DEFAULT_FULL_WARMUP_ITERATIONS = 12;
    private static final int DEFAULT_FULL_MEASUREMENT_ITERATIONS = 40;
    private static final int DEFAULT_FULL_DOCS_PER_THREAD = 12;
    private static final String DEFAULT_FULL_THREAD_COUNTS = "1,2,4,8";
    private static final int DEFAULT_SMOKE_WARMUP_ITERATIONS = 2;
    private static final int DEFAULT_SMOKE_MEASUREMENT_ITERATIONS = 5;
    private static final int DEFAULT_SMOKE_DOCS_PER_THREAD = 0;
    private static final String DEFAULT_SMOKE_THREAD_COUNTS = "";
    private static final String REPOSITORY_URL = "https://github.com/DemchaAV/GraphCompose";

    private static final TextStyle TITLE_STYLE = TextStyle.builder()
            .size(20)
            .decoration(TextDecoration.BOLD)
            .color(new Color(18, 40, 74))
            .build();
    private static final TextStyle BODY_STYLE = TextStyle.builder()
            .size(9.5)
            .decoration(TextDecoration.DEFAULT)
            .color(new Color(58, 69, 84))
            .build();

    private final InvoiceTemplateV1 invoiceTemplate = new InvoiceTemplateV1();
    private final CvTemplateV1 cvTemplate = new CvTemplateV1();
    private final ProposalTemplateV1 proposalTemplate = new ProposalTemplateV1();
    private final InvoiceData invoiceData = InvoiceDataFixtures.standardInvoice();
    private final ProposalData proposalData = ProposalDataFixtures.longProposal();
    private final MainPageCV originalCv = new MainPageCVMock().getMainPageCV();
    private final MainPageCvDTO rewrittenCv = MainPageCvDTO.from(originalCv);

    public static void main(String[] args) throws Exception {
        BenchmarkSupport.configureQuietLogging();
        new CurrentSpeedBenchmark().run();
    }

    private void run() throws Exception {
        BenchmarkProfile profile = BenchmarkProfile.from(System.getProperty("graphcompose.benchmark.profile", "full"));
        BenchmarkConfig config = resolveConfig(profile);
        boolean enforceGate = Boolean.parseBoolean(System.getProperty(
                "graphcompose.benchmark.enforceGate",
                Boolean.toString(profile == BenchmarkProfile.SMOKE)));

        System.out.println("GraphCompose Current Speed Benchmark");
        System.out.println("Timestamp: " + LocalDateTime.now().format(TIMESTAMP_FORMAT));
        System.out.println("Profile: " + profile.id());
        System.out.println("Warmup iterations: " + config.warmupIterations());
        System.out.println("Measurement iterations: " + config.measurementIterations());
        System.out.println("Docs per thread (throughput): " + config.docsPerThread());
        System.out.println("Thread counts: " + Arrays.toString(config.threadCounts()));
        System.out.println("Perf gate: " + (enforceGate ? "enabled" : "disabled"));
        System.out.println();

        List<Scenario> scenarios = List.of(
                new Scenario("engine-simple", "One-page engine composition", this::renderEngineSimpleDocument),
                new Scenario("invoice-template", "Compose-first invoice template", this::renderInvoiceTemplateDocument),
                new Scenario("cv-template", "Compose-first CV template", this::renderCvTemplateDocument),
                new Scenario("proposal-template", "Long multi-page proposal template", this::renderProposalTemplateDocument),
                new Scenario("feature-rich", "QR, barcode, watermark, header/footer, page break", this::renderFeatureRichDocument)
        );

        System.out.println("Latency benchmark");
        System.out.printf("%-18s | %10s | %10s | %10s | %10s | %11s | %10s | %10s%n",
                "Scenario", "Avg ms", "p50 ms", "p95 ms", "Max ms", "Docs/sec", "Avg KB", "Peak MB");
        System.out.println("-".repeat(111));

        long totalBenchmarkBytes = 0;
        List<LatencyRow> latencyRows = new ArrayList<>();
        for (Scenario scenario : scenarios) {
            Result result = benchmarkScenario(scenario, config.warmupIterations(), config.measurementIterations());
            totalBenchmarkBytes += result.totalBytes();
            latencyRows.add(new LatencyRow(
                    scenario.name(),
                    scenario.description(),
                    round(result.avgMillis()),
                    round(result.p50Millis()),
                    round(result.p95Millis()),
                    round(result.maxMillis()),
                    round(result.docsPerSecond()),
                    round(result.avgKilobytes()),
                    round(result.peakHeapMb())));
            printLatencyRow(scenario, result);
        }

        List<ThroughputRow> throughputRows = new ArrayList<>();
        if (profile.includesThroughput()) {
            System.out.println();
            System.out.println("Parallel throughput benchmark");
            System.out.println("Scenario: invoice-template");
            System.out.printf("%-10s | %12s | %16s | %14s%n",
                    "Threads", "Total docs", "Throughput", "Avg doc ms");
            System.out.println("-".repeat(64));

            for (int threadCount : config.threadCounts()) {
                ThroughputResult result = runThroughputBenchmark(
                        "invoice-template",
                        this::renderInvoiceTemplateDocument,
                        threadCount,
                        config.docsPerThread());
                totalBenchmarkBytes += result.totalBytes();
                throughputRows.add(new ThroughputRow(
                        result.scenarioName(),
                        threadCount,
                        result.totalDocs(),
                        round(result.docsPerSecond()),
                        round(result.avgMillisPerDoc())));
                System.out.printf("%-10d | %12d | %13.2f/s | %14.2f%n",
                        threadCount,
                        result.totalDocs(),
                        result.docsPerSecond(),
                        result.avgMillisPerDoc());
            }
        }

        System.out.println();
        System.out.println("Benchmark byte guard: " + totalBenchmarkBytes);

        BenchmarkReportWriter.BenchmarkArtifacts artifacts = BenchmarkReportWriter.prepare("current-speed");
        PathSummary summary = writeReports(
                artifacts,
                profile.id(),
                config.warmupIterations(),
                config.measurementIterations(),
                config.docsPerThread(),
                config.threadCounts(),
                latencyRows,
                throughputRows,
                totalBenchmarkBytes);
        System.out.println("Saved JSON benchmark report to " + summary.jsonPath());
        System.out.println("Saved CSV benchmark reports to " + summary.latencyCsvPath() + " and " + summary.throughputCsvPath());

        if (enforceGate) {
            PerformanceGateResult gateResult = evaluatePerformanceGate(profile, latencyRows);
            System.out.println(gateResult.message());
            if (!gateResult.passed()) {
                throw new IllegalStateException(gateResult.message());
            }
        }
    }

    private Result benchmarkScenario(Scenario scenario, int warmupIterations, int measurementIterations) throws Exception {
        for (int i = 0; i < warmupIterations; i++) {
            scenario.renderer().render();
        }

        long[] durationsNs = new long[measurementIterations];
        long totalBytes = 0;
        long peakHeapBytes = 0;

        for (int i = 0; i < measurementIterations; i++) {
            long start = System.nanoTime();
            byte[] pdfBytes = scenario.renderer().render();
            long end = System.nanoTime();

            durationsNs[i] = end - start;
            totalBytes += pdfBytes.length;
            peakHeapBytes = Math.max(peakHeapBytes, usedHeapBytes());
        }

        long[] sorted = Arrays.copyOf(durationsNs, durationsNs.length);
        Arrays.sort(sorted);

        double avgNs = Arrays.stream(durationsNs).average().orElse(0.0);
        return new Result(
                avgNs / 1_000_000.0,
                percentileMillis(sorted, 0.50),
                percentileMillis(sorted, 0.95),
                sorted[sorted.length - 1] / 1_000_000.0,
                1_000_000_000.0 / avgNs,
                (totalBytes / (double) measurementIterations) / 1024.0,
                bytesToMegabytes(peakHeapBytes),
                totalBytes
        );
    }

    private ThroughputResult runThroughputBenchmark(String scenarioName,
                                                    Renderer renderer,
                                                    int threadCount,
                                                    int docsPerThread) throws Exception {
        int totalDocs = threadCount * docsPerThread;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        long totalBytes = 0;
        long start = System.nanoTime();
        try {
            List<Future<Integer>> futures = executor.invokeAll(
                    java.util.stream.IntStream.range(0, totalDocs)
                            .<Callable<Integer>>mapToObj(i -> () -> renderer.render().length)
                            .toList());

            for (Future<Integer> future : futures) {
                totalBytes += future.get();
            }
        } finally {
            executor.shutdown();
        }

        long end = System.nanoTime();
        double durationSeconds = (end - start) / 1_000_000_000.0;
        return new ThroughputResult(
                scenarioName,
                totalDocs,
                totalDocs / durationSeconds,
                (durationSeconds * 1_000.0) / totalDocs,
                totalBytes
        );
    }

    private void printLatencyRow(Scenario scenario, Result result) {
        System.out.printf("%-18s | %10.2f | %10.2f | %10.2f | %10.2f | %11.2f | %10.2f | %10.2f%n",
                scenario.name(),
                result.avgMillis(),
                result.p50Millis(),
                result.p95Millis(),
                result.maxMillis(),
                result.docsPerSecond(),
                result.avgKilobytes(),
                result.peakHeapMb());
    }

    private double percentileMillis(long[] sortedDurationsNs, double percentile) {
        int index = Math.min(sortedDurationsNs.length - 1, (int) Math.floor(sortedDurationsNs.length * percentile));
        return sortedDurationsNs[index] / 1_000_000.0;
    }

    private int[] parseThreadCounts(String rawValue) {
        return Arrays.stream(rawValue.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .mapToInt(Integer::parseInt)
                .filter(value -> value > 0)
                .toArray();
    }

    private BenchmarkConfig resolveConfig(BenchmarkProfile profile) {
        int defaultWarmupIterations = profile == BenchmarkProfile.SMOKE
                ? DEFAULT_SMOKE_WARMUP_ITERATIONS
                : DEFAULT_FULL_WARMUP_ITERATIONS;
        int defaultMeasurementIterations = profile == BenchmarkProfile.SMOKE
                ? DEFAULT_SMOKE_MEASUREMENT_ITERATIONS
                : DEFAULT_FULL_MEASUREMENT_ITERATIONS;
        int defaultDocsPerThread = profile == BenchmarkProfile.SMOKE
                ? DEFAULT_SMOKE_DOCS_PER_THREAD
                : DEFAULT_FULL_DOCS_PER_THREAD;
        String defaultThreadCounts = profile == BenchmarkProfile.SMOKE
                ? DEFAULT_SMOKE_THREAD_COUNTS
                : DEFAULT_FULL_THREAD_COUNTS;

        return new BenchmarkConfig(
                Integer.getInteger("graphcompose.benchmark.warmup", defaultWarmupIterations),
                Integer.getInteger("graphcompose.benchmark.iterations", defaultMeasurementIterations),
                Integer.getInteger("graphcompose.benchmark.docsPerThread", defaultDocsPerThread),
                parseThreadCounts(System.getProperty("graphcompose.benchmark.threads", defaultThreadCounts)));
    }

    private PerformanceGateResult evaluatePerformanceGate(BenchmarkProfile profile, List<LatencyRow> latencyRows) {
        if (profile != BenchmarkProfile.SMOKE) {
            return new PerformanceGateResult(true, "Performance gate skipped for profile " + profile.id());
        }

        List<String> failures = new ArrayList<>();
        for (LatencyRow row : latencyRows) {
            SmokeThreshold threshold = profile.smokeThresholds().get(row.scenario());
            if (threshold == null) {
                continue;
            }

            double maxAvgMillis = Double.parseDouble(System.getProperty(
                    "graphcompose.benchmark.gate." + row.scenario() + ".maxAvgMillis",
                    Double.toString(threshold.maxAvgMillis())));
            double maxPeakHeapMb = Double.parseDouble(System.getProperty(
                    "graphcompose.benchmark.gate." + row.scenario() + ".maxPeakHeapMb",
                    Double.toString(threshold.maxPeakHeapMb())));

            if (row.avgMillis() > maxAvgMillis) {
                failures.add(row.scenario() + " avg " + format(row.avgMillis()) + " ms > " + format(maxAvgMillis) + " ms");
            }
            if (row.peakHeapMb() > maxPeakHeapMb) {
                failures.add(row.scenario() + " peak heap " + format(row.peakHeapMb()) + " MB > " + format(maxPeakHeapMb) + " MB");
            }
        }

        if (failures.isEmpty()) {
            return new PerformanceGateResult(true, "Performance gate passed for profile " + profile.id());
        }

        return new PerformanceGateResult(
                false,
                "Performance gate failed for profile " + profile.id() + ": " + String.join("; ", failures));
    }

    private long usedHeapBytes() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private double bytesToMegabytes(long bytes) {
        return bytes / (1024.0 * 1024.0);
    }

    private byte[] renderEngineSimpleDocument() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(PDRectangle.A4)
                .margin(24, 24, 24, 24)
                .markdown(true)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();
            double width = composer.canvas().innerWidth();

            cb.vContainer(Align.middle(10))
                    .entityName("BenchmarkSimpleRoot")
                    .size(width, 0)
                    .anchor(Anchor.topLeft())
                    .margin(Margin.of(4))
                    .addChild(cb.text()
                            .entityName("BenchmarkSimpleTitle")
                            .textWithAutoSize("GraphCompose Speed Check")
                            .textStyle(TITLE_STYLE)
                            .anchor(Anchor.topLeft())
                            .build())
                    .addChild(cb.blockText(Align.left(4), BODY_STYLE)
                            .entityName("BenchmarkSimpleBody")
                            .size(width, 2)
                            .anchor(Anchor.topLeft())
                            .padding(Padding.of(4))
                            .text(List.of(
                                    "This is a compact benchmark scenario that exercises the ordinary engine path: a root flow container, heading text, paragraph layout, and final PDF serialization."),
                                    BODY_STYLE,
                                    Padding.zero(),
                                    Margin.zero())
                            .build())
                    .addChild(cb.divider()
                            .entityName("BenchmarkSimpleDivider")
                            .width(width)
                            .thickness(1)
                            .color(ComponentColor.LIGHT_GRAY)
                            .build())
                    .build();

            return composer.toBytes();
        }
    }

    private byte[] renderInvoiceTemplateDocument() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(PDRectangle.A4)
                .margin(22, 22, 22, 22)
                .markdown(true)
                .create()) {
            invoiceTemplate.compose(composer, invoiceData);
            return composer.toBytes();
        }
    }

    private byte[] renderCvTemplateDocument() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(PDRectangle.A4)
                .margin(15, 10, 15, 15)
                .markdown(true)
                .create()) {
            cvTemplate.compose(composer, originalCv, rewrittenCv);
            return composer.toBytes();
        }
    }

    private byte[] renderProposalTemplateDocument() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(PDRectangle.A4)
                .margin(22, 22, 22, 22)
                .markdown(true)
                .create()) {
            proposalTemplate.compose(composer, proposalData);
            return composer.toBytes();
        }
    }

    private byte[] renderFeatureRichDocument() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(PDRectangle.A4)
                .margin(28, 28, 42, 28)
                .markdown(true)
                .create()) {

            composer.metadata(DocumentMetadata.builder()
                    .title("GraphCompose Feature Benchmark")
                    .author("CurrentSpeedBenchmark")
                    .subject("Performance benchmark for feature-rich output")
                    .keywords("benchmark, barcode, qr, watermark")
                    .build());
            composer.watermark(WatermarkConfig.builder()
                    .text("BENCHMARK")
                    .fontSize(78)
                    .rotation(40)
                    .opacity(0.07f)
                    .color(new Color(54, 92, 131))
                    .layer(WatermarkLayer.BEHIND_CONTENT)
                    .position(WatermarkPosition.CENTER)
                    .build());
            composer.header("GraphCompose", "Feature Benchmark", "{date}");
            composer.footer(HeaderFooterConfig.builder()
                    .leftText("benchmark")
                    .centerText("Page {page} of {pages}")
                    .rightText("feature-rich")
                    .showSeparator(true)
                    .build());

            ComponentBuilder cb = composer.componentBuilder();
            double width = composer.canvas().innerWidth();
            var root = cb.vContainer(Align.middle(12))
                    .entityName("BenchmarkFeatureRoot")
                    .size(width, 0)
                    .anchor(Anchor.topLeft());

            root.addChild(cb.text()
                    .entityName("BenchmarkFeatureTitle")
                    .textWithAutoSize("Feature-Rich Benchmark")
                    .textStyle(TITLE_STYLE)
                    .anchor(Anchor.topLeft())
                    .build());

            root.addChild(cb.blockText(Align.left(4), BODY_STYLE)
                    .entityName("BenchmarkFeatureLead")
                    .size(width, 2)
                    .anchor(Anchor.topLeft())
                    .padding(Padding.of(4))
                    .text(List.of(
                                    "This scenario intentionally mixes barcode output, document metadata, repeating chrome, and a forced page break so the benchmark reflects more than raw text rendering."),
                            BODY_STYLE,
                            Padding.zero(),
                            Margin.zero())
                    .build());

            root.addChild(cb.barcode()
                    .entityName("BenchmarkFeatureQr")
                    .data(REPOSITORY_URL)
                    .qrCode()
                    .size(108, 108)
                    .foreground(new Color(18, 40, 74))
                    .anchor(Anchor.topCenter())
                    .build());

            root.addChild(cb.barcode()
                    .entityName("BenchmarkFeatureCode128")
                    .data("GC-BENCH-2026-04-13")
                    .code128()
                    .size(280, 68)
                    .quietZone(4)
                    .anchor(Anchor.topCenter())
                    .build());

            root.addChild(cb.pageBreak()
                    .entityName("BenchmarkFeatureBreak")
                    .build());

            for (int i = 1; i <= 6; i++) {
                root.addChild(cb.blockText(Align.left(4), BODY_STYLE)
                        .entityName("BenchmarkFeatureParagraph" + i)
                        .size(width, 2)
                        .anchor(Anchor.topLeft())
                        .padding(Padding.of(4))
                        .text(List.of(repeatedParagraph(i)),
                                BODY_STYLE,
                                Padding.zero(),
                                Margin.zero())
                        .build());
            }

            root.build();
            return composer.toBytes();
        }
    }

    private String repeatedParagraph(int index) {
        return "Paragraph %d keeps the document flowing across pages so the benchmark captures layout and pagination work rather than only one-screen rendering. "
                .formatted(index)
                + "GraphCompose should continue the content while keeping header/footer placement stable. ".repeat(4)
                + "This also keeps the feature-rich scenario representative of reporting and proposal-style output.";
    }

    private PathSummary writeReports(BenchmarkReportWriter.BenchmarkArtifacts artifacts,
                                     String profileId,
                                     int warmupIterations,
                                     int measurementIterations,
                                     int docsPerThread,
                                     int[] threadCounts,
                                     List<LatencyRow> latencyRows,
                                     List<ThroughputRow> throughputRows,
                                     long totalBenchmarkBytes) throws Exception {
        CurrentSpeedReport report = new CurrentSpeedReport(
                LocalDateTime.now().format(TIMESTAMP_FORMAT),
                profileId,
                warmupIterations,
                measurementIterations,
                docsPerThread,
                Arrays.stream(threadCounts).boxed().toList(),
                latencyRows,
                throughputRows,
                totalBenchmarkBytes);

        var jsonPath = artifacts.writeJson(report);
        var latencyCsvPath = artifacts.writeCsv(
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
        var throughputCsvPath = artifacts.writeCsv(
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

        return new PathSummary(jsonPath.toString(), latencyCsvPath.toString(), throughputCsvPath.toString());
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static String format(double value) {
        return "%.2f".formatted(value);
    }

    private record Scenario(String name, String description, Renderer renderer) {
    }

    @FunctionalInterface
    private interface Renderer {
        byte[] render() throws Exception;
    }

    private record Result(double avgMillis,
                          double p50Millis,
                          double p95Millis,
                          double maxMillis,
                          double docsPerSecond,
                          double avgKilobytes,
                          double peakHeapMb,
                          long totalBytes) {
    }

    private record ThroughputResult(String scenarioName,
                                    int totalDocs,
                                    double docsPerSecond,
                                    double avgMillisPerDoc,
                                    long totalBytes) {
    }

    private record LatencyRow(String scenario,
                              String description,
                              double avgMillis,
                              double p50Millis,
                              double p95Millis,
                              double maxMillis,
                              double docsPerSecond,
                              double avgKilobytes,
                              double peakHeapMb) {
    }

    private record ThroughputRow(String scenario,
                                 int threads,
                                 int totalDocs,
                                 double docsPerSecond,
                                 double avgMillisPerDoc) {
    }

    private record CurrentSpeedReport(String timestamp,
                                      String profile,
                                      int warmupIterations,
                                      int measurementIterations,
                                      int docsPerThread,
                                      List<Integer> threadCounts,
                                      List<LatencyRow> latency,
                                      List<ThroughputRow> throughput,
                                      long totalBytes) {
    }

    private record PathSummary(String jsonPath, String latencyCsvPath, String throughputCsvPath) {
    }

    private record BenchmarkConfig(int warmupIterations,
                                   int measurementIterations,
                                   int docsPerThread,
                                   int[] threadCounts) {
    }

    private record SmokeThreshold(double maxAvgMillis, double maxPeakHeapMb) {
    }

    private record PerformanceGateResult(boolean passed, String message) {
    }

    private enum BenchmarkProfile {
        FULL("full", true, Map.of()),
        SMOKE("smoke", false, Map.of(
                "engine-simple", new SmokeThreshold(800.0, 512.0),
                "invoice-template", new SmokeThreshold(1200.0, 640.0),
                "cv-template", new SmokeThreshold(1500.0, 640.0),
                "proposal-template", new SmokeThreshold(2400.0, 768.0),
                "feature-rich", new SmokeThreshold(2600.0, 768.0)
        ));

        private final String id;
        private final boolean includesThroughput;
        private final Map<String, SmokeThreshold> smokeThresholds;

        BenchmarkProfile(String id, boolean includesThroughput, Map<String, SmokeThreshold> smokeThresholds) {
            this.id = id;
            this.includesThroughput = includesThroughput;
            this.smokeThresholds = smokeThresholds;
        }

        String id() {
            return id;
        }

        boolean includesThroughput() {
            return includesThroughput;
        }

        Map<String, SmokeThreshold> smokeThresholds() {
            return smokeThresholds;
        }

        static BenchmarkProfile from(String raw) {
            for (BenchmarkProfile profile : values()) {
                if (profile.id.equalsIgnoreCase(raw)) {
                    return profile;
                }
            }
            throw new IllegalArgumentException("Unknown benchmark profile: " + raw);
        }
    }
}
