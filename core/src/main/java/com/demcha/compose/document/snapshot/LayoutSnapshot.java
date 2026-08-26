package com.demcha.compose.document.snapshot;

import java.util.List;

/**
 * Deterministic, renderer-agnostic snapshot of a fully resolved document.
 *
 * <p>Instances are produced after layout and pagination have completed, but
 * before any backend renders bytes. The payload intentionally contains only
 * stable layout state so JSON baselines stay readable and maintainable.</p>
 *
 * <p>This shape is frozen on purpose. Optional diagnostics — typography today,
 * others later — live on {@link LayoutDiagnosticSnapshot}, which wraps this
 * record rather than extending it, so a consumer's committed baseline stays
 * byte-identical however they serialize it: through
 * {@code LayoutSnapshotJson}, through a mapper of their own, or through
 * {@code toString()}.</p>
 *
 * @param formatVersion snapshot schema version used to interpret the JSON shape
 * @param canvas        resolved canvas metadata for the composed document
 * @param totalPages    total number of pages touched by the resolved layout
 * @param nodes         deterministic depth-first list of resolved document nodes
 * @author Artem Demchyshyn
 */
public record LayoutSnapshot(
        String formatVersion,
        LayoutCanvasSnapshot canvas,
        int totalPages,
        List<LayoutNodeSnapshot> nodes) {
}
