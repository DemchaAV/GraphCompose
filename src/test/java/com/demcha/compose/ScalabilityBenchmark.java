package com.demcha.compose;

import com.demcha.compose.engine.components.style.Margin;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

/**
 * Linear Scalability Test
 * Measures throughput (documents per second) as thread count increases.
 */
public class ScalabilityBenchmark {

    private static final int DOCUMENTS_PER_THREAD = Integer.getInteger("graphcompose.scalability.documentsPerThread", 100);
    private static final int WARMUP_DOCS = Integer.getInteger("graphcompose.scalability.warmupDocs", 100);
    private static final String THREAD_COUNTS = System.getProperty("graphcompose.scalability.threads", "1,2,4,8,16");

    public static void main(String[] args) throws Exception {
        BenchmarkSupport.configureQuietLogging();
        System.out.println("Starting Scalability Benchmark: Linear Scalability");
        System.out.println("------------------------------------------------------------");

        // Warmup
        for (int i = 0; i < WARMUP_DOCS; i++) {
            generateOne();
        }

        int[] threadCounts = parseThreadCounts(THREAD_COUNTS);
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
        CanonicalBenchmarkSupport.renderSimpleBenchmarkDocument(
                PDRectangle.A4,
                Margin.of(24),
                "ScalabilityRoot",
                "Scalability",
                "Scalability test message.");
    }

    private static int[] parseThreadCounts(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .mapToInt(Integer::parseInt)
                .filter(value -> value > 0)
                .toArray();
    }
}
