package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.dsl.DocumentDsl;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stress test for concurrent {@link DocumentSession} usage. Drives N
 * independent sessions in parallel and asserts each produces a
 * well-formed PDF (validates no race conditions in shared font /
 * registry / cache state) and that sessions seeded with identical
 * content produce identical layout graphs (validates layout
 * determinism under concurrency).
 *
 * <p>Each {@link DocumentSession} is single-threaded by contract — the
 * stress test exercises a fleet of <strong>independent</strong>
 * sessions, each owned by exactly one thread, not concurrent access to
 * a single session. The guarantee under test is that the
 * <em>process-wide</em> machinery (default font registry, glyph cache,
 * built-in node definitions, shape outline cache) handles concurrent
 * lookup safely.</p>
 *
 * <p>Tracked as Track I1 / I2 in the v1.6.6 readiness taskboard.</p>
 */
class DocumentSessionParallelStressTest {

    private static final int THREAD_COUNT = 32;
    private static final int ITERATIONS = 4;
    private static final long TIMEOUT_SECONDS = 60;

    @Test
    void identicalSessionsInParallelProduceIdenticalLayoutGraphs() throws Exception {
        // The layout graph is the canonical deterministic snapshot — PDF
        // bytes may differ across runs (xref hashes, resource-stream
        // ordering) but the layout structure must be bit-stable. If
        // concurrent runs ever surface a different layout-graph
        // toString() than the sequential baseline, we have shared
        // mutable state racing somewhere in the prepare/measure pipeline.
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            String baseline = renderLayoutSignature();
            assertThat(baseline)
                    .as("sequential baseline must be non-empty")
                    .isNotBlank();

            Set<String> signatures = runParallel(this::renderLayoutSignature);
            assertThat(signatures)
                    .as("parallel iteration %d — every thread should produce the baseline layout", iteration)
                    .containsExactly(baseline);
        }
    }

    @Test
    void independentSessionsInParallelProduceValidPdfBytes() throws Exception {
        // Each thread builds its own document and writes a PDF. We don't
        // assert byte-identity — that would over-specify (PDF timestamps,
        // resource ordering). We assert each output starts with the PDF
        // magic %PDF and has plausible size, which is enough to catch
        // any thread that errored out or produced corrupted bytes.
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            Set<Integer> sizes = runParallel(() -> {
                byte[] pdf = renderPdfBytes();
                assertThat(pdf)
                        .as("each PDF must be present and non-empty")
                        .isNotEmpty();
                // 256 bytes is the smallest plausible PDF — even a single-page
                // empty document carries header + catalog + xref + trailer well
                // past that. Anything smaller means the renderer truncated.
                assertThat(pdf.length)
                        .as("each PDF should be at least 256 bytes")
                        .isGreaterThan(256);
                assertThat(new String(pdf, 0, 4))
                        .as("each PDF must carry the %%PDF magic")
                        .isEqualTo("%PDF");
                return pdf.length;
            });
            // All identical content → byte sizes should be within a tight
            // range. We don't lock the exact size (CreationDate / xref
            // offsets can drift by a handful of bytes between threads)
            // but legit metadata variance never crosses ~256 bytes for a
            // fixed-content render; anything past that points at content
            // corruption. Recalibrate this threshold if a PDFBox bump
            // makes legitimate variance bigger.
            int min = sizes.stream().mapToInt(Integer::intValue).min().orElseThrow();
            int max = sizes.stream().mapToInt(Integer::intValue).max().orElseThrow();
            assertThat(max - min)
                    .as("parallel iteration %d — PDF size variance suggests non-deterministic content (min=%d max=%d)",
                            iteration, min, max)
                    .isLessThan(256);
        }
    }

    private <T> Set<T> runParallel(Callable<T> task) throws Exception {
        // The CountDownLatch pair forms a "start-gun" barrier so all
        // THREAD_COUNT workers hit the shared state in the same nanosecond
        // instead of trickling in over the thread-pool ramp-up. Maximises
        // the chance of triggering a race condition; standard pattern for
        // concurrent unit tests.
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        try {
            CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
            CountDownLatch start = new CountDownLatch(1);
            List<Callable<T>> tasks = new ArrayList<>(THREAD_COUNT);
            for (int i = 0; i < THREAD_COUNT; i++) {
                tasks.add(() -> {
                    ready.countDown();
                    start.await();
                    return task.call();
                });
            }
            // Submit all tasks first; they each block on `start`.
            List<Future<T>> futures = new ArrayList<>(THREAD_COUNT);
            for (Callable<T> wrapped : tasks) {
                futures.add(executor.submit(wrapped));
            }
            // Wait for every worker to reach the barrier, then fire.
            ready.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            start.countDown();

            // Collect results. A HashSet collapses identical entries —
            // when the test asserts containsExactly(baseline), a size-1
            // set means every thread agreed on the baseline value.
            Set<T> results = new HashSet<>();
            for (Future<T> future : futures) {
                results.add(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    /** Sequential render — returns the layout-graph toString for parity checks. */
    private String renderLayoutSignature() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 400)
                .margin(DocumentInsets.of(20))
                .create()) {

            DocumentDsl dsl = session.dsl();
            dsl.pageFlow()
                    .name("StressFlow")
                    .module("Header", module -> module
                            .paragraph(p -> p.text("Concurrent stress: header")
                                    .textStyle(DocumentTextStyle.DEFAULT)))
                    .module("Body", module -> module
                            .paragraph(p -> p.text("Lorem ipsum dolor sit amet.")
                                    .textStyle(DocumentTextStyle.DEFAULT))
                            .paragraph(p -> p.text("Consectetur adipiscing elit.")
                                    .textStyle(DocumentTextStyle.DEFAULT)))
                    .build();

            return session.layoutGraph().toString();
        }
    }

    /** Sequential render — returns the bytes of a small in-memory PDF. */
    private byte[] renderPdfBytes() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 400)
                .margin(DocumentInsets.of(20))
                .create()) {

            DocumentDsl dsl = session.dsl();
            dsl.pageFlow()
                    .name("StressFlow")
                    .module("Body", module -> module
                            .paragraph(p -> p.text("Concurrent stress: body")
                                    .textStyle(DocumentTextStyle.DEFAULT)))
                    .build();

            return session.toPdfBytes();
        }
    }
}
