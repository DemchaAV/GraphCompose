package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;
import com.demcha.compose.font.FontLibrary;

import java.util.List;

/**
 * Shared fragment emission context passed to node definitions.
 */
public interface FragmentContext {
    /**
     * Returns the backend font library available during fragment creation.
     *
     * @return font library
     */
    FontLibrary fonts();

    /**
     * Returns the text measurement service for this layout pass.
     *
     * @return text measurement service
     */
    TextMeasurementSystem textMeasurement();

    /**
     * Returns page and margin information for the current document.
     *
     * @return layout canvas
     */
    LayoutCanvas canvas();

    /**
     * Indicates whether paragraph text should be parsed as markdown.
     *
     * @return true when markdown parsing is enabled
     */
    default boolean markdownEnabled() {
        return false;
    }

    /**
     * Dispatches fragment emission to a previously-prepared child node
     * via the active {@code NodeRegistry}.
     *
     * <p>Used by composite primitives (e.g. {@code TableNode} cells with
     * {@code content}) that hold a prepared child sub-tree and need to
     * emit its fragments as part of their own emit pass without holding
     * a registry reference themselves. The default implementation
     * throws {@link UnsupportedOperationException} so non-default
     * {@code FragmentContext} implementations have to opt-in to the
     * recursion.</p>
     *
     * @param child     prepared child node previously obtained from
     *                  {@link PrepareContext#prepare(DocumentNode, BoxConstraints)}
     * @param placement placement assigned to the child within the
     *                  composite parent's geometry
     * @param <E>       child node type
     * @return fragments emitted by the child's {@code NodeDefinition}
     * @throws UnsupportedOperationException when the
     *                                       {@code FragmentContext} implementation does not back
     *                                       child-fragment emission
     */
    default <E extends DocumentNode> List<LayoutFragment> emitChildFragments(
            PreparedNode<E> child,
            FragmentPlacement placement) {
        throw new UnsupportedOperationException(
                "FragmentContext implementation does not support child fragment emission");
    }
}


