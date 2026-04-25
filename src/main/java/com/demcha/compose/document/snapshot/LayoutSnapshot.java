package com.demcha.compose.document.snapshot;

import java.util.List;

/**
 * Deterministic, renderer-agnostic snapshot of a fully resolved document.
 *
 * <p>Instances are produced after layout and pagination have completed, but
 * before any backend renders bytes. The payload intentionally contains only
 * stable layout state so JSON baselines stay readable and maintainable.</p>
 *
 * @param formatVersion snapshot schema version used to interpret the JSON shape
 * @param canvas resolved canvas metadata for the composed document
 * @param totalPages total number of pages touched by the resolved layout
 * @param nodes deterministic depth-first list of resolved document nodes
 */
public record LayoutSnapshot(
        String formatVersion,
        LayoutCanvasSnapshot canvas,
        int totalPages,
        List<LayoutNodeSnapshot> nodes) {
}
