package com.demcha.compose.loyaut_core.components.layout;

import com.demcha.compose.loyaut_core.components.core.Entity;
import com.demcha.compose.loyaut_core.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.compose.loyaut_core.system.interfaces.RenderingSystemECS;

import java.util.Optional;

public interface RenderCoordinate {
    <S extends AutoCloseable> Optional<RenderCoordinateContext> renderCoordinate(Entity entity, RenderingSystemECS<S> renderingSystem);
}
