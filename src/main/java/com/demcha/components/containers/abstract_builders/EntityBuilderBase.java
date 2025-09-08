package com.demcha.components.containers.abstract_builders;

import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.core.EntityName;
import lombok.Getter;
import lombok.experimental.Accessors;
/**
 * Abstract base class for entity builders.
 * <p>
 * This class provides common functionality for building {@link Entity} objects,
 * including managing components, auto-naming entities, and providing a fluent API
 * for method chaining.
 * </p>
 */
@Getter
@Accessors(fluent = true)
public abstract class EntityBuilderBase<B> implements Layout<B>, EntityCreator<B> {

    /**
     * The {@link Entity} instance being built by this builder.
     */
    @Getter
    protected Entity entity;



    /**
     * Automatically generates a default name for the underlying {@link Entity}.
     * The name is composed of the simple class name of the builder and a truncated version of the entity's ID.
     *
     * <p>
     * The generated name follows the pattern: {@code BuilderClassName + first 5 characters of EntityID}.
     * This name is then added to the entity as an {@link EntityName} component.
     * </p>
     */
    protected void autoName() {
        String simpleName = self().getClass().getSimpleName();
        String defaultName = simpleName + entity.getUuid().toString().substring(0, 5);
        entity.addComponent(new EntityName(defaultName));
    }

    /**
     * Adds a {@link Component} to the underlying {@link Entity}.
     * This method allows for the composition of the entity with various functional components.
     *
     * <p>
     * This method delegates the actual addition of the component to the {@link Entity#addComponent(Component)} method.
     * </p>
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
     *
     * <p>
     * Subclasses should implement this method to return {@code this} cast to their specific builder type.
     * </p>
     * @return The current builder instance.
     */
    public B self() {
        return (B) this;
    }


}
