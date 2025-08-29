package com.demcha.components.content.components_builders;

import com.demcha.components.core.Entity;
import com.demcha.core.EntityManager;

public interface ComponentBuilder {

    String entityName();

    Entity buildComponents();
    Entity buildInto(EntityManager entityManager);
}
