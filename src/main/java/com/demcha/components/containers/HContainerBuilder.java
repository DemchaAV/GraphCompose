package com.demcha.components.containers;

import com.demcha.components.content.Box;
import com.demcha.components.content.components_builders.ComponentBoxBuilder;
import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.layout.*;
import com.demcha.components.layout.coordinator.Position;
import com.demcha.components.style.Padding;
import com.demcha.core.PdfDocument;
import com.demcha.system.PdfRender;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Slf4j
public class HContainerBuilder extends ComponentBoxBuilder<HContainerBuilder> implements Box, Component, PdfRender, GuidesRenderer {
    private static final EnumSet<Guide> DEFAULT_GUIDES =
            EnumSet.of(Guide.MARGIN, Guide.PADDING, Guide.BOX);
    private Align align;
    @Getter
    private Set<Entity> entities;
    private double position;
    private double height = 0;
    private double width = 0;

    public HContainerBuilder(Align align, Set<Entity> entities) {
        this.align = align;
        this.entities = entities;
    }

    public HContainerBuilder(Align align) {
        this(align, new HashSet<>());
    }

    public static HContainerBuilder create(Align align) {

        HContainerBuilder hContainerBuilder = new HContainerBuilder(align, new HashSet<Entity>());
        hContainerBuilder.addComponent(new HContainer());
        return hContainerBuilder;
    }

    public static HContainerBuilder create(Entity entity) {
        var align = entity.getComponent(Align.class).orElseThrow();


        HContainerBuilder hContainerBuilder = new HContainerBuilder(align, new HashSet<Entity>());
        hContainerBuilder.addComponent(new HContainer());
        return hContainerBuilder;
    }


    public HContainerBuilder add(Entity entity) {
        entity.addComponent(new ParentComponent(this.entity));
        entity.addComponent(new Anchor(HAnchor.DEFAULT, align.v()));
        var position = entity.getComponent(Position.class).orElse(Position.zero());
        entity.addComponent(new Position(position.x() + this.position, position.y()));
        var outbox = OuterBoxSize.from(entity).orElseThrow();
        this.height = Math.max(height, outbox.height());
        this.position += outbox.width() + align.spacing();
        log.debug("Added entity {} at position {}", entity, position);
        entities.add(entity);
        return self();
    }

    private double entitiesWidth() {
        double width = 0;
        Iterator<Entity> iterator = entities.iterator(); // один итератор

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

    public Entity build(PdfDocument document) {
        Padding padding = entity.getComponent(Padding.class).orElse(Padding.zero());
        var w = entitiesWidth();
        entity.addComponent(new ContentSize(w + padding.horizontal(), height + padding.vertical()));
        document.putEntity(entity);
        for (Entity entity : entities) {
            document.putEntity(entity);
        }
        return entity;
    }

    @Override
    protected HContainerBuilder self() {
        return this;
    }

    @Override
    public boolean render(Entity e, PDPageContentStream cs, boolean guideLines) throws IOException {
        if (guideLines) {
            renderGuides(e, cs, DEFAULT_GUIDES);
        }
        return true;
    }

}
