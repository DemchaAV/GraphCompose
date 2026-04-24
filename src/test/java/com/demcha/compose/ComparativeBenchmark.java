package com.demcha.compose;

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
import net.sf.jasperreports.engine.design.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;

/**
 * Fair Comparative Benchmark (CPU & RAM)
 * Compares GraphCompose canonical semantic composition, iText, and JasperReports by isolating the compilation phase
 * and enforcing layout calculations.
 */
public class ComparativeBenchmark {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int WARMUP_ITERATIONS = 50;
    private static final int MEASUREMENT_ITERATIONS = 100;

    // Предкомпилированный отчет для честного теста Jasper
    private static JasperReport compiledJasperReport;

    public static void main(String[] args) throws Exception {
        BenchmarkSupport.configureQuietLogging();
        System.out.println("Starting FAIR Comparative Benchmark...");
        System.out.println("Timestamp: " + LocalDateTime.now().format(TIMESTAMP_FORMAT));
        System.out.println("------------------------------------------------------------");

        // Подготавливаем Jasper 1 раз (как в Production)
        setupJasper();

        // Прогрев JVM (JIT компилятор)
        System.out.println("Warming up JVM...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            benchmarkGraphComposeCanonical();
            benchmarkIText();
            benchmarkJasper();
        }

        // Замер
        System.out.println("Measuring performance (" + MEASUREMENT_ITERATIONS + " iterations)...");
        System.out.println(String.format("%-20s | %-12s | %-12s", "Library", "Avg Time (ms)", "Avg Heap (MB)"));
        System.out.println("------------------------------------------------------------");

        List<ComparativeRow> rows = List.of(
                runBenchmark("GraphCompose Canonical", ComparativeBenchmark::benchmarkGraphComposeCanonical),
                runBenchmark("iText 5 (Old)", ComparativeBenchmark::benchmarkIText),
                runBenchmark("JasperReports", ComparativeBenchmark::benchmarkJasper)
        );

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
        System.out.println("------------------------------------------------------------");
        System.out.println("Saved JSON benchmark report to " + jsonPath);
        System.out.println("Saved CSV benchmark report to " + csvPath);
    }

    private static ComparativeRow runBenchmark(String name, BenchmarkTask task) throws Exception {
        long totalTimeNs = 0;
        long totalAllocatedBytes = 0;
        long dummyAccumulator = 0; // Защита от Dead Code Elimination

        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            System.gc(); // Форсируем сборку мусора перед каждым замером для чистоты аллокации

            long startBytes = bean.getThreadAllocatedBytes(Thread.currentThread().threadId());
            long startTime = System.nanoTime();

            // Выполняем задачу и получаем байты PDF
            byte[] pdfBytes = task.runAndGetBytes();

            long endTime = System.nanoTime();
            long endBytes = bean.getThreadAllocatedBytes(Thread.currentThread().threadId());

            totalTimeNs += (endTime - startTime);
            totalAllocatedBytes += (endBytes - startBytes);
            dummyAccumulator += pdfBytes.length;
        }

        double avgTimeMs = (totalTimeNs / (double) MEASUREMENT_ITERATIONS) / 1_000_000.0;
        double avgMemMb = (totalAllocatedBytes / (double) MEASUREMENT_ITERATIONS) / (1024.0 * 1024.0);

        System.out.println(String.format("%-20s | %12.2f | %12.2f", name, avgTimeMs, avgMemMb));

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
        try (DocumentSession session = GraphCompose.document().pageSize(PDRectangle.A4).create()) {
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

