package com.demcha.compose.testsupport.engine.assembly;

import com.demcha.compose.testsupport.engine.assembly.container.EmptyBox;
import com.demcha.compose.engine.components.content.shape.FillColor;
import com.demcha.compose.engine.components.content.shape.Stroke;
import com.demcha.compose.engine.components.renderable.Circle;
import com.demcha.compose.engine.components.style.ComponentColor;
import com.demcha.compose.engine.core.EntityManager;

import java.awt.Color;

public class CircleBuilder extends EmptyBox<CircleBuilder> {
    CircleBuilder(EntityManager entityManager) {
        super(entityManager);
    }

    public CircleBuilder circle(Circle circle) {
        return addComponent(circle);
    }

    public CircleBuilder fillColor(FillColor fillColor) {
        return addComponent(fillColor);
    }

    public CircleBuilder fillColor(Color fillColor) {
        return fillColor(new FillColor(fillColor));
    }

    public CircleBuilder fillColor(ComponentColor fillColor) {
        return fillColor(new FillColor(fillColor));
    }

    public CircleBuilder fillColor(int r, int g, int b) {
        return fillColor(new FillColor(new Color(r, g, b)));
    }

    public CircleBuilder stroke(Stroke stroke) {
        return addComponent(stroke);
    }

    @Override
    public void initialize() {
        entity.addComponent(new Circle());
    }
}
