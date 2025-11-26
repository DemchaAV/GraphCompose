package com.demcha.system.interfaces.guides.impl;

import com.demcha.components.core.Entity;
import com.demcha.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.system.interfaces.RenderingSystemECS;
import com.demcha.system.interfaces.guides.PaddingRender;
import lombok.NonNull;

import java.io.IOException;
import java.util.Optional;

/**
 * @param renderingSystem
 */

public record PaddingRenderImpl<T extends AutoCloseable>(
        RenderingSystemECS<T> renderingSystem) implements PaddingRender<T> {
    /**
     * @param entity
     * @param stream
     * @return
     */
    @Override
    public boolean fromStream(@NonNull Entity entity, T stream) {
        Optional<RenderCoordinateContext> padding = padding(entity);
        if (padding.isPresent()) {
            try {
                renderingSystem().renderRectangle(stream, padding.get(), true);
                markers(stream, padding.get());
                return true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }


}
