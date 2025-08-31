package com.demcha.components.containers.abstract_builders;

import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.layout.ParentComponent;
import com.demcha.components.style.Padding;
import com.demcha.core.EntityManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

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
@Getter
//@RequiredArgsConstructor
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
        create();
    }

    T parrent(ParentComponent parent) {
        entity.addComponent(parent);
        return self();
    }

    T parrent(Entity parent) {
        return parrent(new ParentComponent(parent));
    }

    public T addChild(Entity child) {
        child.addComponent(new ParentComponent(this.entity));
        return self();
    }

    public T addChildAndFit(Entity child) {
        var outerBox = OuterBoxSize.from(child).get();
        child.addComponent(new ParentComponent(this.entity));
        var component = entity().getComponent(ContentSize.class).orElseThrow();
        var padding = entity().getComponent(Padding.class).orElse(Padding.zero());
        var newComponentSize = new ContentSize(Math.max(component.width(), outerBox.width() + padding.horizontal())
                , Math.max(component.height(), outerBox.height() + padding.vertical()));
        entity().addComponent(newComponentSize);

        return self();
    }

    public T addParent(Entity parent) {
        addChild(this.entity);
        return self();
    }

    @Override
    public EntityManager manager() {
        return entityManager;
    }
}
