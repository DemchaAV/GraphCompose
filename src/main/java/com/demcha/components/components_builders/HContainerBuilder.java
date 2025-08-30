package com.demcha.components.components_builders;

import com.demcha.components.containers.abstract_builders.ContainerBuilder;
import com.demcha.components.renderable.HContainer;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.layout.Align;
import com.demcha.components.layout.Anchor;
import com.demcha.components.layout.HAnchor;
import com.demcha.components.layout.coordinator.Position;
import com.demcha.components.style.Padding;
import com.demcha.core.EntityManager;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;

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
    public HContainerBuilder(EntityManager entityManager) {
        super(entityManager);
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
    public HContainerBuilder create(Align align) {
        super.create(align); // Call the common logic
        entity.addComponentIfAbsent(new HContainer()); // Add the specific component
        return self();
    }

    /**
     * Updates the position of a child entity within the horizontal container.
     * For horizontal containers, the primary axis is X. The child's X position
     * is adjusted based on the {@code primaryAxisPosition} accumulated from previous children.
     * A default horizontal anchor is also added to the child.
     *
     * @param child The {@link Entity} whose position needs to be updated.
     */
    @Override
    protected void updateChildPosition(Entity child) {
        child.addComponent(new Anchor(HAnchor.DEFAULT, align.v()));
        Position currentPos = child.getComponent(Position.class).orElse(Position.zero());
        // Position along the primary (horizontal) axis
        child.addComponent(new Position(currentPos.x() + this.primaryAxisPosition, currentPos.y()));
    }

    /**
     * Updates the dimensions of the container based on the dimensions of a child entity.
     * The container's width (primary axis) grows with each child's width plus spacing.
     * The container's height (secondary axis) is determined by the maximum height
     * among all its children.
     *
     * @param child The {@link Entity} whose dimensions contribute to the container's overall size.
     */
    @Override
    protected void updateContainerDimensions(Entity child) {
        var outbox = OuterBoxSize.from(child).orElseThrow();
        // Main axis grows with each child
        this.primaryAxisPosition += outbox.width() + align.spacing();
        // Cross axis is the max of all children
        this.secondaryAxisMaxSize = Math.max(secondaryAxisMaxSize, outbox.height());
    }

    /**
     * Calculates the total content size of the horizontal container, including padding.
     * The width is the sum of all child widths plus spacing between them, plus horizontal padding.
     * The height is the maximum height of any child, plus vertical padding.
     *
     * @param padding The {@link Padding} applied to the container.
     * @return A {@link ContentSize} object representing the calculated dimensions.
     */
    @Override
    protected ContentSize calculateContentSize(Padding padding) {
        double entitiesWidth = 0;
        Iterator<Entity> iterator = children.iterator();
        while (iterator.hasNext()) {
            Entity current = iterator.next();
            entitiesWidth += OuterBoxSize.from(current).orElseThrow().width();
            if (iterator.hasNext()) {
                entitiesWidth += align.spacing();
            }
        }
        return new ContentSize(entitiesWidth + padding.horizontal(), secondaryAxisMaxSize + padding.vertical());
    }

    @Override
    public void initialize() {
        entity.addComponent(new HContainer());
    }
}