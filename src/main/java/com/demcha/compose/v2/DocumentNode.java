package com.demcha.compose.v2;

import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;

import java.util.List;

/**
 * Semantic authoring node in the GraphCompose v2 document graph.
 */
public interface DocumentNode {

    /**
     * Optional semantic name used for snapshots and diagnostics.
     */
    String name();

    /**
     * Outer spacing around the node.
     */
    default Margin margin() {
        return Margin.zero();
    }

    /**
     * Inner spacing inside the node box.
     */
    default Padding padding() {
        return Padding.zero();
    }

    /**
     * Child semantic nodes. Leaf nodes return an empty list.
     */
    default List<DocumentNode> children() {
        return List.of();
    }

    /**
     * Stable logical kind for diagnostics and snapshots.
     */
    default String nodeKind() {
        return getClass().getSimpleName();
    }
}
