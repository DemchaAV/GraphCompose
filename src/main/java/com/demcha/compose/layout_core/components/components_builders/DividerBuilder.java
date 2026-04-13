package com.demcha.compose.layout_core.components.components_builders;

import com.demcha.compose.layout_core.components.containers.abstract_builders.EmptyBox;
import com.demcha.compose.layout_core.components.content.shape.LinePath;
import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.content.shape.StrokeColor;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.renderable.Line;
import com.demcha.compose.layout_core.components.style.ComponentColor;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.EntityManager;
import lombok.extern.slf4j.Slf4j;

import java.awt.Color;

/**
 * Convenience builder for a horizontal divider line.
 *
 * <p>A divider is a thin horizontal {@link Line} entity with sensible
 * defaults for color, thickness, and vertical spacing. It is a convenience
 * wrapper rather than a new entity type.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 * cb.divider()
 *     .width(520)
 *     .thickness(1)
 *     .color(ComponentColor.LIGHT_GRAY)
 *     .verticalSpacing(12)
 *     .build();
 * </pre>
 *
 * @author Artem Demchyshyn
 */
@Slf4j
public class DividerBuilder extends EmptyBox<DividerBuilder> {

    private double width = 0;
    private double thickness = 1;
    private Color color = ComponentColor.LIGHT_GRAY;
    private double verticalSpacing = 8;

    DividerBuilder(EntityManager entityManager) {
        super(entityManager);
    }

    /**
     * Sets the width of the divider line in points.
     *
     * @param width line width
     * @return this builder
     */
    public DividerBuilder width(double width) {
        this.width = width;
        return this;
    }

    /**
     * Sets the line thickness (stroke width) in points.
     *
     * @param thickness stroke width
     * @return this builder
     */
    public DividerBuilder thickness(double thickness) {
        this.thickness = thickness;
        return this;
    }

    /**
     * Sets the divider color.
     *
     * @param color the line color
     * @return this builder
     */
    public DividerBuilder color(Color color) {
        this.color = color;
        return this;
    }

    /**
     * Sets the divider color from a {@link ComponentColor}.
     */
    public DividerBuilder color(ComponentColor color) {
        return color(color.color());
    }

    /**
     * Sets vertical spacing (padding above and below the divider line).
     *
     * @param spacing spacing in points
     * @return this builder
     */
    public DividerBuilder verticalSpacing(double spacing) {
        this.verticalSpacing = spacing;
        return this;
    }

    @Override
    public void initialize() {
        entity.addComponent(new Line());
        entity.addComponent(LinePath.horizontal());
    }

    @Override
    public Entity build() {
        double totalHeight = thickness + verticalSpacing * 2;
        entity.addComponent(new ContentSize(width, totalHeight));
        entity.addComponent(new Stroke(new StrokeColor(color), thickness));
        entity.addComponent(new Padding(verticalSpacing, 0, verticalSpacing, 0));
        return registerBuiltEntity();
    }
}
