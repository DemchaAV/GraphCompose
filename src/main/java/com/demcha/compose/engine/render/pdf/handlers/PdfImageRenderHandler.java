package com.demcha.compose.engine.render.pdf.handlers;

import com.demcha.compose.engine.components.content.ImageData;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.layout.coordinator.Placement;
import com.demcha.compose.engine.components.renderable.ImageComponent;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.engine.core.EntityManager;
import com.demcha.compose.engine.render.pdf.PdfRenderingSystemECS;
import com.demcha.compose.engine.render.guides.GuidesRenderer;
import com.demcha.compose.engine.render.RenderHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;
import java.util.EnumSet;

@Slf4j
public final class PdfImageRenderHandler implements RenderHandler<ImageComponent, PdfRenderingSystemECS> {
    private static final EnumSet<GuidesRenderer.Guide> DEFAULT_GUIDES =
            EnumSet.of(GuidesRenderer.Guide.MARGIN, GuidesRenderer.Guide.PADDING, GuidesRenderer.Guide.BOX);

    @Override
    public Class<ImageComponent> renderType() {
        return ImageComponent.class;
    }

    @Override
    public boolean render(EntityManager manager,
                          Entity entity,
                          ImageComponent renderComponent,
                          PdfRenderingSystemECS renderingSystem,
                          boolean guideLines) throws IOException {
        ImageData imageData = entity.getComponent(ImageData.class).orElse(null);
        Placement placement = entity.getComponent(Placement.class).orElse(null);

        if (imageData == null || placement == null) {
            log.warn("Skipping image render because ImageData or Placement is missing for {}", entity);
            return false;
        }

        Padding padding = entity.getComponent(Padding.class).orElse(Padding.zero());
        double x = placement.x() + padding.left();
        double y = placement.y() + padding.bottom();
        double width = Math.max(0.0, placement.width() - padding.horizontal());
        double height = Math.max(0.0, placement.height() - padding.vertical());

        PDPageContentStream stream = renderingSystem.pageSurface(entity);
        if (width > 0.0 && height > 0.0) {
            PDImageXObject image = renderingSystem.getOrCreateImageXObject(imageData, width, height);
            stream.drawImage(image, (float) x, (float) y, (float) width, (float) height);
        }

        if (guideLines) {
            renderingSystem.guidesRenderer().guidesRender(entity, stream, DEFAULT_GUIDES);
        }

        return width > 0.0 && height > 0.0;
    }
}
