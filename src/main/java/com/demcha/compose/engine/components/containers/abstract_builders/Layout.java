package com.demcha.compose.engine.components.containers.abstract_builders;

import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.components.layout.Anchor;
import com.demcha.compose.engine.components.layout.HAnchor;
import com.demcha.compose.engine.components.layout.Layer;
import com.demcha.compose.engine.components.layout.VAnchor;
import com.demcha.compose.engine.components.layout.coordinator.Position;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;

/**
 * This interface defines a set of default methods for configuring layout-related properties
 * of a component builder. It is intended to be implemented by builder classes.
 *
 * @param <B> The type of the builder implementing this interface.
 *
 *            <p>The interface provides methods for setting various layout properties such as:
 *            <ul>
 *                <li>{@code layer}: The z-order of the component.</li>
 *                <li>{@code position}: The absolute position (x, y) of the component.</li>
 *                <li>{@code anchor}: How the component is anchored within its parent.</li>
 *                <li>{@code size}: The width and height of the component.</li>
 *                <li>{@code margin}: The external spacing around the component.</li>
 *                <li>{@code padding}: The internal spacing within the component.</li>
 *            </ul>
 *            Each method returns the builder instance itself, allowing for method chaining.</p>
 */
public interface Layout<B> {
    default B layer(Layer layer) {
        entity().addComponent(layer);
        return self();
    }

    default B layer(int layer) {
        layer(new Layer(layer));
        return self();
    }

    default B position(Position position) {
        entity().addComponent(position);
        return self();
    }

    default B position(double x, double y) {
        position(new Position(x, y));
        return self();
    }

    default B anchor(Anchor anchor) {
        entity().addComponent(anchor);
        return self();
    }

    default B anchor(HAnchor hAnchor, VAnchor vAnchor) {
        anchor(new Anchor(hAnchor, vAnchor));
        return self();
    }

    default B size(ContentSize size) {
        entity().addComponent(size);
        return self();
    }


    default B size(double width, double height) {
        size(new ContentSize(width, height));
        return self();
    }

    default B margin(Margin margin) {
        entity().addComponent(margin);
        return self();
    }

    default B margin(double top, double right, double bottom, double left) {
        margin(new Margin(top, right, bottom, left));
        return self();
    }

    default B padding(Padding padding) {
        entity().addComponent(padding);
        return self();
    }

    default B padding(double top, double right, double bottom, double left) {
        padding(new Padding(top, right, bottom, left));
        return self();
    }

    Entity entity();

    /**
     * Returns the current builder instance, cast to its generic type {@code B}.
     * This method is used to enable method chaining in subclasses.
     */
    B self();

}
