package com.demcha.system.interfaces.guides.impl;

import com.demcha.components.core.Entity;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.system.interfaces.RenderingSystemECS;
import com.demcha.system.interfaces.guides.BoxRender;
import lombok.Data;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.util.Objects;

@Accessors(fluent = true)
@Data
public final class BoxRenderImpl<T extends AutoCloseable> implements BoxRender<T> {
    @ToString.Exclude
    private final RenderingSystemECS<T> renderingSystem;

    public BoxRenderImpl(RenderingSystemECS<T> renderingSystem) {
        this.renderingSystem = renderingSystem;
    }

    /**
     * @param entity
     * @param stream
     * @return
     */
    @Override
    public boolean fromStream(@NonNull Entity entity, T stream) {
        Placement placement = entity.getComponent(Placement.class).orElseThrow();
        var color = renderingSystem().guidLineSettings().BOX_COLOR();
        var stroke = renderingSystem().guidLineSettings().BOX_STROKE();
        RenderCoordinateContext context = new RenderCoordinateContext(placement.x(), placement.y(), placement.width(), placement.height(), placement.startPage(), placement.endPage(), stroke, color);

        try {
            return renderingSystem().renderRectangle(stream, context, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public RenderingSystemECS<T> renderingSystem() {
        return renderingSystem;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BoxRenderImpl) obj;
        return Objects.equals(this.renderingSystem, that.renderingSystem);
    }

    @Override
    public int hashCode() {
        return Objects.hash(renderingSystem);
    }

    @Override
    public String toString() {
        return "BoxRenderImpl[" +
               "renderingSystem=" + renderingSystem + ']';
    }


}
