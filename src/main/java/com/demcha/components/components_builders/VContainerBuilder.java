package com.demcha.components.components_builders;
import com.demcha.components.containers.abstract_builders.ContainerBuilder;
import com.demcha.components.containers.abstract_builders.StackAxis;
import com.demcha.components.renderable.VContainer;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.layout.Align;
import com.demcha.components.layout.Anchor;
import com.demcha.components.layout.VAnchor;
import com.demcha.components.layout.coordinator.Position;
import com.demcha.components.style.Padding;
import com.demcha.core.EntityManager;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;

@Slf4j
public class VContainerBuilder extends ContainerBuilder<VContainerBuilder> {
    /**
     * Constructs a new {@code VContainerBuilder} with the specified Entity Manager.
     *
     * @param entityManager The {@link EntityManager} to which the container will be added.
     */
    public VContainerBuilder(EntityManager entityManager, Align align) {
        super(entityManager,align);
        entity.addComponent(align);
    }

    /**
     * Initializes the container builder with the specified alignment.
     * This method calls the common creation logic from the superclass and then
     * adds a {@link VContainer} component to the entity.
     *
     * @param align The {@link Align} strategy for arranging children within the container.
     * @return This builder instance for method chaining.
     */




    @Override
    public void initialize() {
        entity.addComponent(new VContainer());
        entity.addComponent(StackAxis.VERTICAL);
    }


}
