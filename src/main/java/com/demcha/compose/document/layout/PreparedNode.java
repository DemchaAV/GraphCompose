package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.DocumentNode;

import java.util.Objects;
import java.util.Optional;

/**
 * Prepared semantic node with reusable measure and layout payload.
 *
 * @param <E> semantic node type
 * @param node source semantic node
 * @param measureResult measured node size
 * @param preparedLayout node-specific reusable layout payload
 * @param compositeLayout optional composite child-layout metadata
 */
public record PreparedNode<E extends DocumentNode>(
        E node,
        MeasureResult measureResult,
        PreparedNodeLayout preparedLayout,
        Optional<CompositeLayoutSpec> compositeLayout
) {
    private enum EmptyPreparedLayout implements PreparedNodeLayout {
        INSTANCE
    }

    /**
     * Normalizes optional layout payloads and validates required node state.
     */
    public PreparedNode {
        node = Objects.requireNonNull(node, "node");
        measureResult = Objects.requireNonNull(measureResult, "measureResult");
        preparedLayout = preparedLayout == null ? EmptyPreparedLayout.INSTANCE : preparedLayout;
        compositeLayout = compositeLayout == null ? Optional.empty() : compositeLayout;
    }

    /**
     * Creates a prepared leaf node without a custom layout payload.
     *
     * @param node semantic node
     * @param measureResult measured node size
     * @param <E> semantic node type
     * @return prepared leaf node
     */
    public static <E extends DocumentNode> PreparedNode<E> leaf(E node, MeasureResult measureResult) {
        return new PreparedNode<>(node, measureResult, EmptyPreparedLayout.INSTANCE, Optional.empty());
    }

    /**
     * Creates a prepared leaf node with a reusable layout payload.
     *
     * @param node semantic node
     * @param measureResult measured node size
     * @param preparedLayout node-specific layout payload
     * @param <E> semantic node type
     * @return prepared leaf node
     */
    public static <E extends DocumentNode> PreparedNode<E> leaf(E node,
                                                                MeasureResult measureResult,
                                                                PreparedNodeLayout preparedLayout) {
        return new PreparedNode<>(node, measureResult, preparedLayout, Optional.empty());
    }

    /**
     * Creates a prepared composite node without a custom layout payload.
     *
     * @param node semantic node
     * @param measureResult measured node size
     * @param compositeLayout composite child layout metadata
     * @param <E> semantic node type
     * @return prepared composite node
     */
    public static <E extends DocumentNode> PreparedNode<E> composite(E node,
                                                                     MeasureResult measureResult,
                                                                     CompositeLayoutSpec compositeLayout) {
        return new PreparedNode<>(node, measureResult, EmptyPreparedLayout.INSTANCE, Optional.of(compositeLayout));
    }

    /**
     * Creates a prepared composite node with a reusable layout payload.
     *
     * @param node semantic node
     * @param measureResult measured node size
     * @param preparedLayout node-specific layout payload
     * @param compositeLayout composite child layout metadata
     * @param <E> semantic node type
     * @return prepared composite node
     */
    public static <E extends DocumentNode> PreparedNode<E> composite(E node,
                                                                     MeasureResult measureResult,
                                                                     PreparedNodeLayout preparedLayout,
                                                                     CompositeLayoutSpec compositeLayout) {
        return new PreparedNode<>(node, measureResult, preparedLayout, Optional.of(compositeLayout));
    }

    /**
     * Indicates whether this node carries composite child layout metadata.
     *
     * @return true for composite nodes
     */
    public boolean isComposite() {
        return compositeLayout.isPresent();
    }

    /**
     * Returns required composite child layout metadata.
     *
     * @return composite layout metadata
     */
    public CompositeLayoutSpec requireCompositeLayout() {
        return compositeLayout.orElseThrow(() ->
                new IllegalStateException("Prepared node '" + node.nodeKind() + "' does not have composite layout metadata."));
    }

    /**
     * Returns the prepared layout payload as a concrete type.
     *
     * @param layoutType required layout payload type
     * @param <T> layout payload type
     * @return typed prepared layout payload
     */
    public <T extends PreparedNodeLayout> T requirePreparedLayout(Class<T> layoutType) {
        Objects.requireNonNull(layoutType, "layoutType");
        if (!layoutType.isInstance(preparedLayout)) {
            throw new IllegalStateException("Prepared node '" + node.nodeKind()
                    + "' does not carry layout payload " + layoutType.getSimpleName() + ".");
        }
        return layoutType.cast(preparedLayout);
    }
}



