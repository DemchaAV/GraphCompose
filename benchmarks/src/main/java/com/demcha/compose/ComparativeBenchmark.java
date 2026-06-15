package com.demcha.compose;

import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
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
 * Fair Comparative Benchmark (CPU & RAM)
 * Compares GraphCompose canonical semantic composition, iText, and JasperReports by isolating the compilation phase
 * and enforcing layout calculations.
 */
public class ComparativeBenchmark {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int WARMUP_ITERATIONS = 50;
    private static final int MEASUREMENT_ITERATIONS = 100;

    // Multi-page "report" scenario: a title, an N-row line-item table, and prose.
    // Rendered with equivalent content across all three libraries so the numbers
    // reflect real multi-page document work, not just per-render fixed overhead.
    private static final int REPORT_ROWS = 40;
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

        // Подготавливаем оба отчета Jasper 1 раз (как в Production)
        setupJasper();
        setupJasperReport();

        // Прогрев JVM (JIT компилятор) — оба сценария
        System.out.println("Warming up JVM...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            benchmarkGraphComposeCanonical();
            benchmarkIText();
            benchmarkJasper();
            benchmarkGraphComposeReport();
            benchmarkITextReport();
            benchmarkJasperReport();
        }

        // Замер — два сценария: дешёвый (фиксированные накладные) и многостраничный
        System.out.println("Measuring performance (" + MEASUREMENT_ITERATIONS + " iterations)...");
        List<ComparativeRow> rows = new ArrayList<>();

        System.out.println();
        System.out.println("Scenario: small invoice (single page, ~3 lines)");
        printTableHeader();
        rows.add(runBenchmark("GraphCompose Canonical", ComparativeBenchmark::benchmarkGraphComposeCanonical));
        rows.add(runBenchmark("iText 5 (Old)", ComparativeBenchmark::benchmarkIText));
        rows.add(runBenchmark("JasperReports", ComparativeBenchmark::benchmarkJasper));

        System.out.println();
        System.out.println("Scenario: business report (multi-page: title + " + REPORT_ROWS + "-row table + prose)");
        printTableHeader();
        rows.add(runBenchmark("GraphCompose (report)", ComparativeBenchmark::benchmarkGraphComposeReport));
        rows.add(runBenchmark("iText 5 (report)", ComparativeBenchmark::benchmarkITextReport));
        rows.add(runBenchmark("JasperReports (report)", ComparativeBenchmark::benchmarkJasperReport));

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
        System.out.println("-".repeat(60));
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
        Files.write(directory.resolve("graphcompose-report.pdf"), benchmarkGraphComposeReport());
        Files.write(directory.resolve("itext-report.pdf"), benchmarkITextReport());
        Files.write(directory.resolve("jasper-report.pdf"), benchmarkJasperReport());
        return directory;
    }

    private static void printTableHeader() {
        System.out.printf("%-24s | %14s | %14s%n", "Library", "Avg Time (ms)", "Avg Heap (MB)");
        System.out.println("-".repeat(60));
    }

    private static ComparativeRow runBenchmark(String name, BenchmarkTask task) throws Exception {
        long totalTimeNs = 0;
        long totalAllocatedBytes = 0;
        long dummyAccumulator = 0; // Защита от Dead Code Elimination

        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
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

        double avgTimeMs = (totalTimeNs / (double) MEASUREMENT_ITERATIONS) / 1_000_000.0;
        double avgMemMb = (totalAllocatedBytes / (double) MEASUREMENT_ITERATIONS) / (1024.0 * 1024.0);

        System.out.printf("%-24s | %14.2f | %14.2f%n", name, avgTimeMs, avgMemMb);

        // Печатаем dummy-переменную, чтобы JIT не вырезал код генерации
        if (dummyAccumulator == 0) System.out.println("Error: No bytes generated");

        return new ComparativeRow(
                name,
                round(avgTimeMs),
                round(avgMemMb)
        );
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
     * GraphCompose canonical, multi-page report: title + N-row table + prose,
     * authored through the public page-flow DSL (the realistic consumer path).
     */
    private static byte[] benchmarkGraphComposeReport() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4).margin(DocumentInsets.of(32)).create()) {
            session.pageFlow(flow -> {
                flow.name("Report").spacing(8);
                flow.addParagraph("Quarterly Business Report");
                flow.addParagraph(REPORT_PROSE);
                flow.addTable(t -> {
                    t.autoColumns(4).header("Item", "Qty", "Unit", "Total").repeatHeader();
                    for (int r = 1; r <= REPORT_ROWS; r++) {
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
        Document document = new Document();
        PdfWriter.getInstance(document, baos);
        document.open();

        // Используем таблицу, чтобы iText делал расчет ширины (как GraphCompose)
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.addCell(new Paragraph("INVOICE #12345"));
        table.addCell(new Paragraph("Customer: John Doe"));
        table.addCell(new Paragraph("Amount: $1,000.00"));

        document.add(table);
        document.close();
        return baos.toByteArray();
    }

    /**
     * iText, multi-page report: same title + N-row table + prose. iText paginates
     * the {@code PdfPTable} natively, so this exercises real multi-page layout.
     */
    private static byte[] benchmarkITextReport() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, baos);
        document.open();
        document.add(new Paragraph("Quarterly Business Report"));
        document.add(new Paragraph(REPORT_PROSE));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        for (String header : new String[]{"Item", "Qty", "Unit", "Total"}) {
            table.addCell(new Paragraph(header));
        }
        for (int r = 1; r <= REPORT_ROWS; r++) {
            table.addCell(new Paragraph("Line item " + r));
            table.addCell(new Paragraph("3"));
            table.addCell(new Paragraph("ea"));
            table.addCell(new Paragraph("38.75"));
        }
        document.add(table);
        document.add(new Paragraph(REPORT_PROSE));
        document.close();
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
     * JasperReports, multi-page report: a 4-field detail band filled from an
     * {@code REPORT_ROWS}-row data source, with a title (+ prose) and column
     * header. Compiled once here; the benchmark measures fill + PDF export.
     */
    private static byte[] benchmarkJasperReport() throws Exception {
        List<Map<String, ?>> data = new ArrayList<>(REPORT_ROWS);
        for (int r = 1; r <= REPORT_ROWS; r++) {
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
            cell.setWidth(138);
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
            cell.setWidth(138);
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

    private record ComparativeRow(String library, double avgTimeMs, double avgHeapMb) {
    }

    private record ComparativeReport(String timestamp,
                                     int warmupIterations,
                                     int measurementIterations,
                                     List<ComparativeRow> libraries) {
    }
}

