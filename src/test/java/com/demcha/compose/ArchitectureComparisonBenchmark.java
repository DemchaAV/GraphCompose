package com.demcha.compose;

import com.demcha.compose.layout_core.components.components_builders.ComponentBuilder;
import com.demcha.compose.layout_core.components.components_builders.TableCellSpec;
import com.demcha.compose.layout_core.components.components_builders.TableCellStyle;
import com.demcha.compose.layout_core.components.components_builders.TableColumnSpec;
import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.content.text.TextDecoration;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.ComponentColor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.AbstractDocumentComposer;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.compose.layout_core.debug.LayoutSnapshot;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.model.node.ContainerNode;
import com.demcha.compose.document.model.node.ParagraphNode;
import com.demcha.compose.document.model.node.ShapeNode;
import com.demcha.compose.document.model.node.TableNode;
import com.demcha.compose.document.model.node.TextAlign;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.awt.Color;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Manual benchmark that compares the legacy PDF composer path with the canonical
 * semantic-first document session on equivalent document scenarios.
 */
public final class ArchitectureComparisonBenchmark {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final PDRectangle SIMPLE_PAGE_SIZE = new PDRectangle(320, 260);
    private static final PDRectangle FLOW_PAGE_SIZE = new PDRectangle(220, 180);
    private static final PDRectangle TABLE_PAGE_SIZE = new PDRectangle(240, 220);
    private static final Margin SIMPLE_MARGIN = Margin.of(18);
    private static final Margin FLOW_MARGIN = Margin.of(12);
    private static final Margin TABLE_MARGIN = Margin.of(12);
    private static final int DEFAULT_SMOKE_WARMUP_ITERATIONS = 3;
    private static final int DEFAULT_SMOKE_MEASUREMENT_ITERATIONS = 8;
    private static final int DEFAULT_FULL_WARMUP_ITERATIONS = 8;
    private static final int DEFAULT_FULL_MEASUREMENT_ITERATIONS = 20;
    private static final String LONG_PARAGRAPH = ("GraphCompose keeps pagination in the engine so element extensions only " +
            "need to explain measurement, legal split points, and rendering. ").repeat(55);

    private static final TextStyle TITLE_STYLE = TextStyle.builder()
            .size(18)
            .decoration(TextDecoration.BOLD)
            .color(new Color(18, 40, 74))
            .build();
    private static final TextStyle BODY_STYLE = TextStyle.builder()
            .size(9.5)
            .decoration(TextDecoration.DEFAULT)
            .color(new Color(58, 69, 84))
            .build();
    private static final TableCellStyle TABLE_STYLE = TableCellStyle.builder()
            .padding(Padding.of(4))
            .fillColor(ComponentColor.WHITE)
            .stroke(new Stroke(ComponentColor.BLACK, 1.0))
            .textStyle(BODY_STYLE)
            .textAnchor(Anchor.centerLeft())
            .build();

    public static void main(String[] args) throws Exception {
        BenchmarkSupport.configureQuietLogging();
        new ArchitectureComparisonBenchmark().run();
    }

    private void run() throws Exception {
        BenchmarkProfile profile = BenchmarkProfile.from(System.getProperty(
                "graphcompose.benchmark.architecture.profile",
                System.getProperty("graphcompose.benchmark.profile", "smoke")));
        BenchmarkConfig config = resolveConfig(profile);

        System.out.println("GraphCompose Architecture Comparison Benchmark");
        System.out.println("Timestamp: " + LocalDateTime.now().format(TIMESTAMP_FORMAT));
        System.out.println("Profile: " + profile.id());
        System.out.println("Warmup iterations: " + config.warmupIterations());
        System.out.println("Measurement iterations: " + config.measurementIterations());
        System.out.println();

        List<Scenario> scenarios = List.of(
                new Scenario(
                        "simple-flow",
                        "Single-page title + paragraph + divider",
                        this::buildLegacySimpleScene,
                        this::buildV2SimpleScene,
                        this::renderLegacySimpleLayoutSnapshot,
                        this::renderV2SimpleLayoutSnapshot,
                        this::renderLegacySimplePdf,
                        this::renderV2SimplePdf,
                        this::renderLegacySimplePdfAfterLayout,
                        this::renderV2SimplePdfAfterLayout),
                new Scenario(
                        "paragraph-flow",
                        "Multi-page paragraph split",
                        this::buildLegacyParagraphScene,
                        this::buildV2ParagraphScene,
                        this::renderLegacyParagraphLayoutSnapshot,
                        this::renderV2ParagraphLayoutSnapshot,
                        this::renderLegacyParagraphPdf,
                        this::renderV2ParagraphPdf,
                        this::renderLegacyParagraphPdfAfterLayout,
                        this::renderV2ParagraphPdfAfterLayout),
                new Scenario(
                        "table-flow",
                        "Row-atomic table pagination",
                        this::buildLegacyTableScene,
                        this::buildV2TableScene,
                        this::renderLegacyTableLayoutSnapshot,
                        this::renderV2TableLayoutSnapshot,
                        this::renderLegacyTablePdf,
                        this::renderV2TablePdf,
                        this::renderLegacyTablePdfAfterLayout,
                        this::renderV2TablePdfAfterLayout)
        );

        System.out.println("Scene build benchmark");
        System.out.printf("%-16s | %12s | %12s | %9s | %10s%n",
                "Scenario", "Legacy ms", "Canonical ms", "Delta %", "Winner");
        System.out.println("-".repeat(69));

        long buildGuard = 0;
        List<StageRow> stageRows = new ArrayList<>();
        for (Scenario scenario : scenarios) {
            MeasuredLongResult legacy = benchmarkLongTask(scenario.legacyBuild(), config.warmupIterations(), config.measurementIterations());
            MeasuredLongResult v2 = benchmarkLongTask(scenario.v2Build(), config.warmupIterations(), config.measurementIterations());
            buildGuard += legacy.guardValue() + v2.guardValue();

            double deltaPercent = deltaPercent(legacy.avgMillis(), v2.avgMillis());
            String winner = winner(legacy.avgMillis(), v2.avgMillis());
            stageRows.add(new StageRow(
                    scenario.name(),
                    "scene-build",
                    "Composer/session creation plus scene composition before layout.",
                    round(legacy.avgMillis()),
                    round(v2.avgMillis()),
                    round(deltaPercent),
                    winner,
                    round(legacy.p50Millis()),
                    round(v2.p50Millis()),
                    round(legacy.p95Millis()),
                    round(v2.p95Millis()),
                    round(legacy.avgAllocatedMb()),
                    round(v2.avgAllocatedMb()),
                    round(legacy.avgGcCollections()),
                    round(v2.avgGcCollections()),
                    round(legacy.avgGcMillis()),
                    round(v2.avgGcMillis())));

            System.out.printf("%-16s | %12.2f | %12.2f | %8.2f%% | %10s%n",
                    scenario.name(),
                    legacy.avgMillis(),
                    v2.avgMillis(),
                    deltaPercent,
                    winner);
        }

        System.out.println();
        System.out.println("Layout snapshot benchmark");
        System.out.printf("%-16s | %12s | %12s | %9s | %10s%n",
                "Scenario", "Legacy ms", "Canonical ms", "Delta %", "Winner");
        System.out.println("-".repeat(69));

        long layoutGuard = 0;
        List<LayoutRow> layoutRows = new ArrayList<>();
        for (Scenario scenario : scenarios) {
            MeasuredLongResult legacy = benchmarkLongTask(scenario.legacyLayout(), config.warmupIterations(), config.measurementIterations());
            MeasuredLongResult v2 = benchmarkLongTask(scenario.v2Layout(), config.warmupIterations(), config.measurementIterations());
            layoutGuard += legacy.guardValue() + v2.guardValue();

            double deltaPercent = deltaPercent(legacy.avgMillis(), v2.avgMillis());
            String winner = winner(legacy.avgMillis(), v2.avgMillis());
            layoutRows.add(new LayoutRow(
                    scenario.name(),
                    scenario.description(),
                    round(legacy.avgMillis()),
                    round(v2.avgMillis()),
                    round(deltaPercent),
                    winner,
                    round(legacy.p50Millis()),
                    round(v2.p50Millis()),
                    round(legacy.p95Millis()),
                    round(v2.p95Millis()),
                    round(legacy.avgAllocatedMb()),
                    round(v2.avgAllocatedMb()),
                    round(legacy.avgGcCollections()),
                    round(v2.avgGcCollections()),
                    round(legacy.avgGcMillis()),
                    round(v2.avgGcMillis())));

            stageRows.add(new StageRow(
                    scenario.name(),
                    "layout-pagination",
                    "Resolved layout snapshot including pagination and canonical traversal.",
                    round(legacy.avgMillis()),
                    round(v2.avgMillis()),
                    round(deltaPercent),
                    winner,
                    round(legacy.p50Millis()),
                    round(v2.p50Millis()),
                    round(legacy.p95Millis()),
                    round(v2.p95Millis()),
                    round(legacy.avgAllocatedMb()),
                    round(v2.avgAllocatedMb()),
                    round(legacy.avgGcCollections()),
                    round(v2.avgGcCollections()),
                    round(legacy.avgGcMillis()),
                    round(v2.avgGcMillis())));

            System.out.printf("%-16s | %12.2f | %12.2f | %8.2f%% | %10s%n",
                    scenario.name(),
                    legacy.avgMillis(),
                    v2.avgMillis(),
                    deltaPercent,
                    winner);
        }

        System.out.println();
        System.out.println("PDF export benchmark");
        System.out.printf("%-16s | %12s | %12s | %9s | %12s | %12s | %10s%n",
                "Scenario", "Legacy ms", "Canonical ms", "Delta %", "Legacy KB", "Canonical KB", "Winner");
        System.out.println("-".repeat(109));

        long totalPdfBytes = 0;
        List<PdfRow> pdfRows = new ArrayList<>();
        for (Scenario scenario : scenarios) {
            MeasuredPdfResult legacy = benchmarkPdfTask(scenario.legacyPdf(), config.warmupIterations(), config.measurementIterations());
            MeasuredPdfResult v2 = benchmarkPdfTask(scenario.v2Pdf(), config.warmupIterations(), config.measurementIterations());
            totalPdfBytes += legacy.totalBytes() + v2.totalBytes();

            double deltaPercent = deltaPercent(legacy.avgMillis(), v2.avgMillis());
            String winner = winner(legacy.avgMillis(), v2.avgMillis());
            pdfRows.add(new PdfRow(
                    scenario.name(),
                    scenario.description(),
                    round(legacy.avgMillis()),
                    round(v2.avgMillis()),
                    round(deltaPercent),
                    round(legacy.avgKilobytes()),
                    round(v2.avgKilobytes()),
                    winner,
                    round(legacy.p50Millis()),
                    round(v2.p50Millis()),
                    round(legacy.p95Millis()),
                    round(v2.p95Millis()),
                    round(legacy.avgAllocatedMb()),
                    round(v2.avgAllocatedMb()),
                    round(legacy.avgGcCollections()),
                    round(v2.avgGcCollections()),
                    round(legacy.avgGcMillis()),
                    round(v2.avgGcMillis())));
            stageRows.add(new StageRow(
                    scenario.name(),
                    "pdf-end-to-end",
                    "Full PDF export including layout, pagination, render, and byte serialization.",
                    round(legacy.avgMillis()),
                    round(v2.avgMillis()),
                    round(deltaPercent),
                    winner,
                    round(legacy.p50Millis()),
                    round(v2.p50Millis()),
                    round(legacy.p95Millis()),
                    round(v2.p95Millis()),
                    round(legacy.avgAllocatedMb()),
                    round(v2.avgAllocatedMb()),
                    round(legacy.avgGcCollections()),
                    round(v2.avgGcCollections()),
                    round(legacy.avgGcMillis()),
                    round(v2.avgGcMillis())));

            System.out.printf("%-16s | %12.2f | %12.2f | %8.2f%% | %12.2f | %12.2f | %10s%n",
                    scenario.name(),
                    legacy.avgMillis(),
                    v2.avgMillis(),
                    deltaPercent,
                    legacy.avgKilobytes(),
                    v2.avgKilobytes(),
                    winner);
        }

        System.out.println();
        System.out.println("Render + bytes after layout benchmark");
        System.out.printf("%-16s | %12s | %12s | %9s | %10s%n",
                "Scenario", "Legacy ms", "Canonical ms", "Delta %", "Winner");
        System.out.println("-".repeat(69));

        for (Scenario scenario : scenarios) {
            MeasuredPdfResult legacy = benchmarkPdfTask(scenario.legacyPdfAfterLayout(), config.warmupIterations(), config.measurementIterations());
            MeasuredPdfResult v2 = benchmarkPdfTask(scenario.v2PdfAfterLayout(), config.warmupIterations(), config.measurementIterations());

            double deltaPercent = deltaPercent(legacy.avgMillis(), v2.avgMillis());
            String winner = winner(legacy.avgMillis(), v2.avgMillis());
            stageRows.add(new StageRow(
                    scenario.name(),
                    "render-bytes-after-layout",
                    "PDF render and byte export after an already cached layout pass.",
                    round(legacy.avgMillis()),
                    round(v2.avgMillis()),
                    round(deltaPercent),
                    winner,
                    round(legacy.p50Millis()),
                    round(v2.p50Millis()),
                    round(legacy.p95Millis()),
                    round(v2.p95Millis()),
                    round(legacy.avgAllocatedMb()),
                    round(v2.avgAllocatedMb()),
                    round(legacy.avgGcCollections()),
                    round(v2.avgGcCollections()),
                    round(legacy.avgGcMillis()),
                    round(v2.avgGcMillis())));

            System.out.printf("%-16s | %12.2f | %12.2f | %8.2f%% | %10s%n",
                    scenario.name(),
                    legacy.avgMillis(),
                    v2.avgMillis(),
                    deltaPercent,
                    winner);
        }

        System.out.println();
        System.out.println("Allocation + GC benchmark");
        System.out.printf("%-24s | %10s | %12s | %10s | %10s | %12s | %10s%n",
                "Operation", "Legacy MB", "Canonical MB", "Delta %", "Legacy GC", "Canonical GC", "Winner");
        System.out.println("-".repeat(108));
        for (LayoutRow row : layoutRows) {
            double deltaPercent = deltaPercent(row.legacyAvgAllocatedMb(), row.v2AvgAllocatedMb());
            System.out.printf("%-24s | %10.2f | %10.2f | %9.2f%% | %10.2f | %10.2f | %10s%n",
                    "layout/" + row.scenario(),
                    row.legacyAvgAllocatedMb(),
                    row.v2AvgAllocatedMb(),
                    deltaPercent,
                    row.legacyAvgGcMillis(),
                    row.v2AvgGcMillis(),
                    winner(row.legacyAvgAllocatedMb(), row.v2AvgAllocatedMb()));
        }
        for (PdfRow row : pdfRows) {
            double deltaPercent = deltaPercent(row.legacyAvgAllocatedMb(), row.v2AvgAllocatedMb());
            System.out.printf("%-24s | %10.2f | %10.2f | %9.2f%% | %10.2f | %10.2f | %10s%n",
                    "pdf/" + row.scenario(),
                    row.legacyAvgAllocatedMb(),
                    row.v2AvgAllocatedMb(),
                    deltaPercent,
                    row.legacyAvgGcMillis(),
                    row.v2AvgGcMillis(),
                    winner(row.legacyAvgAllocatedMb(), row.v2AvgAllocatedMb()));
        }

        System.out.println();
        System.out.println("Benchmark guard: " + (buildGuard + layoutGuard + totalPdfBytes));

        BenchmarkReportWriter.BenchmarkArtifacts artifacts = BenchmarkReportWriter.prepare("architecture-comparison");
        ArchitectureComparisonReport report = new ArchitectureComparisonReport(
                LocalDateTime.now().format(TIMESTAMP_FORMAT),
                profile.id(),
                config.warmupIterations(),
                config.measurementIterations(),
                layoutRows,
                pdfRows,
                stageRows,
                buildGuard,
                layoutGuard,
                totalPdfBytes);

        var jsonPath = artifacts.writeJson(report);
        var layoutCsvPath = artifacts.writeCsv(
                "layout",
                List.of("scenario", "description", "legacy_avg_ms", "canonical_avg_ms", "delta_percent", "winner",
                        "legacy_p50_ms", "canonical_p50_ms", "legacy_p95_ms", "canonical_p95_ms",
                        "legacy_avg_allocated_mb", "canonical_avg_allocated_mb",
                        "legacy_avg_gc_collections", "canonical_avg_gc_collections",
                        "legacy_avg_gc_ms", "canonical_avg_gc_ms"),
                layoutRows.stream()
                        .map(row -> List.of(
                                row.scenario(),
                                row.description(),
                                format(row.legacyAvgMillis()),
                                format(row.v2AvgMillis()),
                                format(row.deltaPercent()),
                                row.winner(),
                                format(row.legacyP50Millis()),
                                format(row.v2P50Millis()),
                                format(row.legacyP95Millis()),
                                format(row.v2P95Millis()),
                                format(row.legacyAvgAllocatedMb()),
                                format(row.v2AvgAllocatedMb()),
                                format(row.legacyAvgGcCollections()),
                                format(row.v2AvgGcCollections()),
                                format(row.legacyAvgGcMillis()),
                                format(row.v2AvgGcMillis())))
                        .toList());
        var pdfCsvPath = artifacts.writeCsv(
                "pdf",
                List.of("scenario", "description", "legacy_avg_ms", "canonical_avg_ms", "delta_percent", "legacy_avg_kb",
                        "canonical_avg_kb", "winner", "legacy_p50_ms", "canonical_p50_ms", "legacy_p95_ms", "canonical_p95_ms",
                        "legacy_avg_allocated_mb", "canonical_avg_allocated_mb",
                        "legacy_avg_gc_collections", "canonical_avg_gc_collections",
                        "legacy_avg_gc_ms", "canonical_avg_gc_ms"),
                pdfRows.stream()
                        .map(row -> List.of(
                                row.scenario(),
                                row.description(),
                                format(row.legacyAvgMillis()),
                                format(row.v2AvgMillis()),
                                format(row.deltaPercent()),
                                format(row.legacyAvgKilobytes()),
                                format(row.v2AvgKilobytes()),
                                row.winner(),
                                format(row.legacyP50Millis()),
                                format(row.v2P50Millis()),
                                format(row.legacyP95Millis()),
                                format(row.v2P95Millis()),
                                format(row.legacyAvgAllocatedMb()),
                                format(row.v2AvgAllocatedMb()),
                                format(row.legacyAvgGcCollections()),
                                format(row.v2AvgGcCollections()),
                                format(row.legacyAvgGcMillis()),
                                format(row.v2AvgGcMillis())))
                        .toList());
        var stagesCsvPath = artifacts.writeCsv(
                "stages",
                List.of("scenario", "stage", "description", "legacy_avg_ms", "canonical_avg_ms", "delta_percent", "winner",
                        "legacy_p50_ms", "canonical_p50_ms", "legacy_p95_ms", "canonical_p95_ms",
                        "legacy_avg_allocated_mb", "canonical_avg_allocated_mb",
                        "legacy_avg_gc_collections", "canonical_avg_gc_collections",
                        "legacy_avg_gc_ms", "canonical_avg_gc_ms"),
                stageRows.stream()
                        .map(row -> List.of(
                                row.scenario(),
                                row.stage(),
                                row.description(),
                                format(row.legacyAvgMillis()),
                                format(row.v2AvgMillis()),
                                format(row.deltaPercent()),
                                row.winner(),
                                format(row.legacyP50Millis()),
                                format(row.v2P50Millis()),
                                format(row.legacyP95Millis()),
                                format(row.v2P95Millis()),
                                format(row.legacyAvgAllocatedMb()),
                                format(row.v2AvgAllocatedMb()),
                                format(row.legacyAvgGcCollections()),
                                format(row.v2AvgGcCollections()),
                                format(row.legacyAvgGcMillis()),
                                format(row.v2AvgGcMillis())))
                        .toList());

        System.out.println("Saved JSON benchmark report to " + jsonPath);
        System.out.println("Saved CSV benchmark reports to " + layoutCsvPath + ", " + pdfCsvPath + " and " + stagesCsvPath);
    }

    private MeasuredLongResult benchmarkLongTask(LongTask task, int warmupIterations, int measurementIterations) throws Exception {
        long guard = 0;
        for (int i = 0; i < warmupIterations; i++) {
            guard += task.run();
        }

        long[] durationsNs = new long[measurementIterations];
        long[] allocatedBytes = new long[measurementIterations];
        long[] gcCollections = new long[measurementIterations];
        long[] gcMillis = new long[measurementIterations];
        for (int i = 0; i < measurementIterations; i++) {
            long startAllocatedBytes = currentThreadAllocatedBytes();
            GcSnapshot gcBefore = GcSnapshot.capture();
            long start = System.nanoTime();
            guard += task.run();
            long end = System.nanoTime();
            GcSnapshot gcAfter = GcSnapshot.capture();
            long endAllocatedBytes = currentThreadAllocatedBytes();

            durationsNs[i] = end - start;
            allocatedBytes[i] = Math.max(0L, endAllocatedBytes - startAllocatedBytes);
            gcCollections[i] = gcAfter.collectionCount() - gcBefore.collectionCount();
            gcMillis[i] = gcAfter.collectionTimeMillis() - gcBefore.collectionTimeMillis();
        }
        return MeasuredLongResult.from(durationsNs, allocatedBytes, gcCollections, gcMillis, guard);
    }

    private MeasuredPdfResult benchmarkPdfTask(PdfTask task, int warmupIterations, int measurementIterations) throws Exception {
        long[] durationsNs = new long[measurementIterations];
        long[] allocatedBytes = new long[measurementIterations];
        long[] gcCollections = new long[measurementIterations];
        long[] gcMillis = new long[measurementIterations];
        long totalBytes = 0;

        for (int i = 0; i < warmupIterations; i++) {
            task.run();
        }

        for (int i = 0; i < measurementIterations; i++) {
            long startAllocatedBytes = currentThreadAllocatedBytes();
            GcSnapshot gcBefore = GcSnapshot.capture();
            long start = System.nanoTime();
            byte[] pdfBytes = task.run();
            long end = System.nanoTime();
            GcSnapshot gcAfter = GcSnapshot.capture();
            long endAllocatedBytes = currentThreadAllocatedBytes();

            durationsNs[i] = end - start;
            allocatedBytes[i] = Math.max(0L, endAllocatedBytes - startAllocatedBytes);
            gcCollections[i] = gcAfter.collectionCount() - gcBefore.collectionCount();
            gcMillis[i] = gcAfter.collectionTimeMillis() - gcBefore.collectionTimeMillis();
            totalBytes += pdfBytes.length;
        }

        return MeasuredPdfResult.from(durationsNs, allocatedBytes, gcCollections, gcMillis, totalBytes, measurementIterations);
    }

    private long currentThreadAllocatedBytes() {
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) {
            return 0L;
        }
        if (!bean.isThreadAllocatedMemoryEnabled()) {
            bean.setThreadAllocatedMemoryEnabled(true);
        }
        return bean.getThreadAllocatedBytes(Thread.currentThread().threadId());
    }

    private long buildLegacySimpleScene() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(SIMPLE_PAGE_SIZE)
                .margin(SIMPLE_MARGIN)
                .markdown(false)
                .create()) {
            composeLegacySimpleDocument(composer);
            return legacyEntityCount(composer);
        }
    }

    private long buildV2SimpleScene() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(SIMPLE_PAGE_SIZE)
                .margin(SIMPLE_MARGIN)
                .create()) {
            composeV2SimpleDocument(session);
            return session.roots().size();
        }
    }

    private long buildLegacyParagraphScene() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(FLOW_PAGE_SIZE)
                .margin(FLOW_MARGIN)
                .markdown(false)
                .create()) {
            composeLegacyParagraphDocument(composer);
            return legacyEntityCount(composer);
        }
    }

    private long buildV2ParagraphScene() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(FLOW_PAGE_SIZE)
                .margin(FLOW_MARGIN)
                .create()) {
            composeV2ParagraphDocument(session);
            return session.roots().size();
        }
    }

    private long buildLegacyTableScene() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(TABLE_PAGE_SIZE)
                .margin(TABLE_MARGIN)
                .markdown(false)
                .create()) {
            composeLegacyTableDocument(composer);
            return legacyEntityCount(composer);
        }
    }

    private long buildV2TableScene() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(TABLE_PAGE_SIZE)
                .margin(TABLE_MARGIN)
                .create()) {
            composeV2TableDocument(session);
            return session.roots().size();
        }
    }

    private byte[] renderLegacySimplePdfAfterLayout() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(SIMPLE_PAGE_SIZE)
                .margin(SIMPLE_MARGIN)
                .markdown(false)
                .create()) {
            composeLegacySimpleDocument(composer);
            composer.layoutSnapshot();
            return composer.toBytes();
        }
    }

    private byte[] renderV2SimplePdfAfterLayout() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(SIMPLE_PAGE_SIZE)
                .margin(SIMPLE_MARGIN)
                .create()) {
            composeV2SimpleDocument(session);
            session.layoutSnapshot();
            return session.toPdfBytes();
        }
    }

    private byte[] renderLegacyParagraphPdfAfterLayout() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(FLOW_PAGE_SIZE)
                .margin(FLOW_MARGIN)
                .markdown(false)
                .create()) {
            composeLegacyParagraphDocument(composer);
            composer.layoutSnapshot();
            return composer.toBytes();
        }
    }

    private byte[] renderV2ParagraphPdfAfterLayout() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(FLOW_PAGE_SIZE)
                .margin(FLOW_MARGIN)
                .create()) {
            composeV2ParagraphDocument(session);
            session.layoutSnapshot();
            return session.toPdfBytes();
        }
    }

    private byte[] renderLegacyTablePdfAfterLayout() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(TABLE_PAGE_SIZE)
                .margin(TABLE_MARGIN)
                .markdown(false)
                .create()) {
            composeLegacyTableDocument(composer);
            composer.layoutSnapshot();
            return composer.toBytes();
        }
    }

    private byte[] renderV2TablePdfAfterLayout() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(TABLE_PAGE_SIZE)
                .margin(TABLE_MARGIN)
                .create()) {
            composeV2TableDocument(session);
            session.layoutSnapshot();
            return session.toPdfBytes();
        }
    }

    private long renderLegacySimpleLayoutSnapshot() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(SIMPLE_PAGE_SIZE)
                .margin(SIMPLE_MARGIN)
                .markdown(false)
                .create()) {
            composeLegacySimpleDocument(composer);
            return guard(composer.layoutSnapshot());
        }
    }

    private long renderV2SimpleLayoutSnapshot() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(SIMPLE_PAGE_SIZE)
                .margin(SIMPLE_MARGIN)
                .create()) {
            composeV2SimpleDocument(session);
            return guard(session.layoutSnapshot());
        }
    }

    private byte[] renderLegacySimplePdf() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(SIMPLE_PAGE_SIZE)
                .margin(SIMPLE_MARGIN)
                .markdown(false)
                .create()) {
            composeLegacySimpleDocument(composer);
            return composer.toBytes();
        }
    }

    private byte[] renderV2SimplePdf() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(SIMPLE_PAGE_SIZE)
                .margin(SIMPLE_MARGIN)
                .create()) {
            composeV2SimpleDocument(session);
            return session.toPdfBytes();
        }
    }

    private long renderLegacyParagraphLayoutSnapshot() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(FLOW_PAGE_SIZE)
                .margin(FLOW_MARGIN)
                .markdown(false)
                .create()) {
            composeLegacyParagraphDocument(composer);
            return guard(composer.layoutSnapshot());
        }
    }

    private long renderV2ParagraphLayoutSnapshot() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(FLOW_PAGE_SIZE)
                .margin(FLOW_MARGIN)
                .create()) {
            composeV2ParagraphDocument(session);
            return guard(session.layoutSnapshot());
        }
    }

    private byte[] renderLegacyParagraphPdf() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(FLOW_PAGE_SIZE)
                .margin(FLOW_MARGIN)
                .markdown(false)
                .create()) {
            composeLegacyParagraphDocument(composer);
            return composer.toBytes();
        }
    }

    private byte[] renderV2ParagraphPdf() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(FLOW_PAGE_SIZE)
                .margin(FLOW_MARGIN)
                .create()) {
            composeV2ParagraphDocument(session);
            return session.toPdfBytes();
        }
    }

    private long renderLegacyTableLayoutSnapshot() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(TABLE_PAGE_SIZE)
                .margin(TABLE_MARGIN)
                .markdown(false)
                .create()) {
            composeLegacyTableDocument(composer);
            return guard(composer.layoutSnapshot());
        }
    }

    private long renderV2TableLayoutSnapshot() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(TABLE_PAGE_SIZE)
                .margin(TABLE_MARGIN)
                .create()) {
            composeV2TableDocument(session);
            return guard(session.layoutSnapshot());
        }
    }

    private byte[] renderLegacyTablePdf() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(TABLE_PAGE_SIZE)
                .margin(TABLE_MARGIN)
                .markdown(false)
                .create()) {
            composeLegacyTableDocument(composer);
            return composer.toBytes();
        }
    }

    private byte[] renderV2TablePdf() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(TABLE_PAGE_SIZE)
                .margin(TABLE_MARGIN)
                .create()) {
            composeV2TableDocument(session);
            return session.toPdfBytes();
        }
    }

    private void composeLegacySimpleDocument(PdfComposer composer) {
        ComponentBuilder cb = composer.componentBuilder();
        double width = composer.canvas().innerWidth();

        cb.vContainer(Align.middle(8))
                .entityName("SimpleRoot")
                .size(width, 0)
                .anchor(Anchor.topLeft())
                .addChild(cb.text()
                        .entityName("SimpleTitle")
                        .textWithAutoSize("GraphCompose Speed Check")
                        .textStyle(TITLE_STYLE)
                        .anchor(Anchor.topLeft())
                        .build())
                .addChild(cb.blockText(Align.left(2), BODY_STYLE)
                        .entityName("SimpleBody")
                        .size(width, 2)
                        .anchor(Anchor.topLeft())
                        .padding(Padding.of(4))
                        .text(List.of(
                                        "This scenario compares the smallest realistic document flow: heading text, a wrapped paragraph, and a divider."),
                                BODY_STYLE,
                                Padding.zero(),
                                Margin.zero())
                        .build())
                .addChild(cb.divider()
                        .entityName("SimpleDivider")
                        .width(width)
                        .thickness(1)
                        .color(ComponentColor.LIGHT_GRAY)
                        .build())
                .build();
    }

    private void composeV2SimpleDocument(DocumentSession session) {
        double width = session.canvas().innerWidth();
        session.add(new ContainerNode(
                "SimpleRoot",
                List.of(
                        new ParagraphNode(
                                "SimpleTitle",
                                "GraphCompose Speed Check",
                                TITLE_STYLE,
                                TextAlign.LEFT,
                                0.0,
                                Padding.zero(),
                                Margin.zero()),
                        new ParagraphNode(
                                "SimpleBody",
                                "This scenario compares the smallest realistic document flow: heading text, a wrapped paragraph, and a divider.",
                                BODY_STYLE,
                                TextAlign.LEFT,
                                2.0,
                                Padding.of(4),
                                Margin.zero()),
                        new ShapeNode(
                                "SimpleDivider",
                                width,
                                1,
                                ComponentColor.LIGHT_GRAY,
                                null,
                                Padding.zero(),
                                Margin.zero())),
                8,
                Padding.zero(),
                Margin.zero(),
                null,
                null));
    }

    private void composeLegacyParagraphDocument(PdfComposer composer) {
        ComponentBuilder cb = composer.componentBuilder();
        double width = composer.canvas().innerWidth();

        cb.blockText(Align.left(2), BODY_STYLE)
                .entityName("LongParagraph")
                .size(width, 2)
                .anchor(Anchor.topLeft())
                .padding(Padding.of(4))
                .text(List.of(LONG_PARAGRAPH), BODY_STYLE, Padding.zero(), Margin.zero())
                .build();
    }

    private void composeV2ParagraphDocument(DocumentSession session) {
        session.add(new ParagraphNode(
                "LongParagraph",
                LONG_PARAGRAPH,
                BODY_STYLE,
                TextAlign.LEFT,
                2.0,
                Padding.of(4),
                Margin.zero()));
    }

    private void composeLegacyTableDocument(PdfComposer composer) {
        ComponentBuilder cb = composer.componentBuilder();
        double width = composer.canvas().innerWidth();
        var table = cb.table()
                .entityName("BenchmarkTable")
                .columns(TableColumnSpec.fixed(52), TableColumnSpec.auto())
                .width(width)
                .defaultCellStyle(TABLE_STYLE);
        for (List<TableCellSpec> row : tableRows()) {
            table.row(row.toArray(TableCellSpec[]::new));
        }
        var tableEntity = table.build();

        cb.vContainer(Align.middle(0))
                .entityName("BenchmarkTableRoot")
                .size(width, 0)
                .anchor(Anchor.topLeft())
                .addChild(tableEntity)
                .build();
    }

    private void composeV2TableDocument(DocumentSession session) {
        session.add(new TableNode(
                "BenchmarkTable",
                List.of(TableColumnSpec.fixed(52), TableColumnSpec.auto()),
                tableRows(),
                TABLE_STYLE,
                session.canvas().innerWidth(),
                Padding.zero(),
                Margin.zero()));
    }

    private List<List<TableCellSpec>> tableRows() {
        List<List<TableCellSpec>> rows = new ArrayList<>();
        rows.add(List.of(
                TableCellSpec.text("ID"),
                TableCellSpec.text("Summary")));
        for (int index = 1; index <= 20; index++) {
            TableCellSpec summary = index % 3 == 0
                    ? TableCellSpec.lines("Benchmark item " + index, "Second line")
                    : TableCellSpec.text("Benchmark item " + index);
            rows.add(List.of(
                    TableCellSpec.text("R" + index),
                    summary));
        }
        return List.copyOf(rows);
    }

    private long guard(LayoutSnapshot snapshot) {
        return snapshot.totalPages() * 1_000L + snapshot.nodes().size();
    }

    private BenchmarkConfig resolveConfig(BenchmarkProfile profile) {
        int defaultWarmupIterations = profile == BenchmarkProfile.SMOKE
                ? DEFAULT_SMOKE_WARMUP_ITERATIONS
                : DEFAULT_FULL_WARMUP_ITERATIONS;
        int defaultMeasurementIterations = profile == BenchmarkProfile.SMOKE
                ? DEFAULT_SMOKE_MEASUREMENT_ITERATIONS
                : DEFAULT_FULL_MEASUREMENT_ITERATIONS;

        return new BenchmarkConfig(
                Integer.getInteger("graphcompose.benchmark.architecture.warmup",
                        Integer.getInteger("graphcompose.benchmark.warmup", defaultWarmupIterations)),
                Integer.getInteger("graphcompose.benchmark.architecture.iterations",
                        Integer.getInteger("graphcompose.benchmark.iterations", defaultMeasurementIterations)));
    }

    private double deltaPercent(double legacyMillis, double v2Millis) {
        if (legacyMillis == 0.0) {
            return 0.0;
        }
        return ((v2Millis - legacyMillis) / legacyMillis) * 100.0;
    }

    private String winner(double legacyMillis, double v2Millis) {
        double delta = Math.abs(legacyMillis - v2Millis);
        if (delta < 0.01) {
            return "tie";
        }
        return v2Millis < legacyMillis ? "canonical" : "legacy";
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String format(double value) {
        return "%.2f".formatted(value);
    }

    @FunctionalInterface
    private interface LongTask {
        long run() throws Exception;
    }

    @FunctionalInterface
    private interface PdfTask {
        byte[] run() throws Exception;
    }

    private long legacyEntityCount(PdfComposer composer) throws Exception {
        Field entityManagerField = AbstractDocumentComposer.class.getDeclaredField("entityManager");
        entityManagerField.setAccessible(true);
        EntityManager entityManager = (EntityManager) entityManagerField.get(composer);
        return entityManager.getEntities().size();
    }

    private enum BenchmarkProfile {
        SMOKE("smoke"),
        FULL("full");

        private final String id;

        BenchmarkProfile(String id) {
            this.id = id;
        }

        static BenchmarkProfile from(String raw) {
            return Arrays.stream(values())
                    .filter(profile -> profile.id.equalsIgnoreCase(raw))
                    .findFirst()
                    .orElse(SMOKE);
        }

        String id() {
            return id;
        }
    }

    private record BenchmarkConfig(int warmupIterations, int measurementIterations) {
    }

    private record Scenario(
            String name,
            String description,
            LongTask legacyBuild,
            LongTask v2Build,
            LongTask legacyLayout,
            LongTask v2Layout,
            PdfTask legacyPdf,
            PdfTask v2Pdf,
            PdfTask legacyPdfAfterLayout,
            PdfTask v2PdfAfterLayout) {
    }

    private record MeasuredLongResult(
            double avgMillis,
            double p50Millis,
            double p95Millis,
            double maxMillis,
            double avgAllocatedMb,
            double avgGcCollections,
            double avgGcMillis,
            long guardValue) {

        static MeasuredLongResult from(long[] durationsNs,
                                       long[] allocatedBytes,
                                       long[] gcCollections,
                                       long[] gcMillis,
                                       long guardValue) {
            long[] sorted = Arrays.copyOf(durationsNs, durationsNs.length);
            Arrays.sort(sorted);
            double avgNs = Arrays.stream(durationsNs).average().orElse(0.0);
            return new MeasuredLongResult(
                    avgNs / 1_000_000.0,
                    percentileMillis(sorted, 0.50),
                    percentileMillis(sorted, 0.95),
                    sorted[sorted.length - 1] / 1_000_000.0,
                    averageMegabytes(allocatedBytes),
                    average(gcCollections),
                    average(gcMillis),
                    guardValue);
        }
    }

    private record MeasuredPdfResult(
            double avgMillis,
            double p50Millis,
            double p95Millis,
            double maxMillis,
            double avgAllocatedMb,
            double avgGcCollections,
            double avgGcMillis,
            double avgKilobytes,
            long totalBytes) {

        static MeasuredPdfResult from(long[] durationsNs,
                                      long[] allocatedBytes,
                                      long[] gcCollections,
                                      long[] gcMillis,
                                      long totalBytes,
                                      int measurementIterations) {
            long[] sorted = Arrays.copyOf(durationsNs, durationsNs.length);
            Arrays.sort(sorted);
            double avgNs = Arrays.stream(durationsNs).average().orElse(0.0);
            return new MeasuredPdfResult(
                    avgNs / 1_000_000.0,
                    percentileMillis(sorted, 0.50),
                    percentileMillis(sorted, 0.95),
                    sorted[sorted.length - 1] / 1_000_000.0,
                    averageMegabytes(allocatedBytes),
                    average(gcCollections),
                    average(gcMillis),
                    (totalBytes / (double) measurementIterations) / 1024.0,
                    totalBytes);
        }
    }

    private record LayoutRow(
            String scenario,
            String description,
            double legacyAvgMillis,
            double v2AvgMillis,
            double deltaPercent,
            String winner,
            double legacyP50Millis,
            double v2P50Millis,
            double legacyP95Millis,
            double v2P95Millis,
            double legacyAvgAllocatedMb,
            double v2AvgAllocatedMb,
            double legacyAvgGcCollections,
            double v2AvgGcCollections,
            double legacyAvgGcMillis,
            double v2AvgGcMillis) {
    }

    private record PdfRow(
            String scenario,
            String description,
            double legacyAvgMillis,
            double v2AvgMillis,
            double deltaPercent,
            double legacyAvgKilobytes,
            double v2AvgKilobytes,
            String winner,
            double legacyP50Millis,
            double v2P50Millis,
            double legacyP95Millis,
            double v2P95Millis,
            double legacyAvgAllocatedMb,
            double v2AvgAllocatedMb,
            double legacyAvgGcCollections,
            double v2AvgGcCollections,
            double legacyAvgGcMillis,
            double v2AvgGcMillis) {
    }

    private record StageRow(
            String scenario,
            String stage,
            String description,
            double legacyAvgMillis,
            double v2AvgMillis,
            double deltaPercent,
            String winner,
            double legacyP50Millis,
            double v2P50Millis,
            double legacyP95Millis,
            double v2P95Millis,
            double legacyAvgAllocatedMb,
            double v2AvgAllocatedMb,
            double legacyAvgGcCollections,
            double v2AvgGcCollections,
            double legacyAvgGcMillis,
            double v2AvgGcMillis) {
    }

    private record ArchitectureComparisonReport(
            String timestamp,
            String profile,
            int warmupIterations,
            int measurementIterations,
            List<LayoutRow> layout,
            List<PdfRow> pdf,
            List<StageRow> stages,
            long buildGuard,
            long layoutGuard,
            long totalPdfBytes) {
    }

    private record GcSnapshot(long collectionCount, long collectionTimeMillis) {
        static GcSnapshot capture() {
            long collections = 0L;
            long millis = 0L;
            for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
                long collectionCount = Math.max(0L, bean.getCollectionCount());
                long collectionTime = Math.max(0L, bean.getCollectionTime());
                collections += collectionCount;
                millis += collectionTime;
            }
            return new GcSnapshot(collections, millis);
        }
    }

    private static double average(long[] values) {
        return Arrays.stream(values).average().orElse(0.0);
    }

    private static double averageMegabytes(long[] values) {
        return average(values) / (1024.0 * 1024.0);
    }

    private static double percentileMillis(long[] sortedDurationsNs, double percentile) {
        int index = Math.min(sortedDurationsNs.length - 1, (int) Math.floor(sortedDurationsNs.length * percentile));
        return sortedDurationsNs[index] / 1_000_000.0;
    }
}

