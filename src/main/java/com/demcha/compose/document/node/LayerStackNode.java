package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentInsets;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Composite node that stacks its children inside the same bounding box.
 *
 * <p>Children are painted in source order — the first layer is drawn behind, the
 * last layer is drawn in front. Each layer is positioned inside the stack box
 * using its {@link LayerAlign} alignment. The stack's intrinsic size is the
 * maximum of the layers' outer sizes (clamped to the available page width).</p>
 *
 * <p>Pagination is atomic: the entire stack moves to the next page when its
 * measured height does not fit on the current page.</p>
 *
 * @param name node name used in snapshots and layout graph paths
 * @param layers child layers in back-to-front order
 * @param padding inner padding applied around all layers
 * @param margin outer margin around the stack
 * @author Artem Demchyshyn
 */
public record LayerStackNode(
        String name,
        List<Layer> layers,
        DocumentInsets padding,
        DocumentInsets margin
) implements DocumentNode {

    /**
     * Normalizes layers and insets, validating that at least one layer exists.
     */
    public LayerStackNode {
        name = name == null ? "" : name;
        Objects.requireNonNull(layers, "layers");
        if (layers.isEmpty()) {
            throw new IllegalArgumentException("LayerStackNode '" + name + "' must have at least one layer.");
        }
        List<Layer> normalized = new ArrayList<>(layers.size());
        for (Layer layer : layers) {
            normalized.add(Objects.requireNonNull(layer, "layer"));
        }
        layers = List.copyOf(normalized);
        padding = padding == null ? DocumentInsets.zero() : padding;
        margin = margin == null ? DocumentInsets.zero() : margin;
    }

    @Override
    public List<DocumentNode> children() {
        List<DocumentNode> nodes = new ArrayList<>(layers.size());
        for (Layer layer : layers) {
            nodes.add(layer.node());
        }
        return List.copyOf(nodes);
    }

    /**
     * One layer inside a {@link LayerStackNode}.
     *
     * <p>The layer is positioned by anchoring its bounding box to the
     * {@code align} corner/edge of the stack box, then nudging it by
     * {@code offsetX} / {@code offsetY}. Offsets follow on-screen conventions:
     * positive {@code offsetX} moves the layer to the right, positive
     * {@code offsetY} moves it down. Use the two-arg constructor or
     * {@link #of(DocumentNode, LayerAlign)} when no offset is needed.</p>
     *
     * @param node child node painted in this layer
     * @param align alignment of the layer inside the stack box
     * @param offsetX horizontal offset from the anchor (positive = right)
     * @param offsetY vertical offset from the anchor (positive = down)
     */
    public record Layer(DocumentNode node, LayerAlign align, double offsetX, double offsetY) {
        /**
         * Validates required references and applies a {@link LayerAlign#TOP_LEFT}
         * default when alignment is omitted.
         */
        public Layer {
            Objects.requireNonNull(node, "node");
            align = align == null ? LayerAlign.TOP_LEFT : align;
        }

        /**
         * Creates a layer anchored to the top-left corner of the stack box.
         *
         * @param node child node
         */
        public Layer(DocumentNode node) {
            this(node, LayerAlign.TOP_LEFT, 0.0, 0.0);
        }

        /**
         * Creates a layer with explicit alignment and zero offset.
         *
         * @param node child node
         * @param align alignment of the layer
         */
        public Layer(DocumentNode node, LayerAlign align) {
            this(node, align, 0.0, 0.0);
        }

        /**
         * Convenience factory for a top-left layer (typically a background).
         *
         * @param node child node
         * @return back layer
         */
        public static Layer back(DocumentNode node) {
            return new Layer(node, LayerAlign.TOP_LEFT, 0.0, 0.0);
        }

        /**
         * Convenience factory for a centered layer (typically the content).
         *
         * @param node child node
         * @return centered layer
         */
        public static Layer center(DocumentNode node) {
            return new Layer(node, LayerAlign.CENTER, 0.0, 0.0);
        }

        /**
         * Convenience factory for an explicitly aligned layer with no offset.
         *
         * @param node child node
         * @param align alignment of the layer
         * @return aligned layer
         */
        public static Layer of(DocumentNode node, LayerAlign align) {
            return new Layer(node, align, 0.0, 0.0);
        }

        /**
         * Convenience factory for a layer positioned by anchor + screen-space offset.
         *
         * @param node child node
         * @param align anchor inside the stack box
         * @param offsetX horizontal offset from the anchor (positive = right)
         * @param offsetY vertical offset from the anchor (positive = down)
         * @return positioned layer
         */
        public static Layer of(DocumentNode node, LayerAlign align, double offsetX, double offsetY) {
            return new Layer(node, align, offsetX, offsetY);
        }
    }
}
