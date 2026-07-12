package com.demcha.compose.engine.components.content;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Fixed-capacity, thread-safe memoization cache with access-order (LRU)
 * eviction. Once {@code maxEntries} distinct keys are held, the next insertion
 * evicts the least-recently-used entry, so a long-lived JVM that keeps
 * accumulating unrelated keys cannot grow the cache without bound.
 *
 * <p>Unlike the per-thread font caches in the PDF backend, the image caches are
 * shared across render threads on purpose: a decoded image should be reused by
 * every thread and its (potentially large) bytes held once, not duplicated per
 * thread. Sharing an access-order map needs synchronization — a cache hit
 * mutates the recency order — but the value function runs <em>outside</em> the
 * lock, so a slow file read or image decode never blocks another thread's cache
 * access. Two threads racing on the same absent key may both compute it; because
 * the cache memoizes a deterministic function, the duplicate compute is wasted
 * work only, and every caller ends up sharing the first stored value.
 */
final class BoundedLruCache<K, V> {

    private final int maxEntries;
    private final Map<K, V> entries;

    BoundedLruCache(int maxEntries) {
        if (maxEntries < 1) {
            throw new IllegalArgumentException("maxEntries must be >= 1, got " + maxEntries);
        }
        this.maxEntries = maxEntries;
        this.entries = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > BoundedLruCache.this.maxEntries;
            }
        };
    }

    /**
     * Returns the value cached under {@code key}, computing and storing it with
     * {@code mappingFunction} on a miss. The mapping function must not return
     * {@code null} and runs outside the cache lock.
     */
    V getOrCompute(K key, Function<? super K, ? extends V> mappingFunction) {
        synchronized (entries) {
            V cached = entries.get(key);
            if (cached != null) {
                return cached;
            }
        }
        V computed = Objects.requireNonNull(mappingFunction.apply(key), "mappingFunction must not return null");
        synchronized (entries) {
            V raced = entries.putIfAbsent(key, computed);
            return raced != null ? raced : computed;
        }
    }

    int size() {
        synchronized (entries) {
            return entries.size();
        }
    }

    void clear() {
        synchronized (entries) {
            entries.clear();
        }
    }
}
