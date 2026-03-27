package com.demcha.compose.layout_core.system.interfaces.guides;

import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.layout.coordinator.Placement;
import com.demcha.compose.layout_core.components.layout.coordinator.RenderCoordinateContext;

import java.awt.*;
import java.util.Optional;

/**
 * An interface for rendering a box guide for an entity.
 *
 * @param <T> The type of the stream used for rendering.
 */
public interface BoxRender<T extends  AutoCloseable> extends GuideCoordinate<T> {

    /**
     * Creates a {@link RenderCoordinateContext} for the bounding box of an entity.
     *
     * @param entity The entity for which to create the context.
     * @return An {@link Optional} containing the {@link RenderCoordinateContext}.
     * @throws java.util.NoSuchElementException if the entity does not have a {@link ContentSize} or {@link Placement} component.
     */
    default Optional<RenderCoordinateContext> box(Entity entity) {
        var boxSize = entity.getComponent(ContentSize.class).orElseThrow();
        var placement = entity.getComponent(Placement.class).orElseThrow();
        double x = placement.x();
        double y = placement.y();
        double width = boxSize.width();
        double height = boxSize.height();
        Color color = renderingSystem().guidLineSettings().BOX_COLOR();
        Stroke stroke = renderingSystem().guidLineSettings().BOX_STROKE();
        return Optional.of(new RenderCoordinateContext(x, y, width, height, placement.startPage(), placement.endPage(), stroke, color));
    }
}
