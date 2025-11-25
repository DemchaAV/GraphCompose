package com.demcha.system.interfaces.guides.pdf_guides;

import com.demcha.components.core.Entity;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.system.interfaces.RenderingSystemECS;
import com.demcha.system.interfaces.guides.BoxRender;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

@AllArgsConstructor
@Accessors(fluent = true)
@Getter
public class PdfBoxRender<T extends AutoCloseable> implements BoxRender<T> {
    private final RenderingSystemECS<T> renderingSystem;

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
            return renderingSystem().renderRectangle( stream,context, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


}
