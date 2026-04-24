package com.demcha.compose;

import com.demcha.compose.engine.components.style.Margin;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class GraphComposeStressTest {
    private static final int DEFAULT_THREAD_COUNT = Integer.getInteger("graphcompose.stress.threads", 50);
    private static final int DEFAULT_TOTAL_TASKS = Integer.getInteger("graphcompose.stress.tasks", 5_000);
    private static final long DEFAULT_TIMEOUT_MINUTES = Long.getLong("graphcompose.stress.timeoutMinutes", 1L);

    public static void main(String[] args) {
        BenchmarkSupport.configureQuietLogging();
        System.out.println("Starting GraphCompose stress test...");

        int threadCount = DEFAULT_THREAD_COUNT;
        int totalTasks = DEFAULT_TOTAL_TASKS;
        long timeoutMinutes = DEFAULT_TIMEOUT_MINUTES;

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

        System.out.println("All tasks submitted. Waiting for completion (up to " + timeoutMinutes + " minute(s))...");
        executor.shutdown();

        boolean finished = false;
        try {
            finished = executor.awaitTermination(timeoutMinutes, TimeUnit.MINUTES);
            if (!finished) {
                System.err.println("\nTimeout reached. Threads may be deadlocked or running too slowly.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            System.err.println("\nMain thread was interrupted.");
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Stress test interrupted", e);
        }

        long end = System.currentTimeMillis();

        System.out.println("\n\nStress test summary:");
        System.out.println("Successful: " + successCount.get());
        System.out.println("Errors:     " + errorCount.get());
        System.out.println("Time:       " + (end - start) + " ms");

        if (!finished) {
            throw new IllegalStateException("Stress test timed out after " + timeoutMinutes + " minute(s)");
        }
        if (errorCount.get() > 0) {
            throw new IllegalStateException("Stress test captured " + errorCount.get() + " error(s)");
        }
        if (successCount.get() != totalTasks) {
            throw new IllegalStateException("Stress test completed only " + successCount.get() + " of " + totalTasks + " tasks");
        }
    }

    private static void generateCvInMemory() throws Exception {
        CanonicalBenchmarkSupport.renderSimpleBenchmarkDocument(
                PDRectangle.A4,
                Margin.of(24),
                "StressRoot",
                "Profile",
                "Analytical engineer focused on reliable platform design.");
    }
}
