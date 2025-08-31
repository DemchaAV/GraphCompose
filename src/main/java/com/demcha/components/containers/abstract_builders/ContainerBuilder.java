package com.demcha.components.containers.abstract_builders;


import com.demcha.components.components_builders.HContainerBuilder;
import com.demcha.components.components_builders.VContainerBuilder;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.layout.*;
import com.demcha.components.layout.coordinator.Position;
import com.demcha.components.style.Padding;
import com.demcha.core.EntityManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

    public static final Align DEFAULT_ALIGN = Align.middle(0);

    protected Align align;
    @Setter
    @Getter
    protected StackAxis stackAxis;
    protected double axisHorizontal = 0; // Represents width for H-Container, height for V-Container
    protected double axisVertical = 0; // Represents max height for H-Container, max width for V-Container


    public ContainerBuilder(EntityManager entityManager) {
        super(entityManager);
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
        entity = new Entity();
        autoName();
        this.align = align;
        initialize();
        entity.addComponent(align);
        return self();
    }

    /**
     * Returns the set of child entities currently added to this container builder.
     *
     * @return A {@link Set} of {@link Entity} objects representing the children.
     */
    @Override
    public List<Entity> children() {
        return this.entity.getChildren();
    }

    /**
     * Initializes the container builder with the {@link #DEFAULT_ALIGN}.
     * This is a convenience method that calls {@link #create(Align)} with the default alignment.
     *
     * @return The builder instance for method chaining.
     */
    @Override
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
        child.addComponent(new ParentComponent(entity));
        this.entity.getChildren().add(child);
        return self();
    }

    public T addParrent(Entity parent) {
        this.entity.addComponent(new ParentComponent(parent));
        parent.getChildren().add(entity);
        return self();
    }

    public T addAlin(Align align) {
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
        Padding padding = entity.getComponent(Padding.class).orElse(Padding.zero());

        boolean hasNext = false;
        boolean isLast = false;
        switch (this.stackAxis) {
            case HORIZONTAL, REVERSE_VERTICAL -> {
                for (int i = 0; i < entity.getChildren().size(); i++) {
                    var entity = entity().getChildren().get(i);
                    log.info("HORIZONTAL, REVERSE_VERTICAL build(): {}", entity);
                    updateChildPosition(entity);
                    if (i == (this.entity.getChildren().size() - 2)) {
                        hasNext = true;
                    }
                    if (i == (this.entity.getChildren().size() - 1)) {
                        isLast = true;
                    }
                    updateContainerDimensions(entity, hasNext);
                    rearrange(entity, isLast);
                    entityManager.putEntity(entity);
                }
            }
            case VERTICAL, REVERSE_HORIZONTAL -> {
                for (int i = entity.getChildren().size(); i > 0; i--) {
                    var entity = entity().getChildren().get(i - 1);
                    log.info("VERTICAL, REVERSE_HORIZONTAL build(): {}", entity);
                    updateChildPosition(entity);
                    if (i == entity.getChildren().size() - 2) {
                        hasNext = true;
                    }
                    if (i == (this.entity.getChildren().size() - 1)) {
                        isLast = true;
                    }
                    updateContainerDimensions(entity, hasNext);
                    rearrange(entity, isLast);
                    entityManager.putEntity(entity);
                }
            }
            case DEFAULT -> defaultRearrange();
            case null -> throw new IllegalStateException("Cannot rearrange with null stack axis");
            default -> {
                throw new IllegalStateException("Unexpected value: " + this.stackAxis);
            }
        }

        entity.addComponent(new ContentSize(axisHorizontal, axisVertical));
        entityManager.putEntity(this.entity);

        return entity;
    }

    protected void updateContainerDimensions(Entity entity, boolean isLast) {
        switch (this.stackAxis) {
            case HORIZONTAL, REVERSE_HORIZONTAL -> updateContainerDimensionsHorizontal(entity, isLast);
            case VERTICAL, REVERSE_VERTICAL -> updateContainerDimensionsVertical(entity, isLast);
            case DEFAULT -> defaultRearrange();
            case null -> throw new IllegalStateException("Cannot rearrange with null stack axis");
            default -> {
                throw new IllegalStateException("Unexpected value: " + this.stackAxis);
            }
        }
    }

    protected void rearrange(Entity child, boolean isLast) {
        var out = OuterBoxSize.from(child).orElseThrow();
        switch (this.stackAxis) {
            case HORIZONTAL, REVERSE_HORIZONTAL -> {
                log.info("rearrange: current [{}] axisVertical= {}, axisHorizontal+ {} ", stackAxis, axisVertical, axisHorizontal);
                this.axisVertical = Math.max(this.axisVertical, out.height());
                this.axisHorizontal += isLast ? 0 : align.spacing();
                log.info("rearrange: New [{}] axisVertical= {}, axisHorizontal+ {} ", stackAxis, axisVertical, axisHorizontal);
            }
            case VERTICAL, REVERSE_VERTICAL -> {
                log.info("rearrange: current [{}] axisVertical= {}, axisHorizontal+ {} ", stackAxis, axisVertical, axisHorizontal);
                this.axisHorizontal = Math.max(this.axisHorizontal, out.width());
                this.axisVertical += isLast ? 0 : align.spacing();
                log.info("rearrange: New [{}] axisVertical= {}, axisHorizontal+ {} ", stackAxis, axisVertical, axisHorizontal);

            }
            case DEFAULT -> {
                defaultRearrange();
            }
            case null -> throw new IllegalStateException("Cannot rearrange with null stack axis");
            default -> {
                throw new IllegalStateException("Unexpected value: " + this.stackAxis);
            }
        }
    }

    protected void defaultRearrange() {

    }


    /**
     * Updates the position of a child entity within the horizontal container.
     * For horizontal containers, the primary axis is X. The child's X position
     * is adjusted based on the {@code primaryAxisPosition} accumulated from previous children.
     * A default horizontal anchor is also added to the child.
     *
     * @param child The {@link Entity} whose position needs to be updated.
     */

    protected void updateChildPosition(Entity child) {
        Position cur = child.getComponent(Position.class).orElse(Position.zero());
        var anchor = child.getComponent(Anchor.class).orElse(Anchor.defaultAnchor());
        log.info("Current position {} with Axis {}", child, cur);
        switch (this.stackAxis) {
            case HORIZONTAL, REVERSE_HORIZONTAL -> {
                child.addComponent(new Anchor(HAnchor.DEFAULT, anchor.v())); // выравнивание по вертикали из Align
                Position c = new Position(cur.x() + this.axisHorizontal, cur.y());
                log.info("New position {} with Axis {}", child, c);
                child.addComponent(c);
            }
            case VERTICAL, REVERSE_VERTICAL -> {
                child.addComponent(new Anchor(anchor.h(), VAnchor.DEFAULT)); // выравнивание по горизонтали из Align
                Position c = new Position(cur.x(), cur.y() + this.axisVertical);
                log.info("New position {} with Axis {}", child, c);
                child.addComponent(c);
            }
            case DEFAULT -> defaultRearrange();
            case null -> throw new IllegalStateException("Cannot rearrange with null stack axis");
            default -> {
                throw new IllegalStateException("Unexpected value: " + this.stackAxis);
            }
        }

    }

    /**
     * Updates the dimensions of the container based on the dimensions of a child entity.
     * The container's width (primary axis) grows with each child's width plus spacing.
     * The container's height (secondary axis) is determined by the maximum height
     * among all its children.
     *
     * @param child The {@link Entity} whose dimensions contribute to the container's overall size.
     */

    protected void updateContainerDimensionsHorizontal(Entity child, boolean isLast) {

        var outbox = OuterBoxSize.from(child).orElseThrow();
        log.debug("{}", outbox);
        // Main axis grows with each child
        log.info("updateContainerDimensionsHorizontal: Current axisHorizontal={}, axisVertical={}", axisHorizontal, axisVertical);
        this.axisHorizontal += outbox.width() ;
        // Cross axis is the max of all children
        this.axisVertical = Math.max(axisVertical, outbox.height());
        log.info("updateContainerDimensionsHorizontal: Calculated axisHorizontal={}, axisVertical={}", axisHorizontal, axisVertical);
    }

    protected void updateContainerDimensionsVertical(Entity child, boolean isLast) {

        var outbox = OuterBoxSize.from(child).orElseThrow();
        log.info("updateContainerDimensionsVertical: Current axisHorizontal={}, axisVertical={}", axisHorizontal, axisVertical);
        // Main axis grows with each child
        this.axisVertical += outbox.height() ;
        // Cross axis is the max of all children
        this.axisHorizontal = Math.max(axisHorizontal, outbox.height());
        log.info("updateContainerDimensionsVertical: Calculated axisHorizontal={}, axisVertical={}", axisHorizontal, axisVertical);
    }

    /**
     * Calculates the total content size of the horizontal container, including padding.
     * The width is the sum of all child widths plus spacing between them, plus horizontal padding.
     * The height is the maximum height of any child, plus vertical padding.
     *
     * @param padding The {@link Padding} applied to the container.
     * @return A {@link ContentSize} object representing the calculated dimensions.
     */

    protected ContentSize calculateContentSize(Padding padding) {
        double entitiesWidth = 0;
        Iterator<Entity> iterator = this.entity.getChildren().iterator();
        while (iterator.hasNext()) {
            Entity current = iterator.next();
            entitiesWidth += OuterBoxSize.from(current).orElseThrow().width();
            if (iterator.hasNext()) {
                entitiesWidth += align.spacing();
            }
        }
        return new ContentSize(entitiesWidth + padding.horizontal(), axisVertical + padding.vertical());
    }
}


