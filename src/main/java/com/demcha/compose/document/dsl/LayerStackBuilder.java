package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.style.DocumentInsets;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builder for {@link LayerStackNode} — composes background panels, content
 * layers, watermarks, and overlay decorations inside a single bounding box.
 *
 * <p>Layers are painted in source order: the first layer is drawn behind, the
 * last layer is drawn in front. Each layer is positioned inside the stack using
 * its {@link LayerAlign} alignment and the stack itself is treated as an atomic
 * block by the canonical paginator.</p>
 *
 * @author Artem Demchyshyn
 */
public final class LayerStackBuilder {
    private String name = "";
    private final List<LayerStackNode.Layer> layers = new ArrayList<>();
    private DocumentInsets padding = DocumentInsets.zero();
    private DocumentInsets margin = DocumentInsets.zero();

    /**
     * Creates a layer stack builder.
     */
    public LayerStackBuilder() {
    }

    /**
     * Sets the semantic stack name.
     *
     * @param name name used in snapshots and layout graph paths
     * @return this builder
     */
    public LayerStackBuilder name(String name) {
        this.name = name == null ? "" : name;
        return this;
    }

    /**
     * Appends a layer anchored to the top-left corner of the stack box.
     *
     * @param node child node
     * @return this builder
     */
    public LayerStackBuilder layer(DocumentNode node) {
        return layer(node, LayerAlign.TOP_LEFT);
    }

    /**
     * Appends a layer with explicit alignment inside the stack box.
     *
     * @param node child node
     * @param align layer alignment
     * @return this builder
     */
    public LayerStackBuilder layer(DocumentNode node, LayerAlign align) {
        layers.add(new LayerStackNode.Layer(Objects.requireNonNull(node, "node"), align));
        return this;
    }

    /**
     * Appends a back layer (top-left aligned, drawn first/behind).
     *
     * @param node child node
     * @return this builder
     */
    public LayerStackBuilder back(DocumentNode node) {
        return layer(node, LayerAlign.TOP_LEFT);
    }

    /**
     * Appends a centered layer (typically the content above a background).
     *
     * @param node child node
     * @return this builder
     */
    public LayerStackBuilder center(DocumentNode node) {
        return layer(node, LayerAlign.CENTER);
    }

    /**
     * Sets the inner padding around all layers.
     *
     * @param padding padding insets
     * @return this builder
     */
    public LayerStackBuilder padding(DocumentInsets padding) {
        this.padding = padding == null ? DocumentInsets.zero() : padding;
        return this;
    }

    /**
     * Sets equal inner padding on all sides.
     *
     * @param padding padding in points
     * @return this builder
     */
    public LayerStackBuilder padding(double padding) {
        return padding(DocumentInsets.of(padding));
    }

    /**
     * Sets the outer margin around the stack.
     *
     * @param margin margin insets
     * @return this builder
     */
    public LayerStackBuilder margin(DocumentInsets margin) {
        this.margin = margin == null ? DocumentInsets.zero() : margin;
        return this;
    }

    /**
     * Sets equal outer margin on all sides.
     *
     * @param margin margin in points
     * @return this builder
     */
    public LayerStackBuilder margin(double margin) {
        return margin(DocumentInsets.of(margin));
    }

    /**
     * Builds the layer stack node.
     *
     * @return immutable layer stack node
     */
    public LayerStackNode build() {
        return new LayerStackNode(name, List.copyOf(layers), padding, margin);
    }
}
