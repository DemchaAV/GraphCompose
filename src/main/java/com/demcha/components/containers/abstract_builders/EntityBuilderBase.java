package com.demcha.components.containers.abstract_builders;

import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.core.EntityName;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public abstract class EntityBuilderBase<B> implements Layout<B>, EntityCreator<B> {
    /**
     * Abstract base class for entity builders.
     * <p>
     * This class provides common functionality for building {@link Entity} objects,
     * including managing components, auto-naming entities, and providing a fluent API
     * for method chaining.
     * </p>
     */
    @Getter
    protected Entity entity = new Entity();

    /**
     * Initializes the entity by assigning a default name and returns the builder instance.
     * This method should be called to finalize the initial setup of the entity.
     *
     * @return The current builder instance, allowing for method chaining.
     */
    public B create() {
        autoName();
        return self();
    }

    /**
     * Automatically generates a default name for the underlying {@link Entity}.
     * The name is composed of the simple class name of the builder and a truncated version of the entity's ID.
     */
    protected void autoName() {
        String simpleName = self().getClass().getSimpleName();
        String defaultName = simpleName + entity.getId().toString().substring(0, 5);
        entity.addComponent(new EntityName(defaultName));
    }

    /**
     * Adds a {@link Component} to the underlying {@link Entity}.
     * This method allows for the composition of the entity with various functional components.
     *
     * @param component The {@link Component} to be added to the entity.
     * @return The current builder instance, allowing for method chaining.
     */
    @Override
    public B addComponent(Component component) {
        entity.addComponent(component);
        return self();
    }


    /**
     * Returns the current builder instance, cast to its generic type {@code B}.
     * This method is crucial for enabling fluent API and method chaining in subclasses.
     * @return The current builder instance.
     */
    public B self() {
        return (B) this;
    }


}
