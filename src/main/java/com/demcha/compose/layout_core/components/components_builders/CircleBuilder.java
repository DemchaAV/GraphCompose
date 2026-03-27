package com.demcha.compose.layout_core.components.components_builders;

import com.demcha.compose.layout_core.components.containers.abstract_builders.ShapeBuilderBase;
import com.demcha.compose.layout_core.components.renderable.Circle;
import com.demcha.compose.layout_core.core.EntityManager;

public class CircleBuilder extends ShapeBuilderBase<CircleBuilder> {
    public CircleBuilder(EntityManager entityManager) {
        super(entityManager);
    }

    public CircleBuilder circle(Circle circle) {
        return addComponent(circle);
    }

    @Override
    public void initialize() {
        entity.addComponent(new Circle());
    }
}
