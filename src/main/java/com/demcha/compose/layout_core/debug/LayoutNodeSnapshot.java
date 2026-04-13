package com.demcha.compose.layout_core.debug;

/**
 * Stable, test-oriented projection of one resolved entity.
 */
public record LayoutNodeSnapshot(
        String path,
        String entityName,
        String entityKind,
        String parentPath,
        int childIndex,
        int depth,
        int layer,
        double computedX,
        double computedY,
        double placementX,
        double placementY,
        double placementWidth,
        double placementHeight,
        int startPage,
        int endPage,
        double contentWidth,
        double contentHeight,
        LayoutInsetsSnapshot margin,
        LayoutInsetsSnapshot padding) {
}
