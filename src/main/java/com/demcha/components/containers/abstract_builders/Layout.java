package com.demcha.components.containers.abstract_builders;

import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.Anchor;
import com.demcha.components.layout.HAnchor;
import com.demcha.components.layout.Layer;
import com.demcha.components.layout.VAnchor;
import com.demcha.components.layout.coordinator.Position;
import com.demcha.components.style.Margin;
import com.demcha.components.style.Padding;

public interface Layout<B> extends EntityCreator<B> {

    default B layer(Layer layer) {
        addComponent(layer);
        return self();
    }

    default B layer(int layer) {
        layer(new Layer(layer));
        return self();
    }

    default B position(Position position) {
        addComponent(position);
        return self();
    }

    default B position(double x, double y) {
        position(new Position(x, y));
        return self();
    }

    default B anchor(Anchor anchor) {
        addComponent(anchor);
        return self();
    }

    default B anchor(HAnchor hAnchor, VAnchor vAnchor) {
        anchor(new Anchor(hAnchor, vAnchor));
        return self();
    }

    default B size(ContentSize size) {
        addComponent(size);
        return self();
    }


    default B size(double width, double height) {
        size(new ContentSize(width, height));
        return self();
    }

    default B margin(Margin margin) {
        addComponent(margin);
        return self();
    }

    default B margin(double top, double right, double bottom, double left) {
        margin(new Margin(top, right, bottom, left));
        return self();
    }

    default B padding(Padding padding) {
        addComponent(padding);
        return self();
    }

    default B padding(double top, double right, double bottom, double left) {
        padding(new Padding(top, right, bottom, left));
        return self();
    }

    B self();

}
