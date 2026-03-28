package com.demcha.compose.devtool;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class CoalescingRefreshWorkerTest {

    @Test
    void shouldKeepOnlyOneTrailingRefreshWhileWorkerIsBusy() throws Exception {
        var executor = Executors.newSingleThreadExecutor();
        var processed = new CopyOnWriteArrayList<CoalescingRefreshWorker.RevisionedRequest<String>>();
        var firstStarted = new CountDownLatch(1);
        var allowFirstToFinish = new CountDownLatch(1);
        var allRefreshesFinished = new CountDownLatch(2);

        try {
            var worker = new CoalescingRefreshWorker<String>(
                    executor,
                    (left, right) -> left + "|" + right,
                    request -> {
                        processed.add(request);
                        if (processed.size() == 1) {
                            firstStarted.countDown();
                            try {
                                allowFirstToFinish.await(1, TimeUnit.SECONDS);
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                        }
                        allRefreshesFinished.countDown();
                    });

            worker.offer("first");
            worker.start();
            assertThat(firstStarted.await(1, TimeUnit.SECONDS)).isTrue();

            worker.offer("second");
            worker.start();
            worker.offer("third");
            worker.start();

            allowFirstToFinish.countDown();

            assertThat(allRefreshesFinished.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(processed).extracting(CoalescingRefreshWorker.RevisionedRequest::request)
                    .containsExactly("first", "second|third");
            assertThat(processed.get(1).revision()).isEqualTo(worker.latestRevision());
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }
}
