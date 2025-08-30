package com.demcha.components.components_builders;

import com.demcha.components.content.link.DisplayText;
import com.demcha.core.EntityManager;

public class DisplayUrlTextBuilder extends TextBuilder{
    public DisplayUrlTextBuilder(EntityManager entityManager) {
        super(entityManager);
    }
    @Override
    public void initialize() {
        entity.addComponent(new DisplayText());
    }
}
