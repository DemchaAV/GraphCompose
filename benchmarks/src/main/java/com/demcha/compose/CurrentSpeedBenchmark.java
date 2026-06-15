package com.demcha.compose;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.fixed.pdf.PdfFixedLayoutBackend;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfMetadataOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfWatermarkLayer;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfWatermarkOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfWatermarkPosition;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.builtins.InvoiceTemplateV1;
import com.demcha.compose.document.templates.builtins.ProposalTemplateV1;
import com.demcha.compose.document.templates.cv.presets.ModernProfessional;
import com.demcha.compose.document.templates.cv.spec.CvSpec;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.document.templates.data.proposal.ProposalDocumentSpec;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.compose.engine.components.style.Margin;
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
import java.util.function.Function;

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
    // The 16-thread tier is absorbed from the removed ScalabilityBenchmark so the
    // full profile keeps a thread-scaling data point (smoke runs no throughput).
    private static final String DEFAULT_FULL_THREAD_COUNTS = "1,2,4,8,16";
    // Bumped from 2/5 to 30/100 so smoke runs reach a steady JIT state and the
    // p95 calculation actually has enough samples to interpolate rather than
    // collapsing to the maximum observed time. The smoke profile remains the
    // fast option for CI and pre-commit checks but no longer reports numbers
    // that are 2-3x off cold-start.
    private static final int DEFAULT_SMOKE_WARMUP_ITERATIONS = 30;
    private static final int DEFAULT_SMOKE_MEASUREMENT_ITERATIONS = 100;
    private static final int DEFAULT_SMOKE_DOCS_PER_THREAD = 0;
    private static final String DEFAULT_SMOKE_THREAD_COUNTS = "";
    private static final String REPOSITORY_URL = "https://github.com/DemchaAV/GraphCompose";

    private static final DocumentTextStyle TITLE_STYLE = DocumentTextStyle.builder()
            .size(20)
            .decoration(DocumentTextDecoration.BOLD)
            .color(DocumentColor.of(new Color(18, 40, 74)))
            .build();
    private static final DocumentTextStyle BODY_STYLE = DocumentTextStyle.builder()
            .size(9.5)
            .decoration(DocumentTextDecoration.DEFAULT)
            .color(DocumentColor.of(new Color(58, 69, 84)))
            .build();

    private final InvoiceTemplateV1 invoiceTemplate = new InvoiceTemplateV1();
    private final DocumentTemplate<CvSpec> cvTemplate = ModernProfessional.create(BusinessTheme.modern());
    private final ProposalTemplateV1 proposalTemplate = new ProposalTemplateV1();
    private final InvoiceDocumentSpec invoice = CanonicalBenchmarkSupport.canonicalInvoice();
    private final ProposalDocumentSpec proposal = CanonicalBenchmarkSupport.canonicalProposal();
    private final CvSpec cv = CanonicalBenchmarkSupport.canonicalCv();

    // Canonical scenario list, in table order. Declared statically (the
    // renderer is bound to an instance at run time) so the gate-coverage guard
    // test can read the scenario names without re-measuring: a scenario added
    // here without a matching SMOKE threshold below would silently escape the
    // perf gate, and CurrentSpeedScenarioGateTest fails loudly if that happens.
    private static final List<ScenarioDef> SCENARIO_DEFS = List.of(
            new ScenarioDef("engine-simple", "One-page engine composition",
                    b -> b::renderEngineSimpleDocument),
            new ScenarioDef("invoice-template", "Compose-first invoice template",
                    b -> b::renderInvoiceTemplateDocument),
            new ScenarioDef("cv-template", "Compose-first CV template",
                    b -> b::renderCvTemplateDocument),
            new ScenarioDef("proposal-template", "Long multi-page proposal template",
                    b -> b::renderProposalTemplateDocument),
            new ScenarioDef("feature-rich", "QR, barcode, watermark, header/footer, page break",
                    b -> b::renderFeatureRichDocument),
            new ScenarioDef("long-token", "Long unbreakable tokens (URLs/IDs) forcing character-level wrap",
                    b -> b::renderLongTokenDocument),
            new ScenarioDef("vector-rich", "v1.8 vector surface: bar + pie charts, SVG icons, gradient path",
                    b -> b::renderVectorRichDocument)
    );

    /**
     * Ordered scenario names. Read by {@code CurrentSpeedScenarioGateTest} to
     * assert every scenario is covered by a SMOKE gate threshold.
     *
     * @return the canonical scenario names in table order
     */
    static List<String> scenarioNames() {
        return SCENARIO_DEFS.stream().map(ScenarioDef::name).toList();
    }

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

        List<Scenario> scenarios = SCENARIO_DEFS.stream()
                .map(def -> new Scenario(def.name(), def.description(), def.renderer().apply(this)))
                .toList();

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

        // Stage breakdown: for each template scenario we time compose / layout
        // / render separately so consumers can attribute regressions to the
        // engine vs. PDFBox. Only the template scenarios are probed here; the
        // latency table above still covers every scenario.
        List<StageRow> stageRows = new ArrayList<>();
        if (profile != BenchmarkProfile.SMOKE || config.measurementIterations() >= 20) {
            System.out.println();
            System.out.println("Stage breakdown (median ms per stage)");
            System.out.printf("%-18s | %12s | %12s | %12s | %12s%n",
                    "Scenario", "Compose", "Layout", "Render", "Total");
            System.out.println("-".repeat(78));
            stageRows.add(runStageBreakdown("invoice-template", () -> openInvoiceSession(),
                    s -> invoiceTemplate.compose(s, invoice), config.measurementIterations()));
            stageRows.add(runStageBreakdown("cv-template", () -> openCvSession(),
                    s -> cvTemplate.compose(s, cv), config.measurementIterations()));
            stageRows.add(runStageBreakdown("proposal-template", () -> openProposalSession(),
                    s -> proposalTemplate.compose(s, proposal), config.measurementIterations()));
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
                stageRows,
                throughputRows,
                totalBenchmarkBytes);
        System.out.println("Saved JSON benchmark report to " + summary.jsonPath());
        System.out.println("Saved CSV benchmark reports to " + summary.latencyCsvPath() + ", "
                + summary.stagesCsvPath() + ", and " + summary.throughputCsvPath());
        System.out.println("Saved markdown summary to " + summary.summaryMarkdownPath());

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

        // Best-effort GC stabilization between warmup and measurement so the
        // first iteration does not pay for warmup-era garbage. We do not rely
        // on System.gc() to be honoured, but it consistently lowers variance
        // on HotSpot's stock collectors.
        System.gc();
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long[] durationsNs = new long[measurementIterations];
        long totalBytes = 0;
        long peakHeapBytes = 0;
        long baselineHeapBytes = usedHeapBytes();

        for (int i = 0; i < measurementIterations; i++) {
            long start = System.nanoTime();
            byte[] pdfBytes = scenario.renderer().render();
            long end = System.nanoTime();

            durationsNs[i] = end - start;
            totalBytes += pdfBytes.length;
            // Sample heap before any post-iteration GC pressure relief; the
            // delta over the warmed baseline is a closer approximation of
            // peak-during-iteration than the post-GC plateau used previously.
            long sample = usedHeapBytes();
            peakHeapBytes = Math.max(peakHeapBytes, Math.max(0, sample - baselineHeapBytes));
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

    /**
     * Returns the heap delta sampled during the measurement window.
     *
     * <p>The previous reading subtracted nothing, so the metric reported the
     * full live-heap including warmup-era data and was effectively constant
     * across runs. Samples now record allocation pressure since the GC point
     * between warmup and measurement.</p>
     */
    @SuppressWarnings("unused")
    private void heapMetricNote() {
        // Doc-only marker so tooling that grep'd for the previous metric can
        // see this commit moved peakHeapMb from "post-GC plateau" to
        // "delta vs. warmed baseline".
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

    private DocumentSession openInvoiceSession() {
        return GraphCompose.document()
                .pageSize(com.demcha.compose.document.api.DocumentPageSize.A4)
                .margin(22, 22, 22, 22)
                .create();
    }

    private DocumentSession openCvSession() {
        return GraphCompose.document()
                .pageSize(com.demcha.compose.document.api.DocumentPageSize.A4)
                .margin(15, 10, 15, 15)
                .create();
    }

    private DocumentSession openProposalSession() {
        return GraphCompose.document()
                .pageSize(com.demcha.compose.document.api.DocumentPageSize.A4)
                .margin(22, 22, 22, 22)
                .create();
    }

    @FunctionalInterface
    private interface SessionFactory {
        DocumentSession open();
    }

    @FunctionalInterface
    private interface SessionComposer {
        void compose(DocumentSession session);
    }

    /**
     * Times each pipeline stage for a template scenario across N iterations
     * (with the same warmup count as the latency benchmark) and prints a
     * median-ms-per-stage row so callers can attribute regressions to
     * compose / layout / render independently.
     */
    private StageRow runStageBreakdown(String scenario,
                                       SessionFactory factory,
                                       SessionComposer composer,
                                       int iterations) throws Exception {
        int warmup = Math.max(2, Math.min(20, iterations / 5));
        for (int i = 0; i < warmup; i++) {
            try (DocumentSession session = factory.open()) {
                composer.compose(session);
                session.layoutGraph();
                session.toPdfBytes();
            }
        }
        long[] composeNs = new long[iterations];
        long[] layoutNs = new long[iterations];
        long[] renderNs = new long[iterations];
        long[] totalNs = new long[iterations];
        for (int i = 0; i < iterations; i++) {
            long t0 = System.nanoTime();
            DocumentSession session = factory.open();
            composer.compose(session);
            long t1 = System.nanoTime();
            session.layoutGraph();
            long t2 = System.nanoTime();
            byte[] pdf = session.toPdfBytes();
            long t3 = System.nanoTime();
            session.close();
            long t4 = System.nanoTime();
            composeNs[i] = t1 - t0;
            layoutNs[i] = t2 - t1;
            renderNs[i] = t3 - t2;
            totalNs[i] = t4 - t0;
            if (pdf.length < 0) {
                throw new AssertionError();
            }
        }
        double composeMs = medianMs(composeNs);
        double layoutMs = medianMs(layoutNs);
        double renderMs = medianMs(renderNs);
        double totalMs = medianMs(totalNs);
        System.out.printf("%-18s | %12.3f | %12.3f | %12.3f | %12.3f%n",
                scenario, composeMs, layoutMs, renderMs, totalMs);
        return new StageRow(scenario, round(composeMs), round(layoutMs), round(renderMs), round(totalMs));
    }

    private static double medianMs(long[] arr) {
        long[] sorted = Arrays.copyOf(arr, arr.length);
        Arrays.sort(sorted);
        return sorted[sorted.length / 2] / 1_000_000.0;
    }

    private double percentileMillis(long[] sortedDurationsNs, double percentile) {
        if (sortedDurationsNs.length == 0) {
            return 0.0;
        }
        if (sortedDurationsNs.length == 1) {
            return sortedDurationsNs[0] / 1_000_000.0;
        }
        // Linear interpolation between order statistics. Without it, p95 of
        // five samples collapses to sorted[4] (i.e. the maximum), which is
        // useless for regression detection at small sample counts.
        double rank = (sortedDurationsNs.length - 1) * percentile;
        int lower = (int) Math.floor(rank);
        int upper = (int) Math.ceil(rank);
        if (lower == upper) {
            return sortedDurationsNs[lower] / 1_000_000.0;
        }
        double weight = rank - lower;
        double lowerMs = sortedDurationsNs[lower] / 1_000_000.0;
        double upperMs = sortedDurationsNs[upper] / 1_000_000.0;
        return lowerMs + (upperMs - lowerMs) * weight;
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

    // Package-private and static (uses no instance state) so
    // CurrentSpeedBenchmarkPerfGateTest can drive it with synthetic LatencyRow
    // values instead of real, non-deterministic measurements. LatencyRow,
    // PerformanceGateResult and BenchmarkProfile are package-private for the
    // same reason.
    static PerformanceGateResult evaluatePerformanceGate(BenchmarkProfile profile, List<LatencyRow> latencyRows) {
        if (profile != BenchmarkProfile.SMOKE) {
            return new PerformanceGateResult(true, "Performance gate skipped for profile " + profile.id());
        }

        List<String> failures = new ArrayList<>();
        List<String> advisories = new ArrayList<>();
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
                // peakHeapMb is a GC-timing-noisy used-heap delta, so a breach is
                // reported as advisory rather than failing the gate — matching
                // BenchmarkVerdictTool and avoiding flaky CI from a GC blip. The
                // deterministic memory signal is the allocation-bytes probes.
                advisories.add(row.scenario() + " peak heap " + format(row.peakHeapMb()) + " MB > " + format(maxPeakHeapMb) + " MB");
            }
        }

        String advisoryNote = advisories.isEmpty() ? "" : " (advisory: " + String.join("; ", advisories) + ")";

        if (failures.isEmpty()) {
            return new PerformanceGateResult(true, "Performance gate passed for profile " + profile.id() + advisoryNote);
        }

        return new PerformanceGateResult(
                false,
                "Performance gate failed for profile " + profile.id() + ": " + String.join("; ", failures) + advisoryNote);
    }

    private long usedHeapBytes() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private double bytesToMegabytes(long bytes) {
        return bytes / (1024.0 * 1024.0);
    }

    private byte[] renderEngineSimpleDocument() throws Exception {
        return CanonicalBenchmarkSupport.renderSimpleBenchmarkDocument(
                PDRectangle.A4,
                Margin.of(24),
                "BenchmarkSimpleRoot",
                "GraphCompose Speed Check",
                "This is a compact benchmark scenario that exercises the ordinary engine path: "
                        + "a root flow container, heading text, paragraph layout, and final PDF serialization.");
    }

    private byte[] renderVectorRichDocument() throws Exception {
        DocumentPaint accent = DocumentPaint.linear(
                DocumentColor.rgb(167, 139, 250), DocumentColor.rgb(97, 40, 217));
        SvgIcon icon = SvgIcon.parse(SvgBenchmarkFixtures.MULTI_LAYER_ICON_SVG);
        try (DocumentSession document = GraphCompose.document()
                .pageSize(com.demcha.compose.document.api.DocumentPageSize.A4)
                .margin(28, 28, 28, 28)
                .create()) {
            var flow = document.pageFlow().name("BenchmarkVectorRich").spacing(12);
            flow.addParagraph("v1.8 vector-rich benchmark");
            flow.chart(ChartBenchmarkFixtures.barSpec(), ChartBenchmarkFixtures.barStyle());
            flow.chart(ChartBenchmarkFixtures.pieSpec());
            for (int i = 0; i < 8; i++) {
                flow.addSvgIcon(icon, 32);
            }
            flow.addPath(p -> p.size(220, 28)
                    .moveTo(0.0, 0.5).curveTo(0.25, 1.0, 0.75, 0.0, 1.0, 0.5).fill(accent));
            flow.build();
            return document.toPdfBytes();
        }
    }

    private byte[] renderInvoiceTemplateDocument() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(com.demcha.compose.document.api.DocumentPageSize.A4)
                .margin(22, 22, 22, 22)
                .create()) {
            invoiceTemplate.compose(document, invoice);
            return document.toPdfBytes();
        }
    }

    private byte[] renderCvTemplateDocument() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(com.demcha.compose.document.api.DocumentPageSize.A4)
                .margin(15, 10, 15, 15)
                .create()) {
            cvTemplate.compose(document, cv);
            return document.toPdfBytes();
        }
    }

    private byte[] renderProposalTemplateDocument() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(com.demcha.compose.document.api.DocumentPageSize.A4)
                .margin(22, 22, 22, 22)
                .create()) {
            proposalTemplate.compose(document, proposal);
            return document.toPdfBytes();
        }
    }

    private byte[] renderLongTokenDocument() throws Exception {
        // Worst-case for character-level wrapping: many long unbreakable tokens
        // (long URLs/IDs/no-space runs) that overflow the line and force
        // splitLongToken -> fitCharacters. Exercises audit finding F2.
        try (DocumentSession document = GraphCompose.document()
                .pageSize(com.demcha.compose.document.api.DocumentPageSize.A4)
                .margin(22, 22, 22, 22)
                .create()) {
            var root = document.dsl()
                    .pageFlow()
                    .name("BenchmarkLongTokenRoot")
                    .spacing(8);
            for (int i = 1; i <= 40; i++) {
                final int index = i;
                root.addParagraph(paragraph -> paragraph
                        .name("BenchmarkLongToken" + index)
                        .text("Reference " + index + ": https://example.com/" + "a".repeat(500)
                                + " trailing words to wrap normally after the long token.")
                        .textStyle(BODY_STYLE));
            }
            root.build();
            return document.toPdfBytes();
        }
    }

    private byte[] renderFeatureRichDocument() throws Exception {
        PdfFixedLayoutBackend backend = PdfFixedLayoutBackend.builder()
                .metadata(PdfMetadataOptions.builder()
                        .title("GraphCompose Feature Benchmark")
                        .author("CurrentSpeedBenchmark")
                        .subject("Performance benchmark for feature-rich output")
                        .keywords("benchmark, barcode, qr, watermark")
                        .build())
                .watermark(PdfWatermarkOptions.builder()
                        .text("BENCHMARK")
                        .fontSize(78)
                        .rotation(40)
                        .opacity(0.07f)
                        .color(new Color(54, 92, 131))
                        .layer(PdfWatermarkLayer.BEHIND_CONTENT)
                        .position(PdfWatermarkPosition.CENTER)
                        .build())
                .header(PdfHeaderFooterOptions.builder()
                        .leftText("GraphCompose")
                        .centerText("Feature Benchmark")
                        .rightText("{date}")
                        .build())
                .footer(PdfHeaderFooterOptions.builder()
                        .leftText("benchmark")
                        .centerText("Page {page} of {pages}")
                        .rightText("feature-rich")
                        .showSeparator(true)
                        .build())
                .build();

        try (DocumentSession document = GraphCompose.document()
                .pageSize(com.demcha.compose.document.api.DocumentPageSize.A4)
                .margin(28, 28, 42, 28)
                .create()) {

            var root = document.dsl()
                    .pageFlow()
                    .name("BenchmarkFeatureRoot")
                    .spacing(12);

            root.addParagraph(paragraph -> paragraph
                    .name("BenchmarkFeatureTitle")
                    .text("Feature-Rich Benchmark")
                    .textStyle(TITLE_STYLE));

            root.addParagraph(paragraph -> paragraph
                    .name("BenchmarkFeatureLead")
                    .text("This scenario intentionally mixes barcode output, document metadata, repeating chrome, "
                            + "and a forced page break so the benchmark reflects more than raw text rendering.")
                    .textStyle(BODY_STYLE)
                    .lineSpacing(4)
                    .padding(4, 4, 4, 4));

            root.addBarcode(barcode -> barcode
                    .name("BenchmarkFeatureQr")
                    .data(REPOSITORY_URL)
                    .qrCode()
                    .size(108, 108)
                    .foreground(new Color(18, 40, 74)));

            root.addBarcode(barcode -> barcode
                    .name("BenchmarkFeatureCode128")
                    .data("GC-BENCH-2026-04-13")
                    .code128()
                    .size(280, 68)
                    .quietZone(4));

            root.addPageBreak(pageBreak -> pageBreak.name("BenchmarkFeatureBreak"));

            for (int i = 1; i <= 6; i++) {
                final int paragraphIndex = i;
                root.addParagraph(paragraph -> paragraph
                        .name("BenchmarkFeatureParagraph" + paragraphIndex)
                        .text(repeatedParagraph(paragraphIndex))
                        .textStyle(BODY_STYLE)
                        .lineSpacing(4)
                        .padding(4, 4, 4, 4));
            }

            root.build();
            return document.render(backend);
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
                                     List<StageRow> stageRows,
                                     List<ThroughputRow> throughputRows,
                                     long totalBenchmarkBytes) throws Exception {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        CurrentSpeedReport report = new CurrentSpeedReport(
                timestamp,
                profileId,
                warmupIterations,
                measurementIterations,
                docsPerThread,
                Arrays.stream(threadCounts).boxed().toList(),
                latencyRows,
                stageRows,
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
        var stagesCsvPath = artifacts.writeCsv(
                "stages",
                List.of("scenario", "compose_ms", "layout_ms", "render_ms", "total_ms"),
                stageRows.stream()
                        .map(row -> List.of(
                                row.scenario(),
                                format(row.composeMillis()),
                                format(row.layoutMillis()),
                                format(row.renderMillis()),
                                format(row.totalMillis())))
                        .toList());
        var summaryMarkdownPath = artifacts.writeMarkdown(
                "summary",
                buildSummaryMarkdown(timestamp, profileId, latencyRows, stageRows,
                        throughputRows, totalBenchmarkBytes));

        return new PathSummary(jsonPath.toString(), latencyCsvPath.toString(),
                stagesCsvPath.toString(), throughputCsvPath.toString(),
                summaryMarkdownPath.toString());
    }

    /**
     * Renders a single human-readable summary of the run — the latency table,
     * the per-stage compose/layout/render split (the only place the suite
     * attributes time to engine stages vs. PDFBox), and the throughput table
     * when present — so a reviewer reads one file instead of stitching the JSON
     * and several CSVs together.
     */
    private static String buildSummaryMarkdown(String timestamp,
                                               String profileId,
                                               List<LatencyRow> latencyRows,
                                               List<StageRow> stageRows,
                                               List<ThroughputRow> throughputRows,
                                               long totalBenchmarkBytes) {
        StringBuilder md = new StringBuilder();
        md.append("# Current-speed benchmark — ").append(profileId).append(" profile\n\n");
        md.append('`').append(timestamp).append("`\n\n");

        md.append("## Latency (ms)\n\n");
        md.append("| Scenario | Avg | p50 | p95 | Max | Docs/s | Avg KB | Peak MB |\n");
        md.append("|---|---:|---:|---:|---:|---:|---:|---:|\n");
        for (LatencyRow row : latencyRows) {
            md.append("| ").append(row.scenario())
                    .append(" | ").append(format(row.avgMillis()))
                    .append(" | ").append(format(row.p50Millis()))
                    .append(" | ").append(format(row.p95Millis()))
                    .append(" | ").append(format(row.maxMillis()))
                    .append(" | ").append(format(row.docsPerSecond()))
                    .append(" | ").append(format(row.avgKilobytes()))
                    .append(" | ").append(format(row.peakHeapMb()))
                    .append(" |\n");
        }

        if (!stageRows.isEmpty()) {
            md.append("\n## Stages — template scenarios (median ms — compose / layout / render)\n\n");
            md.append("| Scenario | Compose | Layout | Render | Total |\n");
            md.append("|---|---:|---:|---:|---:|\n");
            for (StageRow row : stageRows) {
                md.append("| ").append(row.scenario())
                        .append(" | ").append(format(row.composeMillis()))
                        .append(" | ").append(format(row.layoutMillis()))
                        .append(" | ").append(format(row.renderMillis()))
                        .append(" | ").append(format(row.totalMillis()))
                        .append(" |\n");
            }
        }

        if (!throughputRows.isEmpty()) {
            md.append("\n## Throughput\n\n");
            md.append("| Threads | Total docs | Docs/s | Avg doc ms |\n");
            md.append("|---:|---:|---:|---:|\n");
            for (ThroughputRow row : throughputRows) {
                md.append("| ").append(row.threads())
                        .append(" | ").append(row.totalDocs())
                        .append(" | ").append(format(row.docsPerSecond()))
                        .append(" | ").append(format(row.avgMillisPerDoc()))
                        .append(" |\n");
            }
        }

        md.append("\nByte guard: ").append(totalBenchmarkBytes).append('\n');
        return md.toString();
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static String format(double value) {
        return "%.2f".formatted(value);
    }

    private record Scenario(String name, String description, Renderer renderer) {
    }

    // Static scenario template: name + description + a factory that binds the
    // renderer to a benchmark instance. Keeps the scenario list declarable as a
    // static constant (so the gate-coverage test can read it) while the actual
    // render still runs against per-run instance state.
    private record ScenarioDef(String name, String description, Function<CurrentSpeedBenchmark, Renderer> renderer) {
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

    record LatencyRow(String scenario,
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

    /**
     * Per-scenario compose / layout / render split (median ms). Persisted so a
     * diff can attribute a regression to an engine stage rather than only the
     * blended total — previously this was printed to the console and discarded.
     */
    private record StageRow(String scenario,
                            double composeMillis,
                            double layoutMillis,
                            double renderMillis,
                            double totalMillis) {
    }

    private record CurrentSpeedReport(String timestamp,
                                      String profile,
                                      int warmupIterations,
                                      int measurementIterations,
                                      int docsPerThread,
                                      List<Integer> threadCounts,
                                      List<LatencyRow> latency,
                                      List<StageRow> stages,
                                      List<ThroughputRow> throughput,
                                      long totalBytes) {
    }

    private record PathSummary(String jsonPath, String latencyCsvPath, String stagesCsvPath,
                               String throughputCsvPath, String summaryMarkdownPath) {
    }

    private record BenchmarkConfig(int warmupIterations,
                                   int measurementIterations,
                                   int docsPerThread,
                                   int[] threadCounts) {
    }

    private record SmokeThreshold(double maxAvgMillis, double maxPeakHeapMb) {
    }

    record PerformanceGateResult(boolean passed, String message) {
    }

    enum BenchmarkProfile {
        FULL("full", true, Map.of()),
        SMOKE("smoke", false, Map.of(
                // Thresholds calibrated against the post-warmup smoke profile
                // (30 warmup + 100 measurement). Each entry sits ~3x above the
                // observed avg on a developer laptop so CI machine variance
                // (typically 1.5-2x slower) does not produce false positives
                // while real regressions of 50% or more still trigger. The
                // previous values (800-2600 ms) were 50-100x looser and would
                // not have flagged even a 10x slowdown. long-token (observed
                // ~3.2 ms / ~94 MB) is gated too so every scenario in the
                // latency table is covered — CurrentSpeedScenarioGateTest pins
                // that invariant.
                "engine-simple", new SmokeThreshold(8.0, 96.0),
                "invoice-template", new SmokeThreshold(35.0, 384.0),
                "cv-template", new SmokeThreshold(25.0, 192.0),
                "proposal-template", new SmokeThreshold(45.0, 384.0),
                "feature-rich", new SmokeThreshold(100.0, 256.0),
                "long-token", new SmokeThreshold(10.0, 256.0),
                "vector-rich", new SmokeThreshold(20.0, 256.0)
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
