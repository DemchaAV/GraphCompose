package com.demcha.components.content.components_builders;

import com.demcha.components.geometry.RectangleComponent;

public class RectangleBuilder extends ComponentBoxBuilder<RectangleBuilder> {
    public static RectangleBuilder create() {
        return new RectangleBuilder();
    }

    @Override
    protected RectangleBuilder self() {
        return this;
    }

    public RectangleBuilder rectangle(RectangleComponent rectangle) {
        return put(rectangle);
    }
}
