package com.demcha.compose.loyaut_core.system.interfaces.guides.impl;

import com.demcha.compose.loyaut_core.components.core.Entity;
import com.demcha.compose.loyaut_core.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.compose.loyaut_core.system.interfaces.RenderingSystemECS;
import com.demcha.compose.loyaut_core.system.interfaces.guides.PaddingRender;
import lombok.Data;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 *
 */
@Accessors(fluent = true)
@Data
public final class PaddingRenderImpl<T extends AutoCloseable> implements PaddingRender<T> {
    @ToString.Exclude
    private final RenderingSystemECS<T> renderingSystem;

    /**
     * @param renderingSystem
     */
    public PaddingRenderImpl(
            RenderingSystemECS<T> renderingSystem) {
        this.renderingSystem = renderingSystem;
    }

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

    @Override
    public RenderingSystemECS<T> renderingSystem() {
        return renderingSystem;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (PaddingRenderImpl) obj;
        return Objects.equals(this.renderingSystem, that.renderingSystem);
    }

    @Override
    public int hashCode() {
        return Objects.hash(renderingSystem);
    }

    @Override
    public String toString() {
        return "PaddingRenderImpl[" +
               "renderingSystem=" + renderingSystem + ']';
    }


}
