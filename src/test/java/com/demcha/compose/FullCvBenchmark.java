package com.demcha.compose;

import com.demcha.templates.data.MainPageCV;
import com.demcha.templates.api.MainPageCvDTO;
import com.demcha.templates.builtins.CvTemplateV1;
import com.demcha.mock.MainPageCVMock;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public class FullCvBenchmark {

    // Количество холостых запусков для оптимизации кода JIT-компилятором
    private static final int WARMUP_ITERATIONS = 100;
    // Количество реальных замеров
    private static final int MEASUREMENT_ITERATIONS = 500;

    public static void main(String[] args) {
        BenchmarkSupport.configureQuietLogging();
        System.out.println("Starting FullCvBenchmark...");

        MainPageCV original = new MainPageCVMock().getMainPageCV();
        MainPageCvDTO rewritten = MainPageCvDTO.from(original);
        CvTemplateV1 template = new CvTemplateV1();

        // 1. Фаза прогрева (Warmup)
        System.out.println("Warming up JVM (JIT compilation, font cache warmup)...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            generateCvInMemory(template, original, rewritten);
        }

        // 2. Фаза измерения (Measurement)
        System.out.println("Measuring performance (" + MEASUREMENT_ITERATIONS + " iterations)...");
        long[] durationsNs = new long[MEASUREMENT_ITERATIONS];

        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            long start = System.nanoTime();
            generateCvInMemory(template, original, rewritten);
            long end = System.nanoTime();
            durationsNs[i] = end - start;
        }

        // 3. Расчет и вывод статистики
        printStatistics(durationsNs);
    }

    private static void generateCvInMemory(CvTemplateV1 template, MainPageCV original, MainPageCvDTO rewritten) {
        try (PDDocument document = template.render(original, rewritten)) {
            // Рендерим в байты, так как I/O диска испортит метрики процессора
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private static void printStatistics(long[] durationsNs) {
        // Сортируем массив для вычисления перцентилей
        Arrays.sort(durationsNs);

        // Переводим наносекунды в миллисекунды (ms)
        double[] durationsMs = Arrays.stream(durationsNs).mapToDouble(ns -> ns / 1_000_000.0).toArray();

        double min = durationsMs[0];
        double max = durationsMs[durationsMs.length - 1];
        double avg = Arrays.stream(durationsMs).average().orElse(0.0);

        // Перцентили показывают реальную картину лучше, чем среднее значение
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
