package com.demcha.compose.engine.debug;

/**
 * Stable, test-oriented projection of a single resolved entity.
 *
 * <p>Each node describes one entity after layout and pagination have been
 * resolved. The identity is intentionally based on tree order plus semantic
 * naming so diffs stay deterministic and understandable in committed JSON
 * baselines.</p>
 *
 * @param path full deterministic node path built from parent path and local segment
 * @param entityName semantic entity name when available, otherwise {@code null}
 * @param entityKind stable render or entity kind used as a fallback identity hint
 * @param parentPath full parent path, or {@code null} for root nodes
 * @param childIndex zero-based position among siblings
 * @param depth resolved tree depth for debugging and ordering checks
 * @param layer resolved layer value after layout
 * @param computedX resolved computed x-coordinate
 * @param computedY resolved computed y-coordinate
 * @param placementX resolved placement box x-coordinate
 * @param placementY resolved placement box y-coordinate
 * @param placementWidth resolved placement box width
 * @param placementHeight resolved placement box height
 * @param startPage zero-based first page touched by this node
 * @param endPage zero-based last page touched by this node
 * @param contentWidth resolved content width
 * @param contentHeight resolved content height
 * @param margin normalized margin insets captured for debugging
 * @param padding normalized padding insets captured for debugging
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
