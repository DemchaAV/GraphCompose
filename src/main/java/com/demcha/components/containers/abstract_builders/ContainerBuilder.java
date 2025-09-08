package com.demcha.components.containers.abstract_builders;


import com.demcha.components.components_builders.HContainerBuilder;
import com.demcha.components.components_builders.VContainerBuilder;
import com.demcha.components.core.Entity;
import com.demcha.components.layout.Align;
import com.demcha.core.EntityManager;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * An abstract base class for building container components (e.g., HContainer, VContainer)
 * within a Entity Manager. This class provides common functionality for managing child entities,
 * handling alignment, and calculating container dimensions.
 *
 * <p>Subclasses are responsible for implementing specific layout logic, such as how child
 * positions are updated and how container dimensions are calculated based on the children
 * and the container's type (horizontal or vertical).</p>
 * <p>This class provides a fluent API for building container components.</p>
 * <p>It extends {@link EmptyBox}, inheriting basic entity creation and naming capabilities.</p>
 *
 * @param <T> The type of the concrete builder extending this abstract class,
 *            allowing for method chaining (fluent API).
 * @see HContainerBuilder
 * @see VContainerBuilder
 * @see EmptyBox
 */
@Slf4j
public abstract class ContainerBuilder<T extends ContainerBuilder<T>> extends EmptyBox<T> implements Box {


    public ContainerBuilder(EntityManager entityManager, Align align) {
        super(entityManager);
        entity.addComponent(align);
    }


    /**
     * Returns the set of child entities currently added to this container builder.
     *
     * @return A {@link Set} of {@link Entity} objects representing the children.
     */

    public List<UUID> children() {
        return this.entity.getChildren();
    }


    /**
     * Adds a child {@link Entity} to this container.
     * This method sets the parent component for the child, updates the child's position,
     * and recalculates the container's dimensions.
     * <p>
     * The {@link Entity} to add as a child.
     *
     * @return The builder instance for method chaining.
     */


    public T addAlin(Align align) {
        log.debug("add alin to entity {}", this.entity);
        this.entity.addComponent(align);
        return self();
    }

    /**
     * Builds the container entity and its children, adding them to the {@link EntityManager}.
     * This method calculates the final content size of the container, considering padding,
     * and then registers the container and all its child entities with the entityManager.
     *
     * @return The built container {@link Entity}.
     */
    @Override
    public Entity build() {
        log.info("building entity {}", this.entity);
        entityManager.putEntity(this.entity);
        return entity;
    }


}


