package com.demcha.compose.engine.debug;

import java.util.List;

/**
 * Deterministic, renderer-agnostic snapshot of a fully resolved document.
 *
 * <p>Instances of this record are produced by
 * {@link com.demcha.compose.document.api.DocumentSession#layoutSnapshot()}
 * after layout and pagination have completed, but before the PDF renderer
 * draws anything. The record is intended for debugging, layout-regression
 * tests, and tooling that needs geometry rather than rendered pixels.</p>
 *
 * <p>The payload intentionally contains only stable layout state so that JSON
 * baselines stay readable and maintainable across normal engine evolution.</p>
 *
 * @param formatVersion snapshot schema version used to interpret the JSON shape
 * @param canvas resolved canvas metadata for the composed document
 * @param totalPages total number of pages touched by the resolved layout
 * @param nodes deterministic depth-first list of resolved entity nodes
 */
public record LayoutSnapshot(
        String formatVersion,
        LayoutCanvasSnapshot canvas,
        int totalPages,
        List<LayoutNodeSnapshot> nodes) {
}
