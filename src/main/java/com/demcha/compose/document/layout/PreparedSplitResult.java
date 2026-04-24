package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.DocumentNode;

/**
 * Result of splitting a prepared semantic node into head/tail pieces.
 */
public record PreparedSplitResult<E extends DocumentNode>(PreparedNode<E> head, PreparedNode<E> tail) {
    public static <E extends DocumentNode> PreparedSplitResult<E> whole(PreparedNode<E> node) {
        return new PreparedSplitResult<>(node, null);
    }

    public boolean splitApplied() {
        return head != null && tail != null;
    }
}



