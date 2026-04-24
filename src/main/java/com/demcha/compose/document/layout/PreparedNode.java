package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.DocumentNode;

import java.util.Objects;
import java.util.Optional;

/**
 * Prepared semantic node with reusable measure and layout payload.
 *
 * @param <E> semantic node type
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

    public PreparedNode {
        node = Objects.requireNonNull(node, "node");
        measureResult = Objects.requireNonNull(measureResult, "measureResult");
        preparedLayout = preparedLayout == null ? EmptyPreparedLayout.INSTANCE : preparedLayout;
        compositeLayout = compositeLayout == null ? Optional.empty() : compositeLayout;
    }

    public static <E extends DocumentNode> PreparedNode<E> leaf(E node, MeasureResult measureResult) {
        return new PreparedNode<>(node, measureResult, EmptyPreparedLayout.INSTANCE, Optional.empty());
    }

    public static <E extends DocumentNode> PreparedNode<E> leaf(E node,
                                                                MeasureResult measureResult,
                                                                PreparedNodeLayout preparedLayout) {
        return new PreparedNode<>(node, measureResult, preparedLayout, Optional.empty());
    }

    public static <E extends DocumentNode> PreparedNode<E> composite(E node,
                                                                     MeasureResult measureResult,
                                                                     CompositeLayoutSpec compositeLayout) {
        return new PreparedNode<>(node, measureResult, EmptyPreparedLayout.INSTANCE, Optional.of(compositeLayout));
    }

    public static <E extends DocumentNode> PreparedNode<E> composite(E node,
                                                                     MeasureResult measureResult,
                                                                     PreparedNodeLayout preparedLayout,
                                                                     CompositeLayoutSpec compositeLayout) {
        return new PreparedNode<>(node, measureResult, preparedLayout, Optional.of(compositeLayout));
    }

    public boolean isComposite() {
        return compositeLayout.isPresent();
    }

    public CompositeLayoutSpec requireCompositeLayout() {
        return compositeLayout.orElseThrow(() ->
                new IllegalStateException("Prepared node '" + node.nodeKind() + "' does not have composite layout metadata."));
    }

    public <T extends PreparedNodeLayout> T requirePreparedLayout(Class<T> layoutType) {
        Objects.requireNonNull(layoutType, "layoutType");
        if (!layoutType.isInstance(preparedLayout)) {
            throw new IllegalStateException("Prepared node '" + node.nodeKind()
                    + "' does not carry layout payload " + layoutType.getSimpleName() + ".");
        }
        return layoutType.cast(preparedLayout);
    }
}



