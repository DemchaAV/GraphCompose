package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.DocumentNode;

import java.util.List;

/**
 * Prepare, split, and fragment emission contract for a semantic node type.
 *
 * @param <E> semantic node type
 */
public interface NodeDefinition<E extends DocumentNode> {

    /**
     * Returns the semantic node type handled by this definition.
     *
     * @return supported node type
     */
    Class<E> nodeType();

    /**
     * Prepares one semantic node for later measurement, pagination, and fragment emission.
     *
     * @param node semantic node instance being compiled
     * @param ctx preparation context with measurement services and caches
     * @param constraints available layout space for this compilation step
     * @return prepared node carrying measured dimensions and optional prepared layout data
     */
    PreparedNode<E> prepare(E node, PrepareContext ctx, BoxConstraints constraints);

    /**
     * Declares the pagination behavior for the supplied semantic node.
     *
     * @param node semantic node instance
     * @return pagination policy used by the compiler
     */
    PaginationPolicy paginationPolicy(E node);

    /**
     * Splits a prepared node when the current page cannot fit it whole.
     *
     * @param prepared prepared node candidate
     * @param request split request describing remaining page space
     * @return split head/tail result, or throws when the node does not support splitting
     * @throws UnsupportedOperationException when the definition does not implement splitting
     */
    default PreparedSplitResult<E> split(PreparedNode<E> prepared, SplitRequest request) {
        throw new UnsupportedOperationException("Node type does not support splitting: " + nodeType().getSimpleName());
    }

    /**
     * Returns semantic children visible to the compiler for composite layout.
     *
     * @param node semantic node instance
     * @return child nodes in canonical layout order
     */
    default List<DocumentNode> children(E node) {
        return node.children();
    }

    /**
     * Emits renderer-facing fragments for one fully placed semantic node.
     *
     * @param prepared prepared node being emitted
     * @param ctx fragment emission context
     * @param placement resolved placement for the node
     * @return immutable list of emitted layout fragments
     */
    List<LayoutFragment> emitFragments(PreparedNode<E> prepared, FragmentContext ctx, FragmentPlacement placement);

    /**
     * Emits renderer-facing overlay fragments that must appear <em>after</em>
     * a composite node's children. Default is empty.
     *
     * <p>Most node types do not need this hook — opening decorations
     * (backgrounds, borders, outlines) already render before children via
     * {@link #emitFragments(PreparedNode, FragmentContext, FragmentPlacement)}.
     * The overlay hook exists for paired begin/end markers such as the
     * graphics-state save/restore pair that
     * {@code ShapeContainerNode} uses to clip its layers to the outline:
     * the clip-begin fragment is emitted via {@code emitFragments} (so it
     * sits behind the children), and the matching clip-end fragment is
     * emitted here (so it sits after the children, restoring graphics
     * state on the same page).</p>
     *
     * @param prepared prepared node being emitted
     * @param ctx fragment emission context
     * @param placement resolved placement for the node
     * @return immutable list of overlay fragments, possibly empty
     */
    default List<LayoutFragment> emitOverlayFragments(PreparedNode<E> prepared,
                                                      FragmentContext ctx,
                                                      FragmentPlacement placement) {
        return List.of();
    }
}



