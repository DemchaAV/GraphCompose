package com.demcha.components.components_builders;

import com.demcha.components.containers.abstract_builders.ContainerBuilder;
import com.demcha.components.containers.abstract_builders.StackAxis;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.layout.Align;
import com.demcha.components.layout.Anchor;
import com.demcha.components.layout.HAnchor;
import com.demcha.components.layout.coordinator.Position;
import com.demcha.components.renderable.HContainer;
import com.demcha.components.core.Entity;
import com.demcha.components.style.Padding;
import com.demcha.core.EntityManager;

import java.util.Iterator;

// horizontall aligne
public class RowBuilder extends ContainerBuilder<RowBuilder> {

    /**
     * Constructs a new {@code HContainerBuilder} associated with a specific Entity Manager.
     *
     * @param entityManager The {@link EntityManager} to which the container and its entities will belong.
     */
    public RowBuilder(EntityManager entityManager, Align align) {
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


