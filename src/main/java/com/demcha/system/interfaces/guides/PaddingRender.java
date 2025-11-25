package com.demcha.system.interfaces.guides;

import com.demcha.components.content.shape.Stroke;
import com.demcha.components.core.Entity;
import com.demcha.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.components.style.Padding;

import java.awt.*;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * An interface for rendering a padding guide for an entity.
 *
 * @param <T> The type of the stream used for rendering.
 */
public interface PaddingRender<T extends  AutoCloseable> extends GuideCoordinate<T> {

    /**
     * Creates a {@link RenderCoordinateContext} for the padding area of an entity.
     * <p>
     * Note: This default implementation relies on {@code resolveCoordinateContext} and {@code renderingSystemECS} methods
     * which are expected to be provided by the implementing class.
     *
     * @param entity The entity for which to create the context.
     * @return An {@link Optional} containing the {@link RenderCoordinateContext} for the padding, or an empty optional if it cannot be resolved.
     */
    default Optional<RenderCoordinateContext> padding(Entity entity) {
        RenderCoordinateContext coordinateContext;
        Optional<RenderCoordinateContext> coordinateOpt =
                resolveCoordinateContext(entity, Padding.class, Padding::zero);

      return coordinateOpt;

    }

    // The following methods are required by the default 'padding' method.
    // They must be implemented by any class that implements this interface.

    default <C extends Padding> Optional<RenderCoordinateContext> resolveCoordinateContext(Entity entity, Class<C> componentClass, Supplier<C> zero) {
        if (!renderingSystem().guidLineSettings().showOnlySetGuide()) {
            return Optional.empty();
        }

        // Берём компонент или default (Margin.zero() / Padding.zero())
        C context = entity.getComponent(componentClass)
                .orElseGet(zero);

        return context.renderCoordinate(entity, renderingSystem());
    }


}
