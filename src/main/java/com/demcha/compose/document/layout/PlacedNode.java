package com.demcha.compose.document.layout;

import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;

/**
 * Canonical laid-out semantic node entry in the v2 layout graph.
 */
public record PlacedNode(
        String path,
        String semanticName,
        String nodeKind,
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
        Margin margin,
        Padding padding
) {
}


