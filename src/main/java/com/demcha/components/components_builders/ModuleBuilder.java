package com.demcha.components.components_builders;

import com.demcha.components.containers.abstract_builders.ContainerBuilder;
import com.demcha.components.containers.abstract_builders.StackAxis;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.InnerBoxSize;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.layout.Align;
import com.demcha.components.renderable.HContainer;
import com.demcha.components.style.Margin;
import com.demcha.components.style.Padding;
import com.demcha.core.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPage;

@Slf4j
public class ModuleBuilder extends ContainerBuilder<ModuleBuilder> {
    private final ContentSize canvasSize;

    /**
     * Constructs a new {@code ModuleBuilder} associated with a specific Entity Manager.
     *
     * @param entityManager The {@link EntityManager} to which the container and its entities will belong.
     */
    public ModuleBuilder(EntityManager entityManager) {
        this(entityManager, (ContentSize) null);
    }

    public ModuleBuilder(EntityManager entityManager, ContentSize canvasSize) {
        super(entityManager);
        this.stackAxis = StackAxis.VERTICAL;
        this.canvasSize = canvasSize;

    }
    public ModuleBuilder(EntityManager entityManager, PDPage page) {
        this(entityManager, new ContentSize(page.getMediaBox().getWidth(), page.getMediaBox().getHeight()));

    }
    public ModuleBuilder(EntityManager entityManager, InnerBoxSize innerBoxSize) {
        this(entityManager, new ContentSize(innerBoxSize.innerW(), innerBoxSize.innerH()));
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
    public ModuleBuilder create(Align align) {
        super.create(align); // Call the common logic
        entity.addComponentIfAbsent(new HContainer()); // Add the specific component
        return self();
    }


    @Override
    public void initialize() {
        entity.addComponent(new com.demcha.components.renderable.Module());
        entity.addComponent(canvasSize==null?new ContentSize(0,0): new ContentSize(canvasSize.width(), 0));
    }

    @Override
    public Entity build() {
        Padding padding = entity.getComponent(Padding.class).orElse(Padding.zero());
        Margin margin = entity.getComponent(Margin.class).orElse(Margin.zero());
        boolean hasNext = false;
        boolean isLast = false;
        switch (this.stackAxis) {
            case HORIZONTAL, REVERSE_VERTICAL -> {
                for (int i = 0; i < entity.getChildren().size(); i++) {
                    var entity = entity().getChildren().get(i);
//                    fitInParent(entity);
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
//                    fitInParent(entity);
                    log.info("VERTICAL, REVERSE_HORIZONTAL build(): {}", entity);
                    updateChildPosition(entity);
                    if (i == entity.getChildren().size() - 1) {
                        hasNext = true;
                    }
                    if (i == (this.entity.getChildren().size() -2)) {
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

        entity.addComponent(new ContentSize(canvasSize==null? axisHorizontal:canvasSize.width()-padding.horizontal() - margin.horizontal(), axisVertical));
        entityManager.putEntity(this.entity);

        return entity;
    }

    private  void fitInParent(Entity child) {
        var childOuter = OuterBoxSize.from(child).orElseThrow();
        var childSize = child.getComponent(ContentSize.class).orElseThrow();
        double availibleW = InnerBoxSize.from(entity).orElseThrow().innerW();
        double different = availibleW - childOuter.width();
        if (different > 0){
            child.addComponent(new ContentSize(availibleW, childSize.height()));
        }
    }
}
