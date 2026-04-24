package com.demcha.compose.testsupport.engine.assembly.container;

import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.layout.ParentComponent;
import com.demcha.compose.engine.core.EntityManager;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * Base builder for entity-producing builders that do not manage a specialized
 * child layout contract of their own.
 * <p>
 * {@code EmptyBox} creates the underlying {@code Entity}, applies shared builder
 * behavior inherited from {@code EntityBuilderBase}, and knows how to register
 * the final entity in the shared {@code EntityManager}. Leaf builders such as
 * text, image, line, and shape builders typically extend this class directly.
 * Container-oriented builders also inherit from it through {@code ContainerBuilder}.
 * </p>
 *
 * @param <T> the concrete builder type for fluent chaining
 */
@Slf4j
@Getter
@Accessors(fluent = true)
public abstract class EmptyBox<T> extends EntityBuilderBase<T> implements BuildEntity {

    /**
     * Shared entity registry that receives the built entity.
     */
    protected final EntityManager entityManager;
    private boolean built;

    protected EmptyBox(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.entity = new Entity();
        autoName();
        initialize();
        log.debug("Created entity {}", self());
    }

    T addParrent(ParentComponent parent) {
        var parrentEntity = entityManager.getEntity(parent.uuid()).orElseThrow(()->{
            return new IllegalStateException("No found entity with id %s".formatted(parent.uuid()));
        });

        return addParrent(parrentEntity);
    }

    T addParrent(Entity parent) {
        log.debug("Add parent {}", parent);
        this.entity.addComponent(new ParentComponent(parent.getUuid()));
        parent.getChildren().add(this.entity.getUuid());
        return self();
    }

    public T addChild(Entity child) {
        log.debug("Add child {} to parent {}", child, this.entity);
        child.addComponent(new ParentComponent(this.entity));
        this.entity.getChildren().add(child.getUuid());
        return self();
    }

    @Override
    public boolean built() {
        return built;
    }

    /**
     * Finalizes and registers the current entity in the {@code EntityManager}.
     *
     * <p>Most subclasses only need to attach the right components before
     * delegating to this behavior.</p>
     */
    @Override
    public Entity build() {
        return registerBuiltEntity();
    }

    protected final Entity registerBuiltEntity() {
        entityManager.putEntity(entity);
        built = true;
        return entity;
    }
}
