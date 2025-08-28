package com.demcha.components.containers.moduls;

import com.demcha.components.content.Stroke;
import com.demcha.components.content.rectangle.FillColor;
import com.demcha.components.content.rectangle.Radius;
import com.demcha.components.content.rectangle.Rectangle;
import com.demcha.components.style.ComponentColor;
import com.demcha.core.PdfDocument;

import java.awt.*;


public abstract class ShapeBuilder<T> extends EmptyBox<T> {

    public ShapeBuilder(PdfDocument document) {
        super(document);
    }

    public T rectangle(Rectangle rectangle) {
        return addComponent(rectangle);
    }

    public T rectangle() {
        return rectangle(new Rectangle());
    }

    public T cornerRadius(Radius radius) {
        return addComponent(radius);
    }

    public T cornerRadius(double radius) {
        return cornerRadius(new Radius(radius));
    }

    public T fillColor(FillColor fillColor) {
        return addComponent(fillColor);
    }

    public T fillColor(Color fillColor) {
        return fillColor(new FillColor(fillColor));
    }

    public T fillColor(ComponentColor fillColor) {
        return fillColor(new FillColor(fillColor));
    }

    public T fillColor(int r, int g, int b) {
        return fillColor(new FillColor(new Color(r, g, b)));
    }

    public T stroke(Stroke stroke) {
        return addComponent(stroke);
    }
}
