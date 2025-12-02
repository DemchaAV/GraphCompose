package com.demcha.loyaut_core.components.containers.abstract_builders;

import com.demcha.loyaut_core.components.core.Entity;
import com.demcha.loyaut_core.core.EntityManager;

/**
 * The {@code BuildEntity} interface defines a contract for classes that are responsible for building an {@link Entity} object.
 * It provides methods to build the entity, access the associated {@link EntityManager}, and retrieve the entity being built.
 * <p>
 * Implementations of this interface are typically used in builder patterns to construct complex {@link Entity} objects step-by-step.
 */
public interface BuildEntity {

    /**
     * Builds the {@link Entity} object. This method is responsible for assembling all
     * components and child entities, if any, into a complete {@link Entity} object.
     *
     * @return The fully constructed and assembled {@link Entity} object.
     */
    default Entity build(){
        manager().putEntity(entity());
        return entity();
    }


    /**
     * Returns the {@link EntityManager} associated with this builder.
     *
     * @return The associated {@link EntityManager}.
     */
    EntityManager manager();

    /**
     * Returns the {@link Entity} object that is currently being built by this builder.
     *
     * @return The entity being built.
     */
    Entity entity();
}
