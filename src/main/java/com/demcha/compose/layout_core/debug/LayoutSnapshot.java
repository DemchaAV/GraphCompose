package com.demcha.compose.layout_core.debug;

import java.util.List;

/**
 * Resolved document snapshot captured after layout and pagination.
 */
public record LayoutSnapshot(
        String formatVersion,
        LayoutCanvasSnapshot canvas,
        int totalPages,
        List<LayoutNodeSnapshot> nodes) {
}
