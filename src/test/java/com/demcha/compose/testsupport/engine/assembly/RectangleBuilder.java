package com.demcha.compose.testsupport.engine.assembly;

import com.demcha.compose.testsupport.engine.assembly.container.ShapeBuilderBase;
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
