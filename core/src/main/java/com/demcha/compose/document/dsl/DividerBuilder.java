package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTransform;

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
     * Builds the divider as a thin shape node carrying the divider signal.
     *
     * <p>The node is the box of a colour it has always been — nothing is drawn
     * differently for the signal. It says only that this box asked to be a rule,
     * which no geometry of the box tells: a divider's thickness is its height and
     * it carries a fill, exactly as a swatch of the same size does. A backend with
     * a rule of its own reads it: the DOCX export writes a divider in the flow, only a
     * fill and no taller than Word's thickest border, as Word's horizontal rule, and
     * leaves any other box of that size to its drawing path, which draws it where the
     * page puts it or drops it with a note.</p>
     *
     * <p>What this builder carries is the node's name, width, thickness, colour,
     * padding, margin, stroke, link, outline entry and anchor — a rule written as
     * Word's own holds its anchor's bookmark on its paragraph, so a divider the DSL
     * builds can be the target of a link or a page reference. The inherited setters for a corner
     * radius, a paint fill and a transform are dropped: each of them would stop the
     * box being a rule at all, and a divider that wants one of them is built from
     * the node's own constructor. A divider given a stroke wide enough to draw is
     * carried with it and is then drawn rather than written as a rule, since Word's
     * rule is a border of one colour with no stroke of its own to carry.</p>
     *
     * @return divider shape node
     */
    @Override
    public ShapeNode build() {
        return new ShapeNode(name, width, height, fillColor, stroke, DocumentCornerRadius.ZERO,
                linkTarget, bookmarkOptions, padding, margin, DocumentTransform.NONE, null,
                anchor, true);
    }
}

/**
 * Builder for semantic tables with row-atomic pagination.
 */
