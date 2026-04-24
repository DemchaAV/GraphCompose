package com.demcha.compose.engine.components.containers.abstract_builders;

import com.demcha.compose.engine.components.core.Component;
import com.demcha.compose.engine.components.core.EntityName;

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
     * This method provides an initialization step for components within the entity,
     * ensuring that all components are properly associated with this entity.
     */

    void initialize();


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
