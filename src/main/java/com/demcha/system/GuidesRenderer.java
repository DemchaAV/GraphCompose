package com.demcha.system;

import com.demcha.components.core.Entity;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.exeptions.RenderGuideLinesException;
import lombok.NonNull;

import java.io.IOException;
import java.util.EnumSet;

public interface GuidesRenderer {


    default  boolean renderGuides(@NonNull Entity entity, EnumSet<Guide> guides) throws IOException {
        boolean any = false;

        var placement = entity.getComponent(Placement.class).orElseThrow();

        if (placement.startPage() != placement.endPage()) {
            if (placement.startPage() < 0 || placement.endPage() < 0) {
                throw new RenderGuideLinesException(entity + " Page " + placement.startPage() + " " + placement.endPage() + " Page can not be a negative number.");
            }

            for (int pageNumber = placement.startPage(); pageNumber >= placement.endPage(); pageNumber--) {

                if (pageNumber == placement.startPage()) {
                    if (guides.contains(Guide.MARGIN)) any |= renderMarginStart(entity, pageNumber);
                    if (guides.contains(Guide.PADDING)) any |= renderPaddingStart(entity, pageNumber);
                    if (guides.contains(Guide.BOX)) any |= boxRenderStart(entity, pageNumber);
                } else if (pageNumber == placement.endPage()) {
                    if (guides.contains(Guide.MARGIN)) any |= renderMarginEnd(entity, pageNumber);
                    if (guides.contains(Guide.PADDING)) any |= renderPaddingEnd(entity, pageNumber);
                    if (guides.contains(Guide.BOX)) any |= boxRenderEnd(entity, pageNumber);

                } else {
                    if (guides.contains(Guide.MARGIN)) any |= renderMarginMiddle(entity, pageNumber);
                    if (guides.contains(Guide.PADDING)) any |= renderPaddingMiddle(entity, pageNumber);
                    if (guides.contains(Guide.BOX)) any |= boxRenderMiddle(entity, pageNumber);

                }

            }
        }else{
            if (guides.contains(Guide.MARGIN)) any |= renderMargin(entity);
            if (guides.contains(Guide.PADDING)) any |= renderPadding(entity);
            if (guides.contains(Guide.BOX)) any |= boxRender(entity);
        }



        return any;
    }

    boolean boxRenderMiddle(@NonNull Entity entity, int pageNumber)throws RenderGuideLinesException;

    <T extends RenderStream<?>> boolean boxRenderMiddle(@NonNull Entity entity, T stream)throws RenderGuideLinesException;

    boolean renderPaddingMiddle(@NonNull Entity entity, int pageNumber)throws RenderGuideLinesException;

    <T extends RenderStream<?>> boolean renderPaddingMiddle(@NonNull Entity entity, T stream)throws RenderGuideLinesException;

    boolean renderMarginMiddle(@NonNull Entity entity, int pageNumber)throws RenderGuideLinesException;

    <T extends RenderStream<?>> boolean renderMarginMiddle(@NonNull Entity entity, T stream)throws RenderGuideLinesException;

    boolean renderMarginEnd(@NonNull Entity entity, int pageNumber)throws RenderGuideLinesException;

    <T extends RenderStream<?>> boolean renderMarginEnd(@NonNull Entity entity, T stream)throws RenderGuideLinesException;

    boolean renderPaddingEnd(@NonNull Entity entity, int pageNumber)throws RenderGuideLinesException;

    <T extends RenderStream<?>> boolean renderPaddingEnd(@NonNull Entity entity, T stream)throws RenderGuideLinesException;

    boolean boxRenderEnd(@NonNull Entity entity, int pageNumber)throws RenderGuideLinesException;

    <T extends RenderStream<?>> boolean boxRenderEnd(@NonNull Entity entity, T stream)throws RenderGuideLinesException;

    boolean boxRenderStart(@NonNull Entity entity, int pageNumber)throws RenderGuideLinesException;

    <T extends RenderStream<?>> boolean boxRenderStart(@NonNull Entity entity, T stream)throws RenderGuideLinesException;

    boolean renderPaddingStart(@NonNull Entity entity, int pageNumber)throws RenderGuideLinesException;

    <T extends RenderStream<?>> boolean renderPaddingStart(@NonNull Entity entity, T stream)throws RenderGuideLinesException;

    boolean renderMarginStart(@NonNull Entity entity, int pageNumber)throws RenderGuideLinesException;

    <T extends RenderStream<?>> boolean renderMarginStart(@NonNull Entity entity, T start)throws RenderGuideLinesException;


    <T extends RenderingSystemECS> boolean boxRender(Entity e) throws RenderGuideLinesException;

    <T extends RenderingSystemECS> boolean renderPadding(Entity e) throws RenderGuideLinesException;

    <T extends RenderingSystemECS> boolean renderMargin(Entity e) throws RenderGuideLinesException;


    enum Guide {MARGIN, PADDING, BOX}
}
