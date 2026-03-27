package com.demcha.compose;

import com.demcha.templates.CvTheme;
import com.demcha.templates.TemplateBuilder;
import com.demcha.compose.layout_core.core.PdfComposer;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GraphComposeStressTest {
    public  static void main(String[] args) {
        System.out.println("🚀 СТАРТ: Инициализация стресс-теста...");

        int threadCount = 50;
        int totalTasks = 5000;

        System.out.println("⚙️ Создаем пул на " + threadCount + " потоков для " + totalTasks + " задач...");
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        long start = System.currentTimeMillis();

        for (int i = 0; i < totalTasks; i++) {
            executor.submit(() -> {
                try {
                    generateCvInMemory();
                    successCount.incrementAndGet();
                } catch (Throwable t) { // Ловим вообще всё, даже системные Error
                    if (errorCount.getAndIncrement() == 0) {
                        System.err.println("\n🔥 ПОЙМАЛИ ПЕРВУЮ ОШИБКУ! ВОТ ЕЁ ПРИЧИНА:");
                        t.printStackTrace();
                    } else {
                        System.err.print("x"); // Печатаем крестик для последующих ошибок
                    }
                }
            });
        }

        System.out.println("⏳ Все задачи отправлены в пул. Ждем выполнения (максимум 1 минуту)...");
        executor.shutdown();

        try {
            boolean finished = executor.awaitTermination(1, TimeUnit.MINUTES);
            if (!finished) {
                System.err.println("\n⚠️ ВРЕМЯ ВЫШЛО! Потоки зависли (Deadlock) или работают слишком медленно.");
            }
        } catch (InterruptedException e) {
            System.err.println("\n⚠️ Главный поток был прерван!");
            e.printStackTrace();
        }

        long end = System.currentTimeMillis();

        System.out.println("\n\n📊 ИТОГИ СТРЕСС-ТЕСТА:");
        System.out.println("✅ Успешно: " + successCount.get());
        System.out.println("❌ Ошибок:  " + errorCount.get());
        System.out.println("⏱️ Время:   " + (end - start) + " мс");
    }

    private static void generateCvInMemory() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(PDRectangle.A4)
                .margin(24, 24, 24, 24)
                .create()) {

            TemplateBuilder template = TemplateBuilder.from(
                    composer.componentBuilder(),
                    CvTheme.defaultTheme()
            );

            template.moduleBuilder("Profile", composer.canvas())
                    .addChild(template.blockText(
                            "Analytical engineer focused on reliable platform design.",
                            composer.canvas().innerWidth()
                    ))
                    .build();

            byte[] pdfBytes = composer.toBytes();
        }
    }
}
