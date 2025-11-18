package com.demcha.system;

import com.demcha.components.core.Entity;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.exeptions.RenderGuideLinesException;
import lombok.NonNull;

import java.io.IOException;
import java.util.EnumSet;

public interface GuidesRenderer {


    default boolean renderGuides(@NonNull Entity entity, EnumSet<Guide> guides) throws IOException {
        boolean any = false;

        var placement = entity.getComponent(Placement.class).orElseThrow();

        if (placement.startPage() != placement.endPage()) {
            if (placement.startPage() < 0 || placement.endPage() < 0) {
                throw new RenderGuideLinesException(entity + " Page " + placement.startPage() + " " + placement.endPage() + " Page can not be a negative number.");
            }

            for (int pageNumber = placement.startPage(); pageNumber >= placement.endPage(); pageNumber--) {

                if (pageNumber == placement.startPage()) {
                    if (guides.contains(Guide.MARGIN)) any |= marginRenderStart(entity, pageNumber);
                    if (guides.contains(Guide.PADDING)) any |= paddingRenderStart(entity, pageNumber);
                    if (guides.contains(Guide.BOX)) any |= boxRenderStart(entity, pageNumber);
                } else if (pageNumber == placement.endPage()) {
                    if (guides.contains(Guide.MARGIN)) any |= marginRenderEnd(entity, pageNumber);
                    if (guides.contains(Guide.PADDING)) any |= paddingRenderEnd(entity, pageNumber);
                    if (guides.contains(Guide.BOX)) any |= boxRenderEnd(entity, pageNumber);

                } else {
                    if (guides.contains(Guide.MARGIN)) any |= marginRenderMiddle(entity, pageNumber);
                    if (guides.contains(Guide.PADDING)) any |= paddingRenderMiddle(entity, pageNumber);
                    if (guides.contains(Guide.BOX)) any |= boxRenderMiddle(entity, pageNumber);

                }

            }
        } else {
            if (guides.contains(Guide.MARGIN)) any |= marginRender(entity);
            if (guides.contains(Guide.PADDING)) any |= paddingRender(entity);
            if (guides.contains(Guide.BOX)) any |= boxRender(entity);
        }


        return any;
    }

    boolean boxRenderMiddle(@NonNull Entity entity, int pageNumber) throws RenderGuideLinesException;


    boolean paddingRenderMiddle(@NonNull Entity entity, int pageNumber) throws RenderGuideLinesException;


    boolean marginRenderMiddle(@NonNull Entity entity, int pageNumber) throws RenderGuideLinesException;


    boolean marginRenderEnd(@NonNull Entity entity, int pageNumber) throws RenderGuideLinesException;


    boolean paddingRenderEnd(@NonNull Entity entity, int pageNumber) throws RenderGuideLinesException;


    boolean boxRenderEnd(@NonNull Entity entity, int pageNumber) throws RenderGuideLinesException;


    boolean boxRenderStart(@NonNull Entity entity, int pageNumber) throws RenderGuideLinesException;


    boolean paddingRenderStart(@NonNull Entity entity, int pageNumber) throws RenderGuideLinesException;


    boolean marginRenderStart(@NonNull Entity entity, int pageNumber) throws RenderGuideLinesException;


    boolean boxRender(Entity e) throws RenderGuideLinesException;

    boolean paddingRender(Entity e) throws RenderGuideLinesException;

    boolean marginRender(Entity e) throws RenderGuideLinesException;


    enum Guide {MARGIN, PADDING, BOX}
}
