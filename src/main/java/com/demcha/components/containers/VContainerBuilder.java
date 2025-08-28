package com.demcha.components.containers;

import com.demcha.components.containers.abstract_builders.EmptyBox;
import com.demcha.components.content.Box;
import com.demcha.components.content.components_builders.ComponentBoxBuilder;
import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.core.EntityName;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.layout.*;
import com.demcha.components.layout.coordinator.Position;
import com.demcha.components.style.Padding;
import com.demcha.core.PdfDocument;
import com.demcha.system.PdfRender;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Slf4j
public class VContainerBuilder extends EmptyBox<VContainerBuilder> {
    public static Align DEFAUT_ALIGN = Align.middle(5);
    private Align align;
    private Set<Entity> entities;
    private double position;
    private double width =0;
    private static final EnumSet<GuidesRenderer.Guide> DEFAULT_GUIDES =
            EnumSet.of(GuidesRenderer.Guide.MARGIN, GuidesRenderer.Guide.PADDING, GuidesRenderer.Guide.BOX);

    public VContainerBuilder(PdfDocument document) {
        super(document);
        this.entities = new HashSet<>();
    }


    public VContainerBuilder create() {
        return create(DEFAUT_ALIGN);
    }

    public VContainerBuilder create(Align align) {
        this.align = align;
        String simpleName = self().getClass().getSimpleName();
        String defaultName = simpleName + "_" + entity.getId().toString().substring(0, 5);
        entity.addComponent(new HContainer());
        entity.addComponent(align);
        entity.addComponent(new EntityName(defaultName));
        return self();
    }


    public VContainerBuilder add(Entity entity) {
        entity.addComponent(new ParentComponent(this.entity));
        entity.addComponent(new Anchor(align.h(),VAnchor.DEFAULT));
        var position = entity.getComponent(Position.class).orElse(Position.zero());
        entity.addComponent(new Position(position.x() , position.y()+this.position));
        var outbox = OuterBoxSize.from(entity).orElseThrow();
        this.width = Math.max(width,outbox.width());
        this.position += outbox.height()+ align.spacing();
        log.debug("Added entity {} at position {}", entity, position);
        entities.add(entity);
        return this;
    }

    private double entitiesHigh() {
        double high = 0;
        Iterator<Entity> iterator = entities.iterator(); // один итератор

        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            var outBoxSize = OuterBoxSize.from(entity).orElseThrow();
            high += outBoxSize.height();

            // если ещё есть элементы, добавляем spacing
            if (iterator.hasNext()) {
                high += align.spacing();
            }
        }
        return high;
    }
@Override
    public Entity build() {
        Padding padding = entity.getComponent(Padding.class).orElse(Padding.zero());
        var h = entitiesHigh();
        entity.addComponent(new ContentSize(width + padding.horizontal(), h + padding.vertical()));
        document.putEntity(entity);
        for (Entity entity : entities) {
            document.putEntity(entity);
        }
        return entity;
    }


}
