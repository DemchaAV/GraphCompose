package com.demcha.compose.layout_core.system.interfaces.guides;

import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.compose.layout_core.components.style.Margin;

import java.util.Optional;
import java.util.function.Supplier;

public interface MarginRender<T extends AutoCloseable> extends GuideCoordinate<T> {

    default Optional<RenderCoordinateContext> margin(Entity entity) {
        Optional<RenderCoordinateContext> coordinateOpt =
                resolveCoordinateContext(entity, Margin.class, Margin::zero);
        return coordinateOpt;
    }

    default <C extends Margin> Optional<RenderCoordinateContext> resolveCoordinateContext(Entity entity, Class<C> componentClass, Supplier<C> zero) {
        if (!renderingSystem().guidLineSettings().showOnlySetGuide()) {
            return Optional.empty();
        }

        C context = entity.getComponent(componentClass)
                .orElseGet(zero);

        return context.renderCoordinate(entity, renderingSystem());
    }


}
