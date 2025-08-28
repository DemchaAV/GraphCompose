package com.demcha.components.containers.abstract_builders;

import com.demcha.components.core.Component;
import com.demcha.components.core.EntityName;

public interface EntityCreator<B> {
    /**
     * create an Instance Entity
     *
     * @return this object
     */
    B create();

    /**
     * set entity name if need;
     */
    default B entityName(EntityName name) {
        addComponent(name);
        return self();
    }

    default B entityName(String name) {
        entityName(new EntityName(name));
        return self();
    }


    /**
     * add component in to our entity
     *
     * @param component
     * @return
     */
    B addComponent(Component component);

    /**
     * return an object itself
     *
     * @return
     */
    B self();
}
