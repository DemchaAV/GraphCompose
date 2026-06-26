package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.style.DocumentInsets;

/**
 * Builder for invisible fixed-size spacer nodes.
 *
 * @author Artem Demchyshyn
 * @since 1.0.0
 */
public final class SpacerBuilder {
    private String name = "";
    private double width;
    private double height;
    private DocumentInsets padding = DocumentInsets.zero();
    private DocumentInsets margin = DocumentInsets.zero();
    private double grow;

    /**
     * Creates a spacer builder.
     */
    public SpacerBuilder() {
    }

    /**
     * Sets the spacer node name.
     *
     * @param name name used in snapshots and layout graph paths
     * @return this builder
     */
    public SpacerBuilder name(String name) {
        this.name = name == null ? "" : name;
        return this;
    }

    /**
     * Sets spacer width.
     *
     * @param width width in points
     * @return this builder
     */
    public SpacerBuilder width(double width) {
        this.width = width;
        return this;
    }

    /**
     * Sets spacer height.
     *
     * @param height height in points
     * @return this builder
     */
    public SpacerBuilder height(double height) {
        this.height = height;
        return this;
    }

    /**
     * Sets spacer width and height.
     *
     * @param width  width in points
     * @param height height in points
     * @return this builder
     */
    public SpacerBuilder size(double width, double height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /**
     * Sets the flex grow factor. Inside a row, a spacer with {@code grow > 0}
     * becomes a spring that absorbs a share of the row's leftover width
     * proportional to its grow factor; {@code 0} (the default) is rigid.
     *
     * @param grow grow factor; must be finite and {@code >= 0}
     * @return this builder
     * @since 1.9.0
     */
    public SpacerBuilder grow(double grow) {
        this.grow = grow;
        return this;
    }

    /**
     * Sets spacer padding.
     *
     * @param padding padding in points
     * @return this builder
     */
    public SpacerBuilder padding(DocumentInsets padding) {
        this.padding = padding == null ? DocumentInsets.zero() : padding;
        return this;
    }

    /**
     * Sets spacer margin.
     *
     * @param margin margin in points
     * @return this builder
     */
    public SpacerBuilder margin(DocumentInsets margin) {
        this.margin = margin == null ? DocumentInsets.zero() : margin;
        return this;
    }

    /**
     * Builds the spacer node.
     *
     * @return spacer node
     */
    public SpacerNode build() {
        return new SpacerNode(name, width, height, padding, margin, grow);
    }
}
