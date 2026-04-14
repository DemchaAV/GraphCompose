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
        BenchmarkSupport.configureQuietLogging();
        System.out.println("Starting GraphCompose stress test...");

        int threadCount = 50;
        int totalTasks = 5000;

        System.out.println("Creating a pool with " + threadCount + " threads for " + totalTasks + " tasks...");
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        long start = System.currentTimeMillis();

        for (int i = 0; i < totalTasks; i++) {
            executor.submit(() -> {
                try {
                    generateCvInMemory();
                    successCount.incrementAndGet();
                } catch (Throwable t) { // Catch everything, including system Errors
                    if (errorCount.getAndIncrement() == 0) {
                        System.err.println("\nFirst error captured. Root cause:");
                        t.printStackTrace();
                    } else {
                        System.err.print("x"); // Mark subsequent errors compactly
                    }
                }
            });
        }

        System.out.println("All tasks submitted. Waiting for completion (up to 1 minute)...");
        executor.shutdown();

        try {
            boolean finished = executor.awaitTermination(1, TimeUnit.MINUTES);
            if (!finished) {
                System.err.println("\nTimeout reached. Threads may be deadlocked or running too slowly.");
            }
        } catch (InterruptedException e) {
            System.err.println("\nMain thread was interrupted.");
            e.printStackTrace();
        }

        long end = System.currentTimeMillis();

        System.out.println("\n\nStress test summary:");
        System.out.println("Successful: " + successCount.get());
        System.out.println("Errors:     " + errorCount.get());
        System.out.println("Time:       " + (end - start) + " ms");
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
