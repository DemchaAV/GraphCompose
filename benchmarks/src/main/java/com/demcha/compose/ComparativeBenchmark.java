package com.demcha.compose;

import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.type.TextAdjustEnum;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fair Comparative Benchmark (CPU &amp; RAM)
 * Compares GraphCompose canonical semantic composition, iText, and JasperReports by isolating the compilation phase
 * and enforcing layout calculations.
 */
public class ComparativeBenchmark {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int WARMUP_ITERATIONS = 50;
    private static final int MEASUREMENT_ITERATIONS = 100;

    // Report-scaling sweep: the same title + prose + N-row table rendered through
    // every library at growing row counts, so the numbers show how each engine
    // SCALES (and whether GraphCompose's lead widens with document size) instead
    // of at a single fixed size. The heavy sizes use fewer iterations to keep the
    // on-demand run reasonable; this is a directional comparative, not a strict
    // JMH measurement (see benchmarks/README.md).
    private static final int[] SWEEP_SIZES = {40, 200, 1000};
    private static final int SWEEP_WARMUP_ITERATIONS = 20;
    private static final int SWEEP_MEASUREMENT_ITERATIONS = 30;

    // Per-library table layout, derived from the widest label any row can print so a
    // longer label (e.g. a bigger sweep size) can never overflow the column and break
    // the alignment again — a fixed column width is the failure mode this once had.
    private static final int LABEL_WIDTH = widestLabelWidth();
    private static final String TABLE_HEADER_FORMAT = "%-" + LABEL_WIDTH + "s | %14s | %14s%n";
    private static final String TABLE_ROW_FORMAT = "%-" + LABEL_WIDTH + "s | %14.2f | %14.2f%n";
    private static final int TABLE_RULE_WIDTH = LABEL_WIDTH + 3 + 14 + 3 + 14;

    // Scaling-summary table: an 8-wide row-count column plus four 16-wide ratio columns.
    private static final String SUMMARY_HEADER_FORMAT = "%-8s | %16s | %16s | %16s | %16s%n";
    private static final String SUMMARY_ROW_FORMAT = "%-8d | %16s | %16s | %16s | %16s%n";
    private static final int SUMMARY_RULE_WIDTH = 8 + 4 * (3 + 16);

    private static final String REPORT_PROSE =
            ("GraphCompose lays out structured business documents across many pages "
                    + "while keeping header and footer placement stable. ").repeat(6);

    // Предкомпилированный отчет для честного теста Jasper
    private static JasperReport compiledJasperReport;
    private static JasperReport compiledJasperReportHeavy;

    public static void main(String[] args) throws Exception {
        BenchmarkSupport.configureQuietLogging();
        System.out.println("Starting FAIR Comparative Benchmark...");
        System.out.println("Timestamp: " + LocalDateTime.now().format(TIMESTAMP_FORMAT));
        System.out.println("------------------------------------------------------------");

        // Per-thread allocation accounting backs the "Avg Heap (MB)" column and the
        // heap-advantage ratios. Enable it explicitly (and bail loudly if the JVM
        // does not support it) instead of trusting the platform default, matching
        // the guard the other allocation probes in this module use.
        com.sun.management.ThreadMXBean allocBean =
                (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!allocBean.isThreadAllocatedMemorySupported()) {
            throw new IllegalStateException("Thread allocated-memory measurement is not supported on this JVM");
        }
        allocBean.setThreadAllocatedMemoryEnabled(true);

        // Подготавливаем оба отчета Jasper 1 раз (как в Production)
        setupJasper();
        setupJasperReport();

        // Прогрев JVM (JIT компилятор) — оба сценария
        System.out.println("Warming up JVM...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            benchmarkGraphComposeCanonical();
            benchmarkIText();
            benchmarkJasper();
        }
        for (int i = 0; i < SWEEP_WARMUP_ITERATIONS; i++) {
            for (int size : SWEEP_SIZES) {
                benchmarkGraphComposeReport(size);
                benchmarkITextReport(size);
                benchmarkJasperReport(size);
            }
        }

        // Замер — два сценария: дешёвый (фиксированные накладные) и масштабирование отчёта
        System.out.println("Measuring performance...");
        List<ComparativeRow> rows = new ArrayList<>();

        System.out.println();
        System.out.println("Scenario: small invoice (single page, ~3 lines), " + MEASUREMENT_ITERATIONS + " iterations");
        printTableHeader();
        rows.add(runBenchmark("GraphCompose Canonical", MEASUREMENT_ITERATIONS, ComparativeBenchmark::benchmarkGraphComposeCanonical).toRow());
        rows.add(runBenchmark("iText 9", MEASUREMENT_ITERATIONS, ComparativeBenchmark::benchmarkIText).toRow());
        rows.add(runBenchmark("JasperReports", MEASUREMENT_ITERATIONS, ComparativeBenchmark::benchmarkJasper).toRow());

        System.out.println();
        System.out.println("Scenario: report scaling sweep (title + prose + N-row table), "
                + SWEEP_MEASUREMENT_ITERATIONS + " iterations per size");
        List<ScalingPoint> scaling = new ArrayList<>();
        for (int size : SWEEP_SIZES) {
            System.out.println();
            System.out.println("  N = " + size + " rows");
            printTableHeader();
            Measured gc = runBenchmark("GraphCompose (" + size + " rows)", SWEEP_MEASUREMENT_ITERATIONS,
                    () -> benchmarkGraphComposeReport(size));
            Measured it = runBenchmark("iText 9 (" + size + " rows)", SWEEP_MEASUREMENT_ITERATIONS,
                    () -> benchmarkITextReport(size));
            Measured js = runBenchmark("JasperReports (" + size + " rows)", SWEEP_MEASUREMENT_ITERATIONS,
                    () -> benchmarkJasperReport(size));
            rows.add(gc.toRow());
            rows.add(it.toRow());
            rows.add(js.toRow());
            // Ratios are computed from the full-precision averages, not the rounded
            // report rows, so the advantage figures don't compound rounding error.
            scaling.add(new ScalingPoint(size, gc, it, js));
        }
        printScalingSummary(scaling);

        BenchmarkReportWriter.BenchmarkArtifacts artifacts = BenchmarkReportWriter.prepare("comparative");
        ComparativeReport report = new ComparativeReport(
                LocalDateTime.now().format(TIMESTAMP_FORMAT),
                WARMUP_ITERATIONS,
                MEASUREMENT_ITERATIONS,
                rows);
        var jsonPath = artifacts.writeJson(report);
        var csvPath = artifacts.writeCsv(
                "libraries",
                List.of("library", "avg_time_ms", "avg_heap_mb"),
                rows.stream()
                        .map(row -> List.of(
                                row.library(),
                                "%.2f".formatted(row.avgTimeMs()),
                                "%.2f".formatted(row.avgHeapMb())))
                        .toList());
        System.out.println("-".repeat(TABLE_RULE_WIDTH));
        System.out.println("Saved JSON benchmark report to " + jsonPath);
        System.out.println("Saved CSV benchmark report to " + csvPath);

        // After all measurement, dump one rendered PDF per library/scenario so the
        // exact documents that were benchmarked can be inspected visually. This runs
        // outside the measured region, so it cannot affect the timing/allocation numbers.
        Path samples = writeSampleRenders(artifacts.directory().resolve("samples"));
        System.out.println("Saved sample renders (one PDF per library/scenario) to " + samples);
    }

    /**
     * Renders each library/scenario once more and writes the bytes to PDF files,
     * so a reader can open the actual documents the benchmark measured.
     */
    private static Path writeSampleRenders(Path directory) throws Exception {
        Files.createDirectories(directory);
        Files.write(directory.resolve("graphcompose-small.pdf"), benchmarkGraphComposeCanonical());
        Files.write(directory.resolve("itext-small.pdf"), benchmarkIText());
        Files.write(directory.resolve("jasper-small.pdf"), benchmarkJasper());
        // The smallest and largest sweep sizes, so the reader can see both a short
        // report and the multi-page document that drives the scaling numbers.
        for (int size : new int[]{SWEEP_SIZES[0], SWEEP_SIZES[SWEEP_SIZES.length - 1]}) {
            Files.write(directory.resolve("graphcompose-report-" + size + ".pdf"), benchmarkGraphComposeReport(size));
            Files.write(directory.resolve("itext-report-" + size + ".pdf"), benchmarkITextReport(size));
            Files.write(directory.resolve("jasper-report-" + size + ".pdf"), benchmarkJasperReport(size));
        }
        return directory;
    }

    private static void printTableHeader() {
        System.out.printf(TABLE_HEADER_FORMAT, "Library", "Avg Time (ms)", "Avg Heap (MB)");
        System.out.println("-".repeat(TABLE_RULE_WIDTH));
    }

    private static Measured runBenchmark(String name, int iterations, BenchmarkTask task) throws Exception {
        long totalTimeNs = 0;
        long totalAllocatedBytes = 0;
        long dummyAccumulator = 0; // Защита от Dead Code Elimination

        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

        for (int i = 0; i < iterations; i++) {
            System.gc(); // Форсируем сборку мусора перед каждым замером для чистоты аллокации

            long startBytes = bean.getThreadAllocatedBytes(Thread.currentThread().getId());
            long startTime = System.nanoTime();

            // Выполняем задачу и получаем байты PDF
            byte[] pdfBytes = task.runAndGetBytes();

            long endTime = System.nanoTime();
            long endBytes = bean.getThreadAllocatedBytes(Thread.currentThread().getId());

            totalTimeNs += (endTime - startTime);
            totalAllocatedBytes += (endBytes - startBytes);
            dummyAccumulator += pdfBytes.length;
        }

        double avgTimeMs = (totalTimeNs / (double) iterations) / 1_000_000.0;
        double avgMemMb = (totalAllocatedBytes / (double) iterations) / (1024.0 * 1024.0);

        System.out.printf(TABLE_ROW_FORMAT, name, avgTimeMs, avgMemMb);

        // Печатаем dummy-переменную, чтобы JIT не вырезал код генерации
        if (dummyAccumulator == 0) System.out.println("Error: No bytes generated");

        return new Measured(name, avgTimeMs, avgMemMb);
    }

    /**
     * GraphCompose canonical: тестируем semantic-first DocumentSession на эквивалентном сценарии.
     */
    private static byte[] benchmarkGraphComposeCanonical() throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(com.demcha.compose.document.api.DocumentPageSize.A4).create()) {
            session.add(new ContainerNode(
                    "Invoice",
                    List.of(
                            new ParagraphNode("Title", "INVOICE #12345", DocumentTextStyle.DEFAULT, TextAlign.LEFT, 0, null, null),
                            new ParagraphNode("Customer", "Customer: John Doe", DocumentTextStyle.DEFAULT, TextAlign.LEFT, 0, null, null),
                            new ParagraphNode("Amount", "Amount: $1,000.00", DocumentTextStyle.DEFAULT, TextAlign.LEFT, 0, null, null)
                    ),
                    5,
                    null,
                    DocumentInsets.of(20),
                    null,
                    null));
            return session.toPdfBytes();
        }
    }

    /**
     * GraphCompose canonical, multi-page report: title + {@code rows}-row table +
     * prose, authored through the public page-flow DSL (the realistic consumer path).
     */
    private static byte[] benchmarkGraphComposeReport(int rows) throws Exception {
        // Equal full-width columns (page width minus the 32pt L/R margins, split
        // four ways), so the table fills the page like iText (setWidthPercentage
        // 100) and Jasper (full-column-width cells) rather than hugging its text.
        final double columnWidth = (DocumentPageSize.A4.width() - 2 * 32) / 4.0;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4).margin(DocumentInsets.of(32)).create()) {
            session.pageFlow(flow -> {
                flow.name("Report").spacing(8);
                flow.addParagraph("Quarterly Business Report");
                flow.addParagraph(REPORT_PROSE);
                flow.addTable(t -> {
                    t.columns(
                            DocumentTableColumn.fixed(columnWidth),
                            DocumentTableColumn.fixed(columnWidth),
                            DocumentTableColumn.fixed(columnWidth),
                            DocumentTableColumn.fixed(columnWidth))
                            .header("Item", "Qty", "Unit", "Total").repeatHeader();
                    for (int r = 1; r <= rows; r++) {
                        t.row("Line item " + r, "3", "ea", "38.75");
                    }
                });
                flow.addParagraph(REPORT_PROSE);
            });
            return session.toPdfBytes();
        }
    }

    /**
     * iText: Тестируем с таблицей, чтобы заставить его рассчитывать геометрию
     */
    private static byte[] benchmarkIText() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // iText 9 (kernel + layout). A full-width 1-column table makes iText do
        // the same width calculation GraphCompose does.
        try (Document document = new Document(new PdfDocument(new PdfWriter(baos)))) {
            Table table = new Table(UnitValue.createPercentArray(new float[]{1})).useAllAvailableWidth();
            table.addCell(new Cell().add(new Paragraph("INVOICE #12345")));
            table.addCell(new Cell().add(new Paragraph("Customer: John Doe")));
            table.addCell(new Cell().add(new Paragraph("Amount: $1,000.00")));
            document.add(table);
        }
        return baos.toByteArray();
    }

    /**
     * iText, multi-page report: same title + {@code rows}-row table + prose. iText
     * paginates the table natively, so this exercises real multi-page layout.
     */
    private static byte[] benchmarkITextReport(int rows) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Document document = new Document(new PdfDocument(new PdfWriter(baos)))) {
            document.add(new Paragraph("Quarterly Business Report"));
            document.add(new Paragraph(REPORT_PROSE));

            Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1})).useAllAvailableWidth();
            for (String header : new String[]{"Item", "Qty", "Unit", "Total"}) {
                table.addHeaderCell(new Cell().add(new Paragraph(header)));
            }
            for (int r = 1; r <= rows; r++) {
                table.addCell(new Cell().add(new Paragraph("Line item " + r)));
                table.addCell(new Cell().add(new Paragraph("3")));
                table.addCell(new Cell().add(new Paragraph("ea")));
                table.addCell(new Cell().add(new Paragraph("38.75")));
            }
            document.add(table);
            document.add(new Paragraph(REPORT_PROSE));
        }
        return baos.toByteArray();
    }

    /**
     * JasperReports: Тестируем ТОЛЬКО заполнение и экспорт (компиляция уже сделана)
     */
    private static byte[] benchmarkJasper() throws Exception {
        // Заполняем отчет (Fill Pass)
        JasperPrint jp = JasperFillManager.fillReport(compiledJasperReport, new HashMap<>(), new JREmptyDataSource());
        // Экспортируем в байты (Export Pass)
        return JasperExportManager.exportReportToPdf(jp);
    }

    /**
     * Подготавливаем и компилируем отчет Jasper 1 раз до начала тестов
     */
    private static void setupJasper() throws Exception {
        JasperDesign jd = new JasperDesign();
        jd.setName("Invoice");
        jd.setPageWidth(595);
        jd.setPageHeight(842);

        JRDesignBand detail = new JRDesignBand();
        detail.setHeight(100);

        JRDesignStaticText text = new JRDesignStaticText();
        text.setX(0); text.setY(0);
        text.setWidth(200); text.setHeight(20);
        text.setText("INVOICE #12345\nCustomer: John Doe\nAmount: $1,000.00");
        detail.addElement(text);

        jd.setTitle(detail);

        compiledJasperReport = JasperCompileManager.compileReport(jd);
    }

    /**
     * JasperReports, multi-page report: a 4-field detail band filled from a
     * {@code rows}-row data source, with a title (+ prose) and column header.
     * Compiled once here; the benchmark measures fill + PDF export.
     */
    private static byte[] benchmarkJasperReport(int rows) throws Exception {
        List<Map<String, ?>> data = new ArrayList<>(rows);
        for (int r = 1; r <= rows; r++) {
            Map<String, Object> row = new HashMap<>();
            row.put("item", "Line item " + r);
            row.put("qty", "3");
            row.put("unit", "ea");
            row.put("total", "38.75");
            data.add(row);
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("prose", REPORT_PROSE);
        JasperPrint jp = JasperFillManager.fillReport(
                compiledJasperReportHeavy, parameters, new JRMapCollectionDataSource(data));
        return JasperExportManager.exportReportToPdf(jp);
    }

    /** A full-width prose text field that wraps and grows, so all of {@code REPORT_PROSE} renders. */
    private static JRDesignTextField proseField(int y) {
        JRDesignTextField field = new JRDesignTextField();
        field.setX(0);
        field.setY(y);
        field.setWidth(555);
        field.setHeight(14);
        field.setTextAdjust(TextAdjustEnum.STRETCH_HEIGHT);
        JRDesignExpression expression = new JRDesignExpression();
        expression.setText("$P{prose}");
        field.setExpression(expression);
        return field;
    }

    /** Compiles the multi-row Jasper report design once, before measurement. */
    private static void setupJasperReport() throws Exception {
        JasperDesign jd = new JasperDesign();
        jd.setName("Report");
        jd.setPageWidth(595);
        jd.setPageHeight(842);
        jd.setLeftMargin(20);
        jd.setRightMargin(20);
        jd.setTopMargin(20);
        jd.setBottomMargin(20);
        jd.setColumnWidth(555);

        String[] fields = {"item", "qty", "unit", "total"};
        for (String name : fields) {
            JRDesignField field = new JRDesignField();
            field.setName(name);
            field.setValueClass(String.class);
            jd.addField(field);
        }

        // Prose is a parameter rendered through a stretching text field, so all of
        // REPORT_PROSE wraps and renders (a fixed static-text box would clip it),
        // matching the full prose GraphCompose and iText lay out.
        JRDesignParameter proseParameter = new JRDesignParameter();
        proseParameter.setName("prose");
        proseParameter.setValueClass(String.class);
        jd.addParameter(proseParameter);

        // Title band: heading + the first full prose block.
        JRDesignBand title = new JRDesignBand();
        title.setHeight(40);
        JRDesignStaticText heading = new JRDesignStaticText();
        heading.setX(0);
        heading.setY(0);
        heading.setWidth(555);
        heading.setHeight(20);
        heading.setText("Quarterly Business Report");
        title.addElement(heading);
        title.addElement(proseField(22));
        jd.setTitle(title);

        // Summary band: the second full prose block (the other two libs render
        // prose both before and after the table).
        JRDesignBand summary = new JRDesignBand();
        summary.setHeight(16);
        summary.addElement(proseField(0));
        jd.setSummary(summary);

        // Column header band.
        String[] headers = {"Item", "Qty", "Unit", "Total"};
        JRDesignBand columnHeader = new JRDesignBand();
        columnHeader.setHeight(20);
        for (int i = 0; i < headers.length; i++) {
            JRDesignStaticText cell = new JRDesignStaticText();
            cell.setX(i * 138);
            cell.setY(0);
            cell.setWidth(i == headers.length - 1 ? 555 - i * 138 : 138);
            cell.setHeight(18);
            cell.setText(headers[i]);
            columnHeader.addElement(cell);
        }
        jd.setColumnHeader(columnHeader);

        // Detail band: one row per data-source record.
        JRDesignBand detail = new JRDesignBand();
        detail.setHeight(18);
        for (int i = 0; i < fields.length; i++) {
            JRDesignTextField cell = new JRDesignTextField();
            cell.setX(i * 138);
            cell.setY(0);
            cell.setWidth(i == fields.length - 1 ? 555 - i * 138 : 138);
            cell.setHeight(16);
            JRDesignExpression expression = new JRDesignExpression();
            expression.setText("$F{" + fields[i] + "}");
            cell.setExpression(expression);
            detail.addElement(cell);
        }
        ((JRDesignSection) jd.getDetailSection()).addBand(detail);

        compiledJasperReportHeavy = JasperCompileManager.compileReport(jd);
    }

    @FunctionalInterface
    public interface BenchmarkTask {
        byte[] runAndGetBytes() throws Exception;
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * The widest label printed in the per-library tables, plus one space of padding.
     * "JasperReports" is the longest library name, so its largest-sweep row is the
     * widest generated label; the fixed small-invoice labels are checked too in case
     * the sweep sizes are ever made tiny.
     */
    private static int widestLabelWidth() {
        int maxSize = 0;
        for (int size : SWEEP_SIZES) {
            maxSize = Math.max(maxSize, size);
        }
        int widest = ("JasperReports (" + maxSize + " rows)").length();
        for (String fixed : new String[]{"Library", "GraphCompose Canonical"}) {
            widest = Math.max(widest, fixed.length());
        }
        return widest + 1;
    }

    /**
     * Prints how GraphCompose's time/memory advantage over iText and Jasper changes
     * as the row count grows, so the "does the lead widen with document size?"
     * question is answered by the numbers rather than asserted. A ratio above 1.0
     * means GraphCompose is that many times faster / lighter at that size.
     */
    private static void printScalingSummary(List<ScalingPoint> scaling) {
        System.out.println();
        System.out.println("Scaling summary (GraphCompose advantage; >1.0 = GraphCompose faster / lighter)");
        System.out.printf(SUMMARY_HEADER_FORMAT,
                "Rows", "Time vs iText", "Time vs Jasper", "Heap vs iText", "Heap vs Jasper");
        System.out.println("-".repeat(SUMMARY_RULE_WIDTH));
        for (ScalingPoint p : scaling) {
            System.out.printf(SUMMARY_ROW_FORMAT,
                    p.rows(),
                    ratio(p.iText().timeMs(), p.graphCompose().timeMs()),
                    ratio(p.jasper().timeMs(), p.graphCompose().timeMs()),
                    ratio(p.iText().heapMb(), p.graphCompose().heapMb()),
                    ratio(p.jasper().heapMb(), p.graphCompose().heapMb()));
        }
    }

    /** {@code other / graphCompose} as an "Nx" string; guards against divide-by-zero. */
    private static String ratio(double other, double graphCompose) {
        if (graphCompose <= 0.0) {
            return "n/a";
        }
        return "%.2fx".formatted(other / graphCompose);
    }

    private record ComparativeRow(String library, double avgTimeMs, double avgHeapMb) {
    }

    /** Full-precision average for one library/scenario, before report rounding. */
    private record Measured(String name, double timeMs, double heapMb) {
        ComparativeRow toRow() {
            return new ComparativeRow(name, round(timeMs), round(heapMb));
        }
    }

    private record ScalingPoint(int rows, Measured graphCompose, Measured iText, Measured jasper) {
    }

    private record ComparativeReport(String timestamp,
                                     int warmupIterations,
                                     int measurementIterations,
                                     List<ComparativeRow> libraries) {
    }
}

