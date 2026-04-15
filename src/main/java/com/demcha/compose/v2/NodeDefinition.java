package com.demcha.compose.v2;

import java.util.List;

/**
 * Prepare, split, and fragment emission contract for a semantic node type.
 *
 * @param <E> semantic node type
 */
public interface NodeDefinition<E extends DocumentNode> {
    Class<E> nodeType();

    PreparedNode<E> prepare(E node, PrepareContext ctx, BoxConstraints constraints);

    PaginationPolicy paginationPolicy(E node);

    default PreparedSplitResult<E> split(PreparedNode<E> prepared, SplitRequest request) {
        throw new UnsupportedOperationException("Node type does not support splitting: " + nodeType().getSimpleName());
    }

    default List<DocumentNode> children(E node) {
        return node.children();
    }

    List<LayoutFragment> emitFragments(PreparedNode<E> prepared, FragmentContext ctx, FragmentPlacement placement);
}
