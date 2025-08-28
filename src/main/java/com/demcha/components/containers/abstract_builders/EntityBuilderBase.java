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
     * The underlying {@link Entity} that this builder is constructing.
     * This entity holds all the components added to the layout.
     * <p>
     * It is initialized with a new {@link Entity} instance upon creation of the builder.
     * </p>
     */
    @Getter
    protected Entity entity = new Entity();

    /**
     * Creates the layout by assigning a default name to the underlying entity
     * and returning the builder instance.
     */

    public B create() {
        autoName();
        return self();
    }

    protected void autoName() {
        String simpleName = self().getClass().getSimpleName();
        String defaultName = simpleName + entity.getId().toString().substring(0, 5);
        entity.addComponent(new EntityName(defaultName));
    }

    /**
     * Adds a {@link Component} to the underlying {@link Entity}.
     *
     * @param component The component to add.
     */
    @Override
    public B addComponent(Component component) {
        entity.addComponent(component);
        return self();
    }


    /**
     * Returns the current builder instance, cast to its generic type {@code B}.
     * This method is used to enable method chaining in subclasses.
     */
    public B self() {
        return (B) this;
    }


}
