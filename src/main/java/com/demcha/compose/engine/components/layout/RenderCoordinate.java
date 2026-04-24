package com.demcha.compose.engine.components.layout;

import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.compose.engine.render.RenderingSystemECS;

import java.util.Optional;

public interface RenderCoordinate {
    <S extends AutoCloseable> Optional<RenderCoordinateContext> renderCoordinate(Entity entity, RenderingSystemECS<S> renderingSystem);
}
