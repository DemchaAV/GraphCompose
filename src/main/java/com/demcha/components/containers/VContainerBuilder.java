package com.demcha.components.containers;
import com.demcha.components.containers.abstract_builders.AbstractContainerBuilder;
import com.demcha.components.content.VContainer;
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
public class VContainerBuilder extends AbstractContainerBuilder<VContainerBuilder> {
    /**
     * Constructs a new {@code VContainerBuilder} with the specified PDF document.
     *
     * @param document The {@link EntityManager} to which the container will be added.
     */
    public VContainerBuilder(EntityManager document) {
        super(document);
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
    public VContainerBuilder create(Align align) {
        super.create(align); // Call the common logic
        entity.addComponentIfAbsent(new VContainer()); // Add the specific component
        return self();
    }
    /**
     * Updates the position of a child entity within the vertical container.
     * <p>
     * This method adds an {@link Anchor} component to the child, aligning it horizontally
     * according to the container's alignment and vertically to {@link VAnchor#DEFAULT}.
     * It then updates the child's vertical position based on the accumulated primary axis position.
     * </p>
     *
     * @param child The {@link Entity} whose position needs to be updated.
     */
    @Override
    protected void updateChildPosition(Entity child) {
        child.addComponent(new Anchor(align.h(), VAnchor.DEFAULT));
        Position currentPos = child.getComponent(Position.class).orElse(Position.zero());
        // Position along the primary (vertical) axis
        child.addComponent(new Position(currentPos.x(), currentPos.y() + this.primaryAxisPosition));
    }
    /**
     * Updates the dimensions of the container based on the dimensions of the current child entity.
     * <p>
     * For a vertical container:
     * <ul>
     *     <li>The primary axis (height) grows with each child's height plus the spacing defined by {@link Align}.</li>
     *     <li>The secondary axis (width) is the maximum width among all children.</li>
     * </ul>
     * </p>
     *
     * @param child The {@link Entity} whose dimensions are used to update the container's overall size.
     * @throws java.util.NoSuchElementException if the child entity does not have an {@link OuterBoxSize} component.
     */
    @Override
    protected void updateContainerDimensions(Entity child) {
        var outbox = OuterBoxSize.from(child).orElseThrow();
        // Main axis grows with each child
        this.primaryAxisPosition += outbox.height() + align.spacing();
        // Cross axis is the max of all children
        this.secondaryAxisMaxSize = Math.max(secondaryAxisMaxSize, outbox.width());
    }
    /**
     * Calculates the total content size of the vertical container, including padding.
     * <p>
     * The content height is the sum of all child entities' heights plus the spacing between them.
     * The content width is the maximum width of all children.
     * Padding is then added to both dimensions.
     * </p>
     *
     * @param padding The {@link Padding} to be applied to the content size.
     * @return A {@link ContentSize} object representing the calculated dimensions of the container's content.
     * @throws java.util.NoSuchElementException if any child entity does not have an {@link OuterBoxSize} component.
     */
    @Override
    protected ContentSize calculateContentSize(Padding padding) {
        double entitiesHeight = 0;
        Iterator<Entity> iterator = children.iterator();
        while (iterator.hasNext()) {
            Entity current = iterator.next();
            entitiesHeight += OuterBoxSize.from(current).orElseThrow().height();
            if (iterator.hasNext()) {
                entitiesHeight += align.spacing();
            }
        }
        return new ContentSize(secondaryAxisMaxSize + padding.horizontal(), entitiesHeight + padding.vertical());
    }

    @Override
    public void initialize() {
        entity.addComponent(new VContainer());
    }
}
