package com.demcha.system.interfaces.guides.impl;

import com.demcha.components.core.Entity;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.system.interfaces.RenderingSystemECS;
import com.demcha.system.interfaces.guides.BoxRender;
import lombok.NonNull;

import java.io.IOException;

public record BoxRenderImpl<T extends AutoCloseable>(RenderingSystemECS<T> renderingSystem) implements BoxRender<T> {
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


}
