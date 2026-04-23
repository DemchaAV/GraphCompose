package com.demcha.compose;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.builtins.CvTemplateV1;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.util.Arrays;

public class FullCvBenchmark {

    private static final int WARMUP_ITERATIONS = Integer.getInteger("graphcompose.benchmark.fullCv.warmup", 100);
    private static final int MEASUREMENT_ITERATIONS = Integer.getInteger("graphcompose.benchmark.fullCv.iterations", 500);

    public static void main(String[] args) {
        BenchmarkSupport.configureQuietLogging();
        System.out.println("Starting FullCvBenchmark...");

        CvDocumentSpec cv = CanonicalBenchmarkSupport.canonicalCv();
        CvTemplateV1 template = new CvTemplateV1();

        System.out.println("Warming up JVM (JIT compilation, font cache warmup)...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            generateCvInMemory(template, cv);
        }

        System.out.println("Measuring performance (" + MEASUREMENT_ITERATIONS + " iterations)...");
        long[] durationsNs = new long[MEASUREMENT_ITERATIONS];

        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            long start = System.nanoTime();
            generateCvInMemory(template, cv);
            long end = System.nanoTime();
            durationsNs[i] = end - start;
        }

        printStatistics(durationsNs);
    }

    private static void generateCvInMemory(CvTemplateV1 template, CvDocumentSpec cv) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(PDRectangle.A4)
                .margin(15, 10, 15, 15)
                .create()) {
            template.compose(document, cv);
            document.toPdfBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private static void printStatistics(long[] durationsNs) {
        Arrays.sort(durationsNs);

        double[] durationsMs = Arrays.stream(durationsNs).mapToDouble(ns -> ns / 1_000_000.0).toArray();

        double min = durationsMs[0];
        double max = durationsMs[durationsMs.length - 1];
        double avg = Arrays.stream(durationsMs).average().orElse(0.0);
        double median = durationsMs[(int) (durationsMs.length * 0.5)];
        double p95 = durationsMs[(int) (durationsMs.length * 0.95)];
        double p99 = durationsMs[(int) (durationsMs.length * 0.99)];

        System.out.println("\nBenchmark results (milliseconds):");
        System.out.println("------------------------------------------------");
        System.out.printf("Min time:           %.2f ms%n", min);
        System.out.printf("Average time:       %.2f ms%n", avg);
        System.out.printf("Median (50%%):       %.2f ms (typical response time)%n", median);
        System.out.printf("95th percentile:    %.2f ms (95%% of runs finish within this)%n", p95);
        System.out.printf("99th percentile:    %.2f ms (rare spikes or GC pressure)%n", p99);
        System.out.printf("Max time:           %.2f ms%n", max);
        System.out.println("------------------------------------------------");

        if (median < 200) {
            System.out.println("Verdict: Excellent. The engine is very fast for this scenario.");
        } else if (median < 1000) {
            System.out.println("Verdict: Good. This is a healthy speed for complex generation.");
        } else {
            System.out.println("Verdict: Slow enough to investigate with a profiler.");
        }
    }
}
