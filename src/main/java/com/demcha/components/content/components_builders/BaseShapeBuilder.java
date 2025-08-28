package com.demcha.components.content.components_builders;

import com.demcha.components.content.Stroke;
import com.demcha.components.content.rectangle.FillColor;
import com.demcha.components.style.ComponentColor;

import java.awt.Color;



public abstract class BaseShapeBuilder<T extends BaseShapeBuilder<T>>
        extends ComponentBoxBuilder<T> {

    public T stroke(Stroke stroke) {
        return addComponent(stroke);

    }

    public T strokeColor(ComponentColor color) {
        return strokeColor(color.color());
    }

    public T strokeColor(Color color) {
        Stroke current = entity.getComponent(Stroke.class).orElse(null);
        Stroke updated = (current == null)
                ? new Stroke(color, 2.0)      // разумный дефолт ширины
                : new Stroke(color, current.width());
        return addComponent(updated);
    }

    public T fillColor(FillColor fillColor) {
        return addComponent(fillColor);
    }

    public T fillColor(ComponentColor fillColor) {
        return fillColor(fillColor.color());
    }

    public T fillColor(Color fillColor) {
        return fillColor(new FillColor(fillColor));
    }
}
