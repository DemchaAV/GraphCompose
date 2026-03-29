package com.demcha.compose.layout_core.components.components_builders;

import com.demcha.compose.layout_core.components.containers.abstract_builders.ContainerBuilder;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.renderable.HContainer;
import com.demcha.compose.layout_core.core.EntityManager;

// horizontall aligne
public class RowBuilder extends ContainerBuilder<RowBuilder> {

    /**
     * Constructs a new {@code HContainerBuilder} associated with a specific Entity Manager.
     *
     * @param entityManager The {@link EntityManager} to which the container and its entities will belong.
     */
    RowBuilder(EntityManager entityManager, Align align) {
        super(entityManager,align);
    }


    /**
     * Initializes the builder for creating a new horizontal container.
     * This method calls the common creation logic from the superclass and then
     * adds the specific {@link HContainer} component to the entity being built.
     *
     * @return This builder instance, allowing for method chaining.
     */


    @Override
    public void initialize() {
        entity.addComponent(new HContainer());
    }


}


