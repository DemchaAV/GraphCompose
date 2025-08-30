package com.demcha.components.components_builders;

import com.demcha.components.containers.abstract_builders.ShapeBuilderBase;
import com.demcha.components.core.Entity;
import com.demcha.components.renderable.Element;
import com.demcha.components.renderable.Rectangle;
import com.demcha.core.EntityManager;

public class BodyButtonBuilder extends ShapeBuilderBase<BodyButtonBuilder> {
    public BodyButtonBuilder(EntityManager document) {
        super(document);
    }


    public BodyButtonBuilder rectangle(Rectangle rectangle) {
        return addComponent(rectangle);
    }

    @Override
    public Entity build() {
        return entity;
    }

    @Override
    public void initialize() {
        entity.addComponent(new Element());
    }
}
