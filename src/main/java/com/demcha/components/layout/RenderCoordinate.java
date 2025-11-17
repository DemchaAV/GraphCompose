package com.demcha.components.layout;

import com.demcha.components.core.Entity;
import com.demcha.components.layout.coordinator.RenderCoordinateContext;

import java.util.Optional;

public interface RenderCoordinate {
    Optional<RenderCoordinateContext> renderCoordinate(Entity entity);
}
