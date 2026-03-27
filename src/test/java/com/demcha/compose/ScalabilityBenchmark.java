package com.demcha.compose;

import com.demcha.templates.CvTheme;
import com.demcha.templates.TemplateBuilder;
import com.demcha.compose.layout_core.core.PdfComposer;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Linear Scalability Test
 * Measures throughput (documents per second) as thread count increases.
 */
public class ScalabilityBenchmark {

    private static final int DOCUMENTS_PER_THREAD = 100;
    private static final int WARMUP_DOCS = 100;

    public static void main(String[] args) throws Exception {
        BenchmarkSupport.configureQuietLogging();
        System.out.println("🚀 Starting Scalability Benchmark: Linear Scalability");
        System.out.println("------------------------------------------------------------");

        // Warmup
        for (int i = 0; i < WARMUP_DOCS; i++) {
            generateOne();
        }

        int[] threadCounts = {1, 2, 4, 8, 16};
        System.out.println(String.format("%-10s | %-15s | %-12s", "Threads", "Total Docs", "Throughput (docs/sec)"));
        System.out.println("------------------------------------------------------------");

        for (int threads : threadCounts) {
            runScalabilityTest(threads);
        }
    }

    private static void runScalabilityTest(int threads) throws Exception {
        int totalDocs = threads * DOCUMENTS_PER_THREAD;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        
        long startTime = System.nanoTime();
        
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < totalDocs; i++) {
            futures.add(executor.submit(() -> {
                try {
                    generateOne();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        long endTime = System.nanoTime();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        double durationSec = (endTime - startTime) / 1_000_000_000.0;
        double throughput = totalDocs / durationSec;

        System.out.println(String.format("%-10d | %-15d | %12.2f", threads, totalDocs, throughput));
    }

    private static void generateOne() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf().pageSize(PDRectangle.A4).create()) {
            TemplateBuilder template = TemplateBuilder.from(composer.componentBuilder(), CvTheme.defaultTheme());
            template.moduleBuilder("Scalability", composer.canvas())
                    .addChild(template.blockText("Scalability test message.", composer.canvas().innerWidth()))
                    .build();
            composer.toBytes();
        }
    }
}
