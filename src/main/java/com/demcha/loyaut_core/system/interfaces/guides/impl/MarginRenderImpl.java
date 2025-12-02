package com.demcha.loyaut_core.system.interfaces.guides.impl;

import com.demcha.loyaut_core.components.core.Entity;
import com.demcha.loyaut_core.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.loyaut_core.system.interfaces.RenderingSystemECS;
import com.demcha.loyaut_core.system.interfaces.guides.MarginRender;
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
public final class MarginRenderImpl<T extends AutoCloseable> implements MarginRender<T> {
   @ToString.Exclude
    private final RenderingSystemECS<T> renderingSystem;

    /**
     * @param renderingSystem
     */
    public MarginRenderImpl(
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

    @Override
    public RenderingSystemECS<T> renderingSystem() {
        return renderingSystem;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (MarginRenderImpl) obj;
        return Objects.equals(this.renderingSystem, that.renderingSystem);
    }

    @Override
    public int hashCode() {
        return Objects.hash(renderingSystem);
    }

    @Override
    public String toString() {
        return "MarginRenderImpl[" +
               "renderingSystem=" + renderingSystem + ']';
    }


}
