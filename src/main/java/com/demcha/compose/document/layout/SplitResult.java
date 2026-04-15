package com.demcha.compose.document.layout;

import com.demcha.compose.document.model.node.DocumentNode;

/**
 * Result of splitting a semantic node into head/tail pieces.
 */
public record SplitResult<E extends DocumentNode>(E head, E tail) {
    public static <E extends DocumentNode> SplitResult<E> whole(E node) {
        return new SplitResult<>(node, null);
    }

    public boolean splitApplied() {
        return head != null && tail != null;
    }
}



