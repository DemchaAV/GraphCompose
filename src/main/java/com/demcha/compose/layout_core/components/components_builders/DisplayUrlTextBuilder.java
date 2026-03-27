package com.demcha.compose.layout_core.components.components_builders;

import com.demcha.compose.layout_core.components.content.link.DisplayText;
import com.demcha.compose.layout_core.core.EntityManager;

public class DisplayUrlTextBuilder extends TextBuilder{

    public DisplayUrlTextBuilder(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public void initialize() {
        entity.addComponent(new DisplayText());
    }
}
