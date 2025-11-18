package com.demcha.system.intarfaces;

import com.demcha.components.core.Entity;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.exeptions.RenderGuideLinesException;
import lombok.NonNull;

import java.io.IOException;
import java.util.EnumSet;

/**
 * An interface for rendering visual debugging guides for layout components.
 * <p>
 * This renderer provides methods to draw visual representations of an {@link Entity}'s
 * margin, padding, and bounding box. It is designed to handle elements that may be
 * broken across multiple pages, providing distinct rendering logic for the start, middle,
 * and end parts of a broken element.
 * <p>
 * The primary entry point is {@link #guidesRender(Entity, EnumSet)}, which orchestrates
 * the rendering process based on the entity's {@link Placement} component.
 */
public interface GuidesRenderer {

    /**
     * Renders the specified guides for a given entity.
     * <p>
     * This method checks if the entity is broken across multiple pages by inspecting its
     * {@link Placement} component.
     * <ul>
     *     <li>If the entity exists on a single page ({@code startPage == endPage}), it calls
     *     the single-page rendering methods (e.g., {@link #marginRender(Entity)}).</li>
     *     <li>If the entity is broken, it iterates through the pages from start to end,
     *     calling the appropriate part-specific rendering methods:
     *         <ul>
     *             <li>{@code render...Start()} for the first page.</li>
     *             <li>{@code render...Middle()} for any pages in between.</li>
     *             <li>{@code render...End()} for the last page.</li>
     *         </ul>
     *     </li>
     * </ul>
     *
     * @param entity The entity for which to render guides. Must not be null.
     * @param guides An {@link EnumSet} specifying which guides to render (e.g., {@code Guide.MARGIN}).
     * @return {@code true} if any guide was successfully rendered, {@code false} otherwise.
     * @throws IOException               if an I/O error occurs during rendering.
     * @throws RenderGuideLinesException if a rendering-specific error occurs.
     */
    default boolean guidesRender(@NonNull Entity entity, EnumSet<Guide> guides) throws IOException {
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

    /**
     * Renders the middle part of a bounding box for a broken element.
     * This typically includes only the vertical sides of the box.
     *
     * @param entity     The entity to render.
     * @param pageNumber The page number on which to render.
     * @return {@code true} if rendering was successful.
     * @throws RenderGuideLinesException if a rendering error occurs.
     */
    boolean boxRenderMiddle(@NonNull Entity entity, int pageNumber) throws RenderGuideLinesException;

    /**
     * Renders the middle part of a padding guide for a broken element.
     *
     * @param entity     The entity to render.
     * @param pageNumber The page number on which to render.
     * @return {@code true} if rendering was successful.
     * @throws RenderGuideLinesException if a rendering error occurs.
     */
    boolean paddingRenderMiddle(@NonNull Entity entity, int pageNumber) throws RenderGuideLinesException;

    /**
     * Renders the middle part of a margin guide for a broken element.
     *
     * @param entity     The entity to render.
     * @param pageNumber The page number on which to render.
     * @return {@code true} if rendering was successful.
     * @throws RenderGuideLinesException if a rendering error occurs.
     */
    boolean marginRenderMiddle(@NonNull Entity entity, int pageNumber) throws RenderGuideLinesException;

    /**
     * Renders the final part of a margin guide for a broken element.
     * This typically excludes the top border.
     *
     * @param entity     The entity to render.
     * @param pageNumber The page number on which to render.
     * @return {@code true} if rendering was successful.
     * @throws RenderGuideLinesException if a rendering error occurs.
     */
    boolean marginRenderEnd(@NonNull Entity entity, int pageNumber) throws RenderGuideLinesException;

    /**
     * Renders the final part of a padding guide for a broken element.
     *
     * @param entity     The entity to render.
     * @param pageNumber The page number on which to render.
     * @return {@code true} if rendering was successful.
     * @throws RenderGuideLinesException if a rendering error occurs.
     */
    boolean paddingRenderEnd(@NonNull Entity entity, int pageNumber) throws RenderGuideLinesException;

    /**
     * Renders the final part of a bounding box for a broken element.
     *
     * @param entity     The entity to render.
     * @param pageNumber The page number on which to render.
     * @return {@code true} if rendering was successful.
     * @throws RenderGuideLinesException if a rendering error occurs.
     */
    boolean boxRenderEnd(@NonNull Entity entity, int pageNumber) throws RenderGuideLinesException;

    /**
     * Renders the starting part of a bounding box for a broken element.
     * This typically excludes the bottom border.
     *
     * @param entity     The entity to render.
     * @param pageNumber The page number on which to render.
     * @return {@code true} if rendering was successful.
     * @throws RenderGuideLinesException if a rendering error occurs.
     */
    boolean boxRenderStart(@NonNull Entity entity, int pageNumber) throws RenderGuideLinesException;

    /**
     * Renders the starting part of a padding guide for a broken element.
     *
     * @param entity     The entity to render.
     * @param pageNumber The page number on which to render.
     * @return {@code true} if rendering was successful.
     * @throws RenderGuideLinesException if a rendering error occurs.
     */
    boolean paddingRenderStart(@NonNull Entity entity, int pageNumber) throws RenderGuideLinesException;

    /**
     * Renders the starting part of a margin guide for a broken element.
     *
     * @param entity     The entity to render.
     * @param pageNumber The page number on which to render.
     * @return {@code true} if rendering was successful.
     * @throws RenderGuideLinesException if a rendering error occurs.
     */
    boolean marginRenderStart(@NonNull Entity entity, int pageNumber) throws RenderGuideLinesException;

    /**
     * Renders the complete bounding box for an element that is not broken.
     *
     * @param e The entity to render.
     * @return {@code true} if rendering was successful.
     * @throws RenderGuideLinesException if a rendering error occurs.
     */
    boolean boxRender(@NonNull Entity e) throws RenderGuideLinesException;

    /**
     * Renders the complete padding guide for an element that is not broken.
     *
     * @param e The entity to render.
     * @return {@code true} if rendering was successful.
     * @throws RenderGuideLinesException if a rendering error occurs.
     */
    boolean paddingRender(@NonNull Entity e) throws RenderGuideLinesException;

    /**
     * Renders the complete margin guide for an element that is not broken.
     *
     * @param e The entity to render.
     * @return {@code true} if rendering was successful.
     * @throws RenderGuideLinesException if a rendering error occurs.
     */
    boolean marginRender(@NonNull Entity e) throws RenderGuideLinesException;

    /**
     * Enum representing the different types of visual guides that can be rendered.
     */
    enum Guide {
        /**
         * Represents the margin area of an element.
         */
        MARGIN,
        /**
         * Represents the padding area of an element.
         */
        PADDING,
        /**
         * Represents the bounding box (content area) of an element.
         */
        BOX
    }
}
