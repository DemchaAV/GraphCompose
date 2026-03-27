package com.demcha.compose;

import com.demcha.templates.data.MainPageCV;
import com.demcha.templates.api.MainPageCvDTO;
import com.demcha.templates.builtins.Template_CV1;
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
        System.out.println("🚀 Запуск бенчмарка FullCvBenchmark...");

        MainPageCV original = new MainPageCVMock().getMainPageCV();
        MainPageCvDTO rewritten = MainPageCvDTO.from(original);
        Template_CV1 template = new Template_CV1();

        // 1. Фаза прогрева (Warmup)
        System.out.println("🔥 Прогрев JVM (JIT-компиляция, кэширование шрифтов)...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            generateCvInMemory(template, original, rewritten);
        }

        // 2. Фаза измерения (Measurement)
        System.out.println("⏱️ Измерение скорости (" + MEASUREMENT_ITERATIONS + " итераций)...");
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

    private static void generateCvInMemory(Template_CV1 template, MainPageCV original, MainPageCvDTO rewritten) {
        try (PDDocument document = template.render(original, rewritten)) {
            // Рендерим в байты, так как I/O диска испортит метрики процессора
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при генерации PDF", e);
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

        System.out.println("\n📊 Результаты бенчмарка (в миллисекундах):");
        System.out.println("------------------------------------------------");
        System.out.printf("Минимальное время:  %.2f ms%n", min);
        System.out.printf("Среднее время:      %.2f ms%n", avg);
        System.out.printf("Медиана (50%%):      %.2f ms (Стандартное время ответа)%n", median);
        System.out.printf("95-й перцентиль:    %.2f ms (У 95%% юзеров будет не дольше)%n", p95);
        System.out.printf("99-й перцентиль:    %.2f ms (Редкие пики или сборка мусора)%n", p99);
        System.out.printf("Максимальное время: %.2f ms%n", max);
        System.out.println("------------------------------------------------");

        if (median < 200) {
            System.out.println("✅ Вердикт: Идеально! Движок летает. Можно даже не оптимизировать.");
        } else if (median < 1000) {
            System.out.println("🆗 Вердикт: Хорошо. Нормальная скорость для сложной генерации.");
        } else {
            System.out.println("⚠️ Вердикт: Медленновато. Стоит подключить профайлер и поискать узкое место.");
        }
    }
}
