package com.demcha.system.interfaces.guides.pdf_guides;

import com.demcha.components.core.Entity;
import com.demcha.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.components.style.Margin;
import com.demcha.system.interfaces.RenderingSystemECS;
import com.demcha.system.interfaces.guides.MarginRender;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.util.Optional;

@AllArgsConstructor
@Accessors(fluent = true)
@Getter
public class PdfMarginRender<T extends AutoCloseable> implements MarginRender<T> {
    /**
     * @return
     */
    private final RenderingSystemECS<T> renderingSystem;



    /**
     * @param entity
     * @param stream
     * @return
     */
    @Override
    public boolean fromStream(@NonNull Entity entity, T stream) {
        Optional<RenderCoordinateContext> margin = margin(entity);
        if (margin.isPresent()){
            try {
                return renderingSystem().renderRectangle(stream,margin.get(), true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return false;

    }


}
