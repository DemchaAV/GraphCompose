package com.demcha.compose;

import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.fixed.pdf.PdfMeasurementResources;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.layout.DocumentLayoutPassContext;
import com.demcha.compose.document.layout.LayoutCanvas;
import com.demcha.compose.document.layout.LayoutCompiler;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.NodeRegistry;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;

import java.awt.Color;
import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Deterministic measurement-count and allocation probe for the canonical layout
 * pipeline.
 *
 * <p>For each scenario this harness authors a document through the public DSL,
 * then compiles its node graph through a {@link LayoutCompiler} whose
 * {@code TextMeasurementSystem} is wrapped in a
 * {@link CountingTextMeasurementSystem}. It reports, deterministically and
 * independent of wall-clock / GC-timing noise:</p>
 *
 * <ul>
 *     <li><b>measurement requests</b> — how the layout asks the measurement
 *         system for widths (proves F1/F2/F3); and</li>
 *     <li><b>compile allocation bytes</b> — bytes allocated by the layout
 *         {@code compile} pass, via
 *         {@link com.sun.management.ThreadMXBean#getCurrentThreadAllocatedBytes()}.
 *         Unlike the {@code peakHeapMb} sampled by {@code CurrentSpeedBenchmark}
 *         (a GC-timing-dependent used-heap delta), allocated-bytes is the
 *         deterministic memory signal for the allocation findings (F7 style/inset
 *         churn, F8 box recomputation, fragment re-copy, per-cell table lists).</li>
 * </ul>
 *
 * <p>The allocation window wraps only {@code compile(...)}; font loading and DSL
 * authoring happen outside it, so the number reflects layout allocation — the
 * thing the optimizations move. Needs no {@code src/main} changes.</p>
 */
public final class MeasurementCountBenchmark {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final com.sun.management.ThreadMXBean THREAD_MX =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

    private static final DocumentTextStyle BODY_STYLE = DocumentTextStyle.builder()
            .size(9.5)
            .decoration(DocumentTextDecoration.DEFAULT)
            .color(DocumentColor.of(new Color(58, 69, 84)))
            .build();

    private static final String LONG_PARAGRAPH =
            ("GraphCompose lays out structured business documents efficiently across many pages "
                    + "while keeping header and footer placement stable. ").repeat(120);

    private static final String LONG_TOKEN_PARAGRAPH =
            "Prefix text before an unbreakable token " + "x".repeat(600)
                    + " and several trailing words that must still wrap onto the following lines here.";

    public static void main(String[] args) throws Exception {
        BenchmarkSupport.configureQuietLogging();
        new MeasurementCountBenchmark().run();
    }

    private void run() throws Exception {
        enableAllocationMeasurement();

        System.out.println("GraphCompose Measurement-Count + Allocation Probe");
        System.out.println("Timestamp: " + LocalDateTime.now().format(TIMESTAMP_FORMAT));
        System.out.println("Thread allocation measurement: " + (allocationSupported() ? "enabled" : "UNAVAILABLE (Alloc KB = n/a)"));
        System.out.println();

        Consumer<PageFlowBuilder> longText = flow ->
                flow.addParagraph(p -> p.text(LONG_PARAGRAPH).textStyle(BODY_STYLE));
        Consumer<PageFlowBuilder> longToken = flow ->
                flow.addParagraph(p -> p.text(LONG_TOKEN_PARAGRAPH).textStyle(BODY_STYLE));
        Consumer<PageFlowBuilder> largeTable = MeasurementCountBenchmark::authorLargeTable;

        // Warm up the JVM (class loading + JIT) BEFORE the allocation window so the
        // "Alloc KB" column reflects steady-state per-document layout allocation, not
        // one-time cold-start cost. Without this the FIRST scenario measured carried
        // ~36 MB of class-load / JIT / static-init allocation — a JVM artifact, not a
        // layout cost (verified: cold first compile 36.6 MB vs warm 0.65 MB for the
        // same long-text document). The measurement-COUNT columns are exact either way.
        for (int warmup = 0; warmup < 5; warmup++) {
            measureScenario("warmup", longText);
            measureScenario("warmup", longToken);
            measureScenario("warmup", largeTable);
        }

        List<Result> results = new ArrayList<>();
        results.add(measureScenario("long-text", longText));
        results.add(measureScenario("long-token", longToken));
        results.add(measureScenario("large-table", largeTable));

        System.out.printf("%-14s | %11s | %9s | %9s | %11s | %8s | %11s | %10s | %6s%n",
                "Scenario", "WidthReqs", "Distinct", "Repeat %", "Sum chars", "Max arg", "LineMetrics", "Alloc KB", "Pages");
        System.out.println("-".repeat(108));
        for (Result result : results) {
            CountingTextMeasurementSystem.Counts c = result.counts();
            System.out.printf("%-14s | %11d | %9d | %8.1f%% | %11d | %8d | %11d | %10s | %6d%n",
                    result.scenario(),
                    c.widthRequests(),
                    c.distinctWidthRequests(),
                    c.repeatRatePct(),
                    c.summedRequestChars(),
                    c.maxRequestChars(),
                    c.lineMetricsCalls(),
                    formatAllocKb(result.compileAllocBytes()),
                    result.pages());
        }

        writeReport(results);
    }

    private Result measureScenario(String scenario, Consumer<PageFlowBuilder> author) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(24, 24, 24, 24)
                .create()) {
            session.pageFlow(author);
            List<DocumentNode> roots = session.roots();
            LayoutCanvas canvas = session.canvas();
            NodeRegistry registry = session.registry();

            try (PdfMeasurementResources resources = PdfMeasurementResources.open(List.of())) {
                CountingTextMeasurementSystem counter =
                        new CountingTextMeasurementSystem(resources.textMeasurementSystem());
                DocumentLayoutPassContext context = new DocumentLayoutPassContext(
                        registry, canvas, resources.fontLibrary(), counter, false);
                LayoutCompiler compiler = new LayoutCompiler(registry);
                DocumentGraph graph = new DocumentGraph(roots);

                // Measure allocation around the layout compile only — font
                // loading and authoring are already done, so this is the
                // layout pass's own allocation footprint.
                long allocBefore = currentThreadAllocatedBytes();
                LayoutGraph layout = compiler.compile(graph, context, context);
                long allocBytes = allocBefore < 0 ? -1 : currentThreadAllocatedBytes() - allocBefore;

                return new Result(scenario, counter.snapshot(), layout.totalPages(), layout.fragments().size(), allocBytes);
            }
        }
    }

    private static void authorLargeTable(PageFlowBuilder flow) {
        flow.addTable(table -> {
            table.autoColumns(6).header("Item", "Qty", "Unit", "Price", "Tax", "Total");
            for (int row = 1; row <= 200; row++) {
                table.row("Line item " + row, "3", "ea", "12.50", "1.25", "38.75");
            }
        });
    }

    private static void enableAllocationMeasurement() {
        try {
            if (THREAD_MX.isThreadAllocatedMemorySupported() && !THREAD_MX.isThreadAllocatedMemoryEnabled()) {
                THREAD_MX.setThreadAllocatedMemoryEnabled(true);
            }
        } catch (UnsupportedOperationException ignored) {
            // Allocation measurement unsupported on this JVM; Alloc KB reports n/a.
        }
    }

    private static boolean allocationSupported() {
        try {
            return THREAD_MX.isThreadAllocatedMemorySupported() && THREAD_MX.isThreadAllocatedMemoryEnabled();
        } catch (UnsupportedOperationException ex) {
            return false;
        }
    }

    private static long currentThreadAllocatedBytes() {
        if (!allocationSupported()) {
            return -1;
        }
        return THREAD_MX.getCurrentThreadAllocatedBytes();
    }

    private static String formatAllocKb(long bytes) {
        return bytes < 0 ? "n/a" : "%.1f".formatted(bytes / 1024.0);
    }

    private void writeReport(List<Result> results) throws Exception {
        CounterReport report = new CounterReport(
                LocalDateTime.now().format(TIMESTAMP_FORMAT),
                results.stream().map(Result::toScenarioCounts).toList());

        BenchmarkReportWriter.BenchmarkArtifacts artifacts = BenchmarkReportWriter.prepare("counters");
        var jsonPath = artifacts.writeJson(report);
        var csvPath = artifacts.writeCsv(
                "counters",
                List.of("scenario", "width_requests", "distinct_width_requests", "repeat_rate_pct",
                        "summed_request_chars", "max_request_chars", "text_width_calls", "measure_calls",
                        "line_metrics_calls", "compile_alloc_bytes", "pages", "fragments"),
                results.stream()
                        .map(result -> {
                            CountingTextMeasurementSystem.Counts c = result.counts();
                            return List.of(
                                    result.scenario(),
                                    Long.toString(c.widthRequests()),
                                    Long.toString(c.distinctWidthRequests()),
                                    "%.2f".formatted(c.repeatRatePct()),
                                    Long.toString(c.summedRequestChars()),
                                    Long.toString(c.maxRequestChars()),
                                    Long.toString(c.textWidthCalls()),
                                    Long.toString(c.measureCalls()),
                                    Long.toString(c.lineMetricsCalls()),
                                    Long.toString(result.compileAllocBytes()),
                                    Integer.toString(result.pages()),
                                    Integer.toString(result.fragments()));
                        })
                        .toList());

        System.out.println();
        System.out.println("Saved JSON counter report to " + jsonPath);
        System.out.println("Saved CSV counter report to " + csvPath);
    }

    private record Result(String scenario,
                          CountingTextMeasurementSystem.Counts counts,
                          int pages,
                          int fragments,
                          long compileAllocBytes) {
        ScenarioCounts toScenarioCounts() {
            return new ScenarioCounts(
                    scenario,
                    counts.widthRequests(),
                    counts.distinctWidthRequests(),
                    counts.repeatRatePct(),
                    counts.summedRequestChars(),
                    counts.maxRequestChars(),
                    counts.textWidthCalls(),
                    counts.measureCalls(),
                    counts.lineMetricsCalls(),
                    counts.lineHeightCalls(),
                    compileAllocBytes,
                    pages,
                    fragments);
        }
    }

    private record ScenarioCounts(String scenario,
                                  long widthRequests,
                                  long distinctWidthRequests,
                                  double repeatRatePct,
                                  long summedRequestChars,
                                  long maxRequestChars,
                                  long textWidthCalls,
                                  long measureCalls,
                                  long lineMetricsCalls,
                                  long lineHeightCalls,
                                  long compileAllocBytes,
                                  int pages,
                                  int fragments) {
    }

    private record CounterReport(String timestamp, List<ScenarioCounts> scenarios) {
    }
}
