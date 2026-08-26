package com.demcha.compose.document.snapshot;

import java.util.List;

/**
 * Deterministic, renderer-agnostic snapshot of a fully resolved document.
 *
 * <p>Instances are produced after layout and pagination have completed, but
 * before any backend renders bytes. The payload intentionally contains only
 * stable layout state so JSON baselines stay readable and maintainable.</p>
 *
 * <p>{@code typography} is a parallel list rather than a field on each node,
 * because text belongs to fragments: a paragraph that breaks across a page
 * boundary produces one entry per page, and a per-node projection would have to
 * keep one and discard the other. Join it to {@code nodes} on
 * {@link LayoutTypographySnapshot#path()}.</p>
 *
 * @param formatVersion snapshot schema version used to interpret the JSON shape
 * @param canvas        resolved canvas metadata for the composed document
 * @param totalPages    total number of pages touched by the resolved layout
 * @param nodes         deterministic depth-first list of resolved document nodes
 * @param typography    resolved text runs in deterministic order, or empty when
 *                      the document contains no text
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
     * Creates a snapshot with no typography.
     *
     * <p>Keeps the shape callers written before 2.2.2 use compiling. It is not a
     * way to opt out of typography: a snapshot built this way reports a document
     * with no text, which is only true if that is what it has.</p>
     *
     * @param formatVersion snapshot schema version
     * @param canvas        resolved canvas metadata
     * @param totalPages    total number of pages
     * @param nodes         resolved document nodes
     * @since 2.2.2
     */
    public LayoutSnapshot(String formatVersion,
                          LayoutCanvasSnapshot canvas,
                          int totalPages,
                          List<LayoutNodeSnapshot> nodes) {
        this(formatVersion, canvas, totalPages, nodes, List.of());
    }
}
