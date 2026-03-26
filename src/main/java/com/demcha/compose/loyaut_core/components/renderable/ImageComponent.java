package com.demcha.compose.loyaut_core.components.renderable;

import com.demcha.compose.loyaut_core.components.content.ImageData;
import com.demcha.compose.loyaut_core.components.core.Entity;
import com.demcha.compose.loyaut_core.components.layout.coordinator.Placement;
import com.demcha.compose.loyaut_core.components.style.Padding;
import com.demcha.compose.loyaut_core.core.EntityManager;
import com.demcha.compose.loyaut_core.system.interfaces.guides.GuidesRenderer;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfRender;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;
import java.util.EnumSet;

@Slf4j
@EqualsAndHashCode
@NoArgsConstructor
public class ImageComponent implements PdfRender {
    private static final EnumSet<GuidesRenderer.Guide> DEFAULT_GUIDES =
            EnumSet.of(GuidesRenderer.Guide.MARGIN, GuidesRenderer.Guide.PADDING, GuidesRenderer.Guide.BOX);

    @Override
    public boolean pdf(EntityManager manager, Entity e, PdfRenderingSystemECS renderingSystemECS, boolean guideLines) throws IOException {
        ImageData imageData = e.getComponent(ImageData.class).orElse(null);
        Placement placement = e.getComponent(Placement.class).orElse(null);

        if (imageData == null || placement == null) {
            log.warn("Skipping image render because ImageData or Placement is missing for {}", e);
            return false;
        }

        Padding padding = e.getComponent(Padding.class).orElse(Padding.zero());
        double x = placement.x() + padding.left();
        double y = placement.y() + padding.bottom();
        double width = Math.max(0.0, placement.width() - padding.horizontal());
        double height = Math.max(0.0, placement.height() - padding.vertical());

        try (PDPageContentStream cs = renderingSystemECS.stream().openContentStream(e)) {
            if (width > 0.0 && height > 0.0) {
                PDImageXObject image = PDImageXObject.createFromByteArray(
                        renderingSystemECS.doc(),
                        imageData.getBytes(),
                        e.getUuid().toString()
                );
                cs.drawImage(image, (float) x, (float) y, (float) width, (float) height);
            }

            if (guideLines) {
                renderingSystemECS.guidesRenderer().guidesRender(e, cs, DEFAULT_GUIDES);
            }
        }

        return width > 0.0 && height > 0.0;
    }
}
