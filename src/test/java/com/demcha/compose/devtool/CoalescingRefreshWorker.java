package com.demcha.compose.devtool;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;

/**
 * Coalesces refresh requests so that only one worker is in flight while keeping
 * the newest merged request queued as a trailing refresh.
 */
final class CoalescingRefreshWorker<T> {
    private final Executor executor;
    private final BinaryOperator<T> merger;
    private final Consumer<RevisionedRequest<T>> consumer;
    private final AtomicLong latestRevision = new AtomicLong();
    private final AtomicReference<PendingRequest<T>> pendingRequest = new AtomicReference<>();
    private final AtomicBoolean workerScheduled = new AtomicBoolean();

    CoalescingRefreshWorker(Executor executor,
                            BinaryOperator<T> merger,
                            Consumer<RevisionedRequest<T>> consumer) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.merger = Objects.requireNonNull(merger, "merger");
        this.consumer = Objects.requireNonNull(consumer, "consumer");
    }

    void offer(T request) {
        Objects.requireNonNull(request, "request");

        long revision = latestRevision.incrementAndGet();
        pendingRequest.getAndUpdate(existing -> existing == null
                ? new PendingRequest<>(revision, request)
                : new PendingRequest<>(revision, merger.apply(existing.request(), request)));
    }

    void start() {
        if (!workerScheduled.compareAndSet(false, true)) {
            return;
        }

        try {
            executor.execute(this::drainLoop);
        } catch (RejectedExecutionException ex) {
            workerScheduled.set(false);
        }
    }

    long latestRevision() {
        return latestRevision.get();
    }

    private void drainLoop() {
        try {
            while (true) {
                PendingRequest<T> next = pendingRequest.getAndSet(null);
                if (next == null) {
                    return;
                }

                consumer.accept(new RevisionedRequest<>(next.revision(), next.request()));
            }
        } finally {
            workerScheduled.set(false);
            if (pendingRequest.get() != null) {
                start();
            }
        }
    }

    record RevisionedRequest<T>(long revision, T request) {
    }

    private record PendingRequest<T>(long revision, T request) {
    }
}
