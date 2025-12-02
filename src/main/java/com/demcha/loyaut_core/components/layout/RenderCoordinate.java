package com.demcha.loyaut_core.components.layout;

import com.demcha.loyaut_core.components.core.Entity;
import com.demcha.loyaut_core.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.loyaut_core.system.interfaces.RenderingSystemECS;

import java.util.Optional;

public interface RenderCoordinate {
    <S extends AutoCloseable> Optional<RenderCoordinateContext> renderCoordinate(Entity entity, RenderingSystemECS<S> renderingSystem);
}
