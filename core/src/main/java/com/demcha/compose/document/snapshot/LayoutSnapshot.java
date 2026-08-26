package com.demcha.compose.document.snapshot;

import java.util.List;

/**
 * Deterministic, renderer-agnostic snapshot of a fully resolved document.
 *
 * <p>Instances are produced after layout and pagination have completed, but
 * before any backend renders bytes. The payload intentionally contains only
 * stable layout state so JSON baselines stay readable and maintainable.</p>
 *
 * <p>{@code typography} is <em>opt-in</em> and empty unless the snapshot was
 * taken through
 * {@link com.demcha.compose.document.api.DocumentSession#layoutSnapshot(LayoutSnapshotOptions)}
 * with {@link LayoutSnapshotOptions.Builder#typography(boolean)} enabled. That
 * keeps a snapshot taken the ordinary way byte-identical to the baselines
 * consumers already have on disk.</p>
 *
 * <p>It is a parallel list rather than a field on each node because text belongs
 * to fragments: a paragraph that breaks across a page boundary produces one entry
 * per page, and a per-node projection would have to keep one and discard the
 * other. Join it to {@code nodes} on {@link LayoutTypographySnapshot#path()} —
 * one-to-many, since a node such as a chart owns many text fragments.</p>
 *
 * @param formatVersion snapshot schema version used to interpret the JSON shape
 * @param canvas        resolved canvas metadata for the composed document
 * @param totalPages    total number of pages touched by the resolved layout
 * @param nodes         deterministic depth-first list of resolved document nodes
 * @param typography    resolved paragraph text runs in deterministic order; empty
 *                      unless typography was requested, and empty for a document
 *                      whose only text is drawn outside the paragraph pipeline
 *                      ({@code @since 2.2.2})
 * @author Artem Demchyshyn
 */
public record LayoutSnapshot(
        String formatVersion,
        LayoutCanvasSnapshot canvas,
        int totalPages,
        List<LayoutNodeSnapshot> nodes,
        List<LayoutTypographySnapshot> typography) {

    /**
     * Freezes both lists so a snapshot cannot be mutated after extraction.
     */
    public LayoutSnapshot {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        typography = typography == null ? List.of() : List.copyOf(typography);
    }

    /**
     * Creates a snapshot with no optional diagnostic section — the shape this
     * record had before {@code typography} was added.
     *
     * <p>Load-bearing rather than convenient: it restores the constructor
     * descriptor the binary-compatibility baseline pins, so adding the
     * {@code typography} component stays a compatible change for consumers
     * compiled against an earlier 2.x. Do not remove it because the repo has few
     * callers.</p>
     *
     * @param formatVersion snapshot schema version
     * @param canvas        resolved canvas metadata
     * @param totalPages    total number of pages
     * @param nodes         resolved document nodes
     */
    public LayoutSnapshot(String formatVersion,
                          LayoutCanvasSnapshot canvas,
                          int totalPages,
                          List<LayoutNodeSnapshot> nodes) {
        this(formatVersion, canvas, totalPages, nodes, List.of());
    }
}
