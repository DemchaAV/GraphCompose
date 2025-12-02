package com.demcha.loyaut_core.components.containers.abstract_builders;

import com.demcha.loyaut_core.components.core.Entity;
import com.demcha.loyaut_core.components.layout.ParentComponent;
import com.demcha.loyaut_core.core.EntityManager;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base class for building entities that represent an empty box or a container
 * within a Entity Manager. This class extends {@link EntityBuilderBase} and implements
 * {@link BuildEntity}, providing a foundation for components that might not render
 * visible content themselves but serve as structural elements or placeholders.
 *
 * <p>It is designed to be extended by concrete builder classes that define how to
 * construct specific empty box-like entities within the PDF.
 *
 * @param <T> The type of the entity that this builder will construct.
 */
@Slf4j
@Getter
@Accessors(fluent = true)
public abstract class EmptyBox<T> extends EntityBuilderBase<T> implements BuildEntity {

    /**
     * The Entity Manager instance to which the built entity will be added or associated.
     * This entityManager is typically provided during the construction of the builder
     * and is used by concrete implementations to interact with the PDF.
     */
    protected final EntityManager entityManager;

    protected EmptyBox(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.entity = new Entity();
        autoName();
        initialize();
        log.info("Created entity {}", self());
    }

    T addParrent(ParentComponent parent) {
        var parrentEntity = entityManager.getEntity(parent.uuid()).orElseThrow(()->{
            return new IllegalStateException("No found entity with id %s".formatted(parent.uuid()));
        });

        return addParrent(parrentEntity);
    }

    T addParrent(Entity parent) {
        log.info("Add parent {}", parent);
        this.entity.addComponent(new ParentComponent(parent.getUuid()));
        parent.getChildren().add(this.entity.getUuid());
        return self();
    }

    public T addChild(Entity child) {
        log.info("Add child {} to parent {}", child, this.entity);
        child.addComponent(new ParentComponent(this.entity));
        this.entity.getChildren().add(child.getUuid());
        return self();
    }

    @Override
    public EntityManager manager() {
        return entityManager;
    }
}
