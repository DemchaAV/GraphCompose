package com.demcha.components.containers.abstract_builders;

import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.core.EntityName;

/**
 * An interface for creating and configuring entities.
 *
 * @param <B> The type of the builder itself, allowing for method chaining.
 */
public interface EntityCreator<B> {


    /**
     * Configures the entity with the specified name.
     *
     * @param name The name of the entity.
     * @return The builder instance for method chaining.
     */
    default B entityName(EntityName name) {
        addComponent(name);
        return self();
    }

    /**
     * Configures the entity with the specified name.
     *
     * @param name The name of the entity.
     * @return The builder instance for method chaining.
     */

    default B entityName(String name) {
        entityName(new EntityName(name));
        return self();
    }


    /**
     * Builds the entity.
     *
     * @return The built entity.
     */
    Entity build();

    /**
     * add component in to our entity
     *
     * @param component The component to add.
     * @return The builder instance for method chaining.
     */
    B addComponent(Component component);

    /**
     * Returns the builder instance itself.
     *
     * @return
     */
    B self();
}
