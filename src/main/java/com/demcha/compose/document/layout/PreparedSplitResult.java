package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.DocumentNode;

/**
 * Result of splitting a prepared semantic node into head/tail pieces.
 *
 * @param <E> semantic node type
 * @param head first prepared node fragment, or the whole node when unsplit
 * @param tail remaining prepared node fragment, or {@code null} when unsplit
 */
public record PreparedSplitResult<E extends DocumentNode>(PreparedNode<E> head, PreparedNode<E> tail) {
    /**
     * Creates an unsplit result where the original node remains whole.
     *
     * @param node prepared node
     * @param <E> semantic node type
     * @return whole-node split result
     */
    public static <E extends DocumentNode> PreparedSplitResult<E> whole(PreparedNode<E> node) {
        return new PreparedSplitResult<>(node, null);
    }

    /**
     * Indicates whether this result actually split the source node.
     *
     * @return true when both head and tail are present
     */
    public boolean splitApplied() {
        return head != null && tail != null;
    }
}



