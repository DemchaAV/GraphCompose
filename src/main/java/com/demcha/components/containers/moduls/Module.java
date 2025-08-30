package com.demcha.components.containers.moduls;

import com.demcha.components.containers.abstract_builders.AbstractContainerBuilder;
import com.demcha.components.renderable.Element;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.style.Padding;
import com.demcha.core.EntityManager;

public class Module extends AbstractContainerBuilder<Module> {
    public Module(EntityManager document) {
        super(document);
    }

    @Override
    protected void updateChildPosition(Entity child) {

    }

    @Override
    protected void updateContainerDimensions(Entity child) {

    }

    @Override
    protected ContentSize calculateContentSize(Padding padding) {
        return null;
    }


    @Override
    public void initialize() {
        entity.addComponent(new Element());
    }
}
