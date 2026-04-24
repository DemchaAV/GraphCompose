package com.demcha.compose.engine.components.components_builders;

import com.demcha.compose.engine.components.containers.abstract_builders.ShapeBuilderBase;
import com.demcha.compose.engine.components.renderable.Rectangle;
import com.demcha.compose.engine.core.EntityManager;

public class RectangleBuilder extends ShapeBuilderBase<RectangleBuilder> {
    RectangleBuilder(EntityManager entityManager) {
        super(entityManager);
    }


    public RectangleBuilder rectangle(Rectangle rectangle) {
        return addComponent(rectangle);
    }


    @Override
    public void initialize() {
        entity.addComponent(new Rectangle());
    }
}
