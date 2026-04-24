package com.demcha.compose.testsupport.engine.assembly;

import com.demcha.compose.engine.components.content.link.DisplayText;
import com.demcha.compose.engine.core.EntityManager;

public class DisplayUrlTextBuilder extends TextBuilder{

    DisplayUrlTextBuilder(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public void initialize() {
        entity.addComponent(new DisplayText());
    }
}
