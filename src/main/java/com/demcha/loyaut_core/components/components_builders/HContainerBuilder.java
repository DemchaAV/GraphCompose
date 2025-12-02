package com.demcha.loyaut_core.components.components_builders;

import com.demcha.loyaut_core.components.containers.abstract_builders.ContainerBuilder;
import com.demcha.loyaut_core.components.containers.abstract_builders.StackAxis;
import com.demcha.loyaut_core.components.layout.Align;
import com.demcha.loyaut_core.components.renderable.HContainer;
import com.demcha.loyaut_core.core.EntityManager;
import lombok.extern.slf4j.Slf4j;

/**
 * Builder class for creating horizontal containers ({@link HContainer}).
 * This builder extends {@link ContainerBuilder} and specializes in arranging
 * child entities horizontally, managing their positions and calculating the
 * container's overall dimensions based on its children.
 * <p>
 * It uses Lombok's {@code @Slf4j} for logging.
 * </p>
 */
@Slf4j
public class HContainerBuilder extends ContainerBuilder<HContainerBuilder> {

    /**
     * Constructs a new {@code HContainerBuilder} associated with a specific Entity Manager.
     *
     * @param entityManager The {@link EntityManager} to which the container and its entities will belong.
     */
    public HContainerBuilder(EntityManager entityManager, Align align) {
        super(entityManager, align);
    }


    /**
     * Initializes the builder for creating a new horizontal container.
     * This method calls the common creation logic from the superclass and then
     * adds the specific {@link HContainer} component to the entity being built.
     *
     * @param align The alignment strategy to be used for arranging children within the container.
     * @return This builder instance, allowing for method chaining.
     */

    @Override
    public void initialize() {
        entity.addComponent(new HContainer());
        entity.addComponent(StackAxis.HORIZONTAL);
    }
}