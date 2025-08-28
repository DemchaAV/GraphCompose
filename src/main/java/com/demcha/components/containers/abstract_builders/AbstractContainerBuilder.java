package com.demcha.components.containers.abstract_builders;


import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.Align;
import com.demcha.components.layout.ParentComponent;
import com.demcha.components.style.Padding;
import com.demcha.core.PdfDocument;

import java.util.HashSet;
import java.util.Set;

/**
 * An abstract base class for building container components (e.g., HContainer, VContainer)
 * within a PDF document. This class provides common functionality for managing child entities,
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
 * @see com.demcha.components.containers.HContainerBuilder
 * @see com.demcha.components.containers.VContainerBuilder
 * @see EmptyBox
 */
public abstract class AbstractContainerBuilder<T extends AbstractContainerBuilder<T>> extends EmptyBox<T> {

    public static final Align DEFAULT_ALIGN = Align.middle(5);

    protected final Set<Entity> entities;
    protected Align align;
    protected double primaryAxisPosition = 0; // Represents width for H-Container, height for V-Container
    protected double secondaryAxisMaxSize = 0; // Represents max height for H-Container, max width for V-Container

    public AbstractContainerBuilder(PdfDocument document) {
        super(document);
        this.entities = new HashSet<>();
    }

    /**
     * Initializes the container builder with a specified alignment.
     * This method sets the alignment for the container and automatically names the entity.
     * Subclasses are expected to add their specific container component (e.g., HContainer, VContainer)
     * after this method is called.
     *
     * @param align The {@link Align} component to set for the container.
     * @return The builder instance for method chaining.
     */
    public T create(Align align) {
        this.align = align;
        autoName();
        entity.addComponent(align);
        // Let subclasses add the specific container component (HContainer or VContainer)
        return self();
    }

    /**
     * Initializes the container builder with the {@link #DEFAULT_ALIGN}.
     * This is a convenience method that calls {@link #create(Align)} with the default alignment.
     *
     * @return The builder instance for method chaining.
     */
    public T create() {
        return create(DEFAULT_ALIGN);
    }

    /**
     * Adds a child {@link Entity} to this container.
     * This method sets the parent component for the child, updates the child's position,
     * and recalculates the container's dimensions.
     *
     * @param child The {@link Entity} to add as a child.
     * @return The builder instance for method chaining.
     */
    public T addChild(Entity child) {
        child.addComponent(new ParentComponent(this.entity));

        // Delegate positioning and dimension calculation to subclasses
        updateChildPosition(child);
        updateContainerDimensions(child);

        entities.add(child);
        return self();
    }

    /**
     * Builds the container entity and its children, adding them to the {@link PdfDocument}.
     * This method calculates the final content size of the container, considering padding,
     * and then registers the container and all its child entities with the document.
     *
     * @return The built container {@link Entity}.
     */
    @Override
    public Entity build() {
        Padding padding = entity.getComponent(Padding.class).orElse(Padding.zero());

        // Delegate final dimension calculation to subclasses
        ContentSize contentSize = calculateContentSize(padding);
        entity.addComponent(contentSize);

        document.putEntity(this.entity);
        for (Entity entity : entities) {
            document.putEntity(entity);
        }
        return entity;
    }

    /**
     * Abstract method to be implemented by subclasses to define how a child entity's
     * position is updated within the container.
     *
     * @param child The child {@link Entity} whose position needs to be updated.
     */
    protected abstract void updateChildPosition(Entity child);

    /**
     * Abstract method to be implemented by subclasses to define how the container's
     * dimensions are updated based on the addition of a new child entity.
     *
     * @param child The newly added child {@link Entity} that influences container dimensions.
     */
    protected abstract void updateContainerDimensions(Entity child);

    /**
     * Abstract method to be implemented by subclasses to calculate the final
     * {@link ContentSize} of the container, taking into account its children and padding.
     *
     * @param padding The {@link Padding} applied to the container.
     * @return The calculated {@link ContentSize} of the container.
     */
    protected abstract ContentSize calculateContentSize(Padding padding);
}