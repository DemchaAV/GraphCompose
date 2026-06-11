package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;

import java.awt.*;

/**
 * Builder for thin horizontal divider nodes.
 *
 * @since 1.0.0
 */
public final class DividerBuilder extends ShapeBuilder {
    DividerBuilder() {
        height = 1.0;
        fillColor = DocumentColor.LIGHT_GRAY;
    }

    /**
     * Sets divider width.
     *
     * @param width width in points
     * @return this builder
     */
    public DividerBuilder width(double width) {
        super.width(width);
        return this;
    }

    /**
     * Sets divider height.
     *
     * @param height height in points
     * @return this builder
     */
    public DividerBuilder height(double height) {
        super.height(height);
        return this;
    }

    /**
     * Sets divider thickness.
     *
     * @param height thickness in points
     * @return this builder
     */
    public DividerBuilder thickness(double height) {
        return height(height);
    }

    /**
     * Sets divider color.
     *
     * @param color divider color
     * @return this builder
     */
    public DividerBuilder color(Color color) {
        super.fillColor(color);
        return this;
    }

    /**
     * Sets divider color with a public canonical color.
     *
     * @param color divider color
     * @return this builder
     */
    public DividerBuilder color(DocumentColor color) {
        return color(color == null ? null : color.color());
    }

    /**
     * Sets divider node name.
     *
     * @param name name used in snapshots and layout graph paths
     * @return this builder
     */
    @Override
    public DividerBuilder name(String name) {
        super.name(name);
        return this;
    }

    /**
     * Sets divider padding with the public canonical spacing value.
     *
     * @param padding padding in points
     * @return this builder
     */
    @Override
    public DividerBuilder padding(DocumentInsets padding) {
        super.padding(padding);
        return this;
    }

    /**
     * Sets divider margin with the public canonical spacing value.
     *
     * @param margin margin in points
     * @return this builder
     */
    @Override
    public DividerBuilder margin(DocumentInsets margin) {
        super.margin(margin);
        return this;
    }

    /**
     * Builds the divider as a thin shape node.
     *
     * @return divider shape node
     */
    @Override
    public ShapeNode build() {
        return new ShapeNode(name, width, height, fillColor, stroke, null, null, padding, margin);
    }
}

/**
 * Builder for semantic tables with row-atomic pagination.
 */
