package com.demcha.components.containers;

import com.demcha.components.containers.moduls.EmptyBox;
import com.demcha.components.core.Entity;
import com.demcha.components.core.EntityName;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.layout.Align;
import com.demcha.components.layout.Anchor;
import com.demcha.components.layout.HAnchor;
import com.demcha.components.layout.ParentComponent;
import com.demcha.components.layout.coordinator.Position;
import com.demcha.components.style.Padding;
import com.demcha.core.PdfDocument;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Slf4j

public class HContainerBuilder extends EmptyBox<HContainerBuilder> {
    public static Align DEFAUT_ALIGN = Align.middle(5);
    private final Set<Entity> entities = new HashSet<>();
    private Align align;
    private double curPosition = 0;
    private double height = 0;

    public HContainerBuilder(PdfDocument document) {
        super(document);
    }

    public HContainerBuilder create() {
        return create(DEFAUT_ALIGN);
    }

    public HContainerBuilder create(Align align) {
        this.align = align;
        String simpleName = self().getClass().getSimpleName();
        String defaultName = simpleName + "_" + entity.getId().toString().substring(0, 5);
        entity.addComponent(new HContainer());
        entity.addComponent(align);
        entity.addComponent(new EntityName(defaultName));
        return self();
    }


    public HContainerBuilder addChild(Entity entity) {
        entity.addComponent(new ParentComponent(getEntity()));
        entity.addComponent(new Anchor(HAnchor.DEFAULT, align.v()));
        var position = entity.getComponent(Position.class).orElse(Position.zero());
        entity.addComponent(new Position(position.x() + this.curPosition, position.y()));
        var outbox = OuterBoxSize.from(entity).orElseThrow();
        this.height = Math.max(height, outbox.height());
        this.curPosition += outbox.width() + align.spacing();
        log.debug("Added entity {} at position {}", entity, position);
        entities.add(entity);
        return this;
    }

    private double entitiesWidth() {
        double width = 0;
        Iterator<Entity> iterator = entities.iterator();

        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            var outBoxSize = OuterBoxSize.from(entity).orElseThrow();
            width += outBoxSize.width();

            // если ещё есть элементы, добавляем spacing
            if (iterator.hasNext()) {
                width += align.spacing();
            }
        }
        return width;
    }

    public Entity build() {
        Padding padding = this.entity.getComponent(Padding.class).orElse(Padding.zero());
        var w = entitiesWidth();
        this.entity.addComponent(new ContentSize(w + padding.horizontal(), height + padding.vertical()));
        document.putEntity(this.entity);
        for (Entity entity : entities) {
            document.putEntity(entity);
        }
        return this.entity;
    }

}
