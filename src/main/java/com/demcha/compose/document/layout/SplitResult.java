package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.DocumentNode;

/**
 * Result of splitting a semantic node into head/tail pieces.
 *
 * @param <E> semantic node type
 * @param head first semantic node fragment, or the whole node when unsplit
 * @param tail remaining semantic node fragment, or {@code null} when unsplit
 */
public record SplitResult<E extends DocumentNode>(E head, E tail) {
    /**
     * Creates an unsplit result where the original node remains whole.
     *
     * @param node semantic node
     * @param <E> semantic node type
     * @return whole-node split result
     */
    public static <E extends DocumentNode> SplitResult<E> whole(E node) {
        return new SplitResult<>(node, null);
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



