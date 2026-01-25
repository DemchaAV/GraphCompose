package com.demcha.compose.loyaut_core.components.containers.abstract_builders;

import com.demcha.compose.loyaut_core.components.content.shape.Stroke;
import com.demcha.compose.loyaut_core.components.content.shape.CornerRadius;
import com.demcha.compose.loyaut_core.components.content.shape.FillColor;
import com.demcha.compose.loyaut_core.components.renderable.Rectangle;
import com.demcha.compose.loyaut_core.components.style.ComponentColor;
import com.demcha.compose.loyaut_core.core.EntityManager;

import java.awt.*;


/**
 * An abstract builder class for creating shapes within a Entity Manager.
 * This class provides methods to add various shape-related components like rectangles, corner radii, fill colors, and strokes.
 * @param <T> The type of the concrete builder extending this abstract class, allowing for method chaining.
 */
public abstract class ShapeBuilderBase<T> extends EmptyBox<T> {

    public ShapeBuilderBase(EntityManager entityManager) {
        super(entityManager);
    }

    /**
     * Adds a rectangle component to the shape.
     *
     * @param rectangle The Rectangle object to add.
     * @return The current builder instance for method chaining.
     */
    public T rectangle(Rectangle rectangle) {
        return addComponent(rectangle);
    }

    /**
     * Adds a default rectangle component to the shape.
     *
     * @return The current builder instance for method chaining.
     */
    public T rectangle() {
        return rectangle(new Rectangle());
    }

    /**
     * Sets the corner radius for the shape.
     *
     * @param radius The CornerRadius object specifying the corner radius.
     * @return The current builder instance for method chaining.
     */
    public T cornerRadius(CornerRadius radius) {
        return addComponent(radius);
    }

    /**
     * Sets the corner radius for the shape using a double value.
     *
     * @param radius The double value representing the corner radius.
     * @return The current builder instance for method chaining.
     */
    public T cornerRadius(double radius) {
        return cornerRadius(new CornerRadius(radius));
    }

    /**
     * Sets the fill color for the shape.
     *
     * @param fillColor The FillColor object to set.
     * @return The current builder instance for method chaining.
     */
    public T fillColor(FillColor fillColor) {
        return addComponent(fillColor);
    }

    /**
     * Sets the fill color for the shape using a AWT Color object.
     *
     * @param fillColor The AWT Color object to set.
     * @return The current builder instance for method chaining.
     */
    public T fillColor(Color fillColor) {
        return fillColor(new FillColor(fillColor));
    }

    /**
     * Sets the fill color for the shape using a ComponentColor object.
     *
     * @param fillColor The ComponentColor object to set.
     * @return The current builder instance for method chaining.
     */
    public T fillColor(ComponentColor fillColor) {
        return fillColor(new FillColor(fillColor));
    }

    /**
     * Sets the fill color for the shape using individual RGB integer values.
     *
     * @param r The red component (0-255).
     * @param g The green component (0-255).
     * @param b The blue component (0-255).
     * @return The current builder instance for method chaining.
     */
    public T fillColor(int r, int g, int b) {
        return fillColor(new FillColor(new Color(r, g, b)));
    }

    /**
     * Sets the stroke (outline) for the shape.
     *
     * @param stroke The Stroke object to set.
     * @return The current builder instance for method chaining.
     */
    public T stroke(Stroke stroke) {
        return addComponent(stroke);
    }
}
