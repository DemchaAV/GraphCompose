package com.demcha.components.components_builders;

import com.demcha.components.containers.abstract_builders.EmptyBox;
import com.demcha.components.renderable.Link;
import com.demcha.core.EntityManager;

public class LinkBuilder extends EmptyBox<LinkBuilder> {
    public LinkBuilder(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public void initialize() {
            entity().addComponent(new Link());
    }
}
