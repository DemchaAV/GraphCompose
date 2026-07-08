package com.demcha.compose.engine.components.content;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit coverage for {@link BoundedLruCache}: memoization on a hit, access-order
 * (LRU) eviction once over capacity, argument validation, and the shared
 * single-value guarantee under concurrent misses on the same key.
 */
class BoundedLruCacheTest {

    @Test
    void computesOnceThenReturnsTheCachedValueOnRepeat() {
        BoundedLruCache<String, String> cache = new BoundedLruCache<>(4);
        Map<String, Integer> computeCounts = new HashMap<>();
        Function<String, String> compute = key -> {
            computeCounts.merge(key, 1, Integer::sum);
            return key.toUpperCase();
        };

        assertThat(cache.getOrCompute("a", compute)).isEqualTo("A");
        assertThat(cache.getOrCompute("a", compute)).isEqualTo("A");

        assertThat(computeCounts).containsEntry("a", 1);
        assertThat(cache.size()).isEqualTo(1);
    }

    @Test
    void evictsLeastRecentlyUsedAndKeepsRecentlyAccessedOverCapacity() {
        BoundedLruCache<String, String> cache = new BoundedLruCache<>(2);
        Map<String, Integer> computeCounts = new HashMap<>();
        Function<String, String> compute = key -> {
            computeCounts.merge(key, 1, Integer::sum);
            return key.toUpperCase();
        };

        cache.getOrCompute("a", compute);          // {a}
        cache.getOrCompute("b", compute);          // {a, b}
        cache.getOrCompute("a", compute);          // hit — touches a, so b is now least-recently-used
        cache.getOrCompute("c", compute);          // inserting c evicts b, not a
        cache.getOrCompute("a", compute);          // still a hit — a was never evicted
        cache.getOrCompute("b", compute);          // b was evicted — recomputed

        assertThat(cache.size()).isEqualTo(2);
        assertThat(computeCounts)
                .containsEntry("a", 1)   // computed once, kept hot by the access touch
                .containsEntry("b", 2)   // evicted then recomputed
                .containsEntry("c", 1);
    }

    @Test
    void constructorRejectsNonPositiveCapacity() {
        assertThatThrownBy(() -> new BoundedLruCache<String, String>(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxEntries");
        assertThatThrownBy(() -> new BoundedLruCache<String, String>(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxEntries");
    }

    @Test
    void getOrComputeRejectsNullFromTheMappingFunction() {
        BoundedLruCache<String, String> cache = new BoundedLruCache<>(4);

        assertThatThrownBy(() -> cache.getOrCompute("a", key -> null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("mappingFunction");
    }

    @Test
    void clearEmptiesTheCache() {
        BoundedLruCache<String, String> cache = new BoundedLruCache<>(4);
        cache.getOrCompute("a", String::toUpperCase);

        cache.clear();

        assertThat(cache.size()).isZero();
    }

    @Test
    void concurrentMissesOnTheSameKeyAllShareOneStoredValue() throws Exception {
        BoundedLruCache<String, Object> cache = new BoundedLruCache<>(4);
        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Object>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    // Every call returns a fresh Object; only a shared cache can
                    // hand every thread the same instance back.
                    return cache.getOrCompute("k", key -> new Object());
                }));
            }
            start.countDown();

            Object shared = results.get(0).get(5, TimeUnit.SECONDS);
            for (Future<Object> result : results) {
                assertThat(result.get(5, TimeUnit.SECONDS)).isSameAs(shared);
            }
            assertThat(cache.size()).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void distinctKeysStayBoundedByCapacityUnderConcurrency() throws Exception {
        int capacity = 8;
        BoundedLruCache<Integer, Integer> cache = new BoundedLruCache<>(capacity);
        int threads = 12;
        int keysPerThread = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<?>> done = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                done.add(pool.submit(() -> {
                    start.await();
                    for (int k = 0; k < keysPerThread; k++) {
                        cache.getOrCompute(k, key -> key * key);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> f : done) {
                f.get(5, TimeUnit.SECONDS);
            }
            assertThat(cache.size()).isLessThanOrEqualTo(capacity);
        } finally {
            pool.shutdownNow();
        }
    }
}
