package com.demcha.compose.document.api;

import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.snapshot.LayoutSnapshot;

import java.util.function.Supplier;

/**
 * Mutable cache for the resolved layout graph and snapshot exposed by
 * {@link DocumentSession}.
 *
 * <p>The cache exposes a monotonically increasing {@link #revision()} counter
 * that the session bumps via {@link #invalidate()} whenever authoring state
 * changes. Stored entries become stale once their captured revision drifts
 * from the current revision; the next {@link #layout(Supplier)} or
 * {@link #snapshot(Supplier)} call recomputes them through the supplied
 * supplier.</p>
 *
 * <p>Instances are not thread-safe — the owning {@link DocumentSession}
 * already documents that contract for the public API.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocumentLayoutCache {
    private long revision;
    private LayoutGraph cachedLayout;
    private long cachedLayoutRevision = -1;
    private LayoutSnapshot cachedSnapshot;
    private long cachedSnapshotRevision = -1;

    DocumentLayoutCache() {
    }

    /**
     * Returns the current revision counter, useful for diagnostic logging.
     *
     * @return current revision value
     */
    long revision() {
        return revision;
    }

    /**
     * Bumps the revision counter and clears both cached entries.
     */
    void invalidate() {
        revision++;
        cachedLayout = null;
        cachedSnapshot = null;
    }

    /**
     * Indicates whether the layout-graph cache still matches the current
     * revision. Callers use this to emit a cache-hit log line before
     * dispatching to {@link #layout(Supplier)}.
     *
     * @return {@code true} when the cached layout graph is still valid
     */
    boolean isLayoutCached() {
        return cachedLayout != null && cachedLayoutRevision == revision;
    }

    /**
     * Returns the cached layout graph or computes a fresh one through the
     * supplied function and stores it for the current revision.
     *
     * @param compute lazy compute path used on cache miss
     * @return cached or freshly computed layout graph
     */
    LayoutGraph layout(Supplier<LayoutGraph> compute) {
        if (isLayoutCached()) {
            return cachedLayout;
        }
        cachedLayout = compute.get();
        cachedLayoutRevision = revision;
        return cachedLayout;
    }

    /**
     * Indicates whether the snapshot cache still matches the current revision.
     *
     * @return {@code true} when the cached snapshot is still valid
     */
    boolean isSnapshotCached() {
        return cachedSnapshot != null && cachedSnapshotRevision == revision;
    }

    /**
     * Returns the cached snapshot or computes a fresh one through the
     * supplied function and stores it for the current revision.
     *
     * @param compute lazy compute path used on cache miss
     * @return cached or freshly computed snapshot
     */
    LayoutSnapshot snapshot(Supplier<LayoutSnapshot> compute) {
        if (isSnapshotCached()) {
            return cachedSnapshot;
        }
        cachedSnapshot = compute.get();
        cachedSnapshotRevision = revision;
        return cachedSnapshot;
    }
}
