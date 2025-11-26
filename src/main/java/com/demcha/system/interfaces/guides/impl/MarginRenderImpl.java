package com.demcha.system.interfaces.guides.impl;

import com.demcha.components.core.Entity;
import com.demcha.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.system.interfaces.RenderingSystemECS;
import com.demcha.system.interfaces.guides.MarginRender;
import lombok.NonNull;

import java.io.IOException;
import java.util.Optional;

/**
 * @param renderingSystem
 */
public record MarginRenderImpl<T extends AutoCloseable>(
        RenderingSystemECS<T> renderingSystem) implements MarginRender<T> {
    /**
     * @param entity
     * @param stream
     * @return
     */
    @Override
    public boolean fromStream(@NonNull Entity entity, T stream) {
        Optional<RenderCoordinateContext> marginOpt = margin(entity);
        if (marginOpt.isPresent()) {
            try {
                var margin = marginOpt.get();
                renderingSystem().renderRectangle(stream, margin, true);
                markers(stream, margin);
                return true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return false;

    }


}
