package com.demcha.components.content.components_builders;

import com.demcha.components.content.Stroke;
import com.demcha.components.content.rectangle.Rectangle;
import com.demcha.components.style.ColorComponent;

import java.awt.*;

public class RectangleBuilder extends BaseShapeBuilder<RectangleBuilder> {
    public static RectangleBuilder create() { return new RectangleBuilder(); }
    @Override protected RectangleBuilder self() { return this; }

    public RectangleBuilder rectangle(Rectangle rectangle) {
        return addComponent(rectangle);
    }
}
