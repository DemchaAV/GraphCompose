package com.demcha.components.components_builders;

import com.demcha.components.containers.abstract_builders.ShapeBuilderBase;
import com.demcha.components.renderable.Rectangle;
import com.demcha.core.EntityManager;

public class RectangleBuilder extends ShapeBuilderBase<RectangleBuilder> {
    public RectangleBuilder(EntityManager entityManager) {
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
