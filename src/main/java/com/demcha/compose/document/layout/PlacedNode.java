package com.demcha.compose.document.layout;

import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;

/**
 * Canonical laid-out semantic node entry in the v2 layout graph.
 *
 * @param path stable semantic path for the node
 * @param semanticName author-provided semantic name
 * @param nodeKind canonical node kind
 * @param parentPath parent semantic path, or {@code null} for a root
 * @param childIndex index within the parent child list
 * @param depth depth in the semantic tree
 * @param layer render/layout layer
 * @param computedX computed x coordinate before final placement offsets
 * @param computedY computed y coordinate before final placement offsets
 * @param placementX final placed x coordinate
 * @param placementY final placed y coordinate
 * @param placementWidth final placed width
 * @param placementHeight final placed height
 * @param startPage first page occupied by the node
 * @param endPage last page occupied by the node
 * @param contentWidth measured content width
 * @param contentHeight measured content height
 * @param margin resolved outer margin
 * @param padding resolved inner padding
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


