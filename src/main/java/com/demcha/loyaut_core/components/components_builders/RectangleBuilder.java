package com.demcha.loyaut_core.components.components_builders;

import com.demcha.loyaut_core.components.containers.abstract_builders.ShapeBuilderBase;
import com.demcha.loyaut_core.components.renderable.Rectangle;
import com.demcha.loyaut_core.core.EntityManager;

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
