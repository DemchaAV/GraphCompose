package com.demcha.components.content.components_builders;

import com.demcha.components.containers.abstract_builders.ShapeBuilderBase;
import com.demcha.components.content.rectangle.Rectangle;
import com.demcha.components.core.Entity;
import com.demcha.core.PdfDocument;

public class RectangleBuilder extends ShapeBuilderBase<RectangleBuilder> {
    public RectangleBuilder(PdfDocument document) {
        super(document);
    }


    @Override
    public Entity build() {
        return entity;
    }


    public RectangleBuilder rectangle(Rectangle rectangle) {
        return addComponent(rectangle);
    }
}
