package com.demcha.components.renderable;

import com.demcha.components.core.Entity;
import com.demcha.system.RenderingSystemECS;
import lombok.Data;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.EnumSet;

@Data
public class VContainer extends Container {

    private static final EnumSet<Guide> DEFAULT_GUIDES =
            EnumSet.of(Guide.MARGIN, Guide.PADDING, Guide.BOX);


    @Override
    public boolean pdfRender(Entity e, PDDocument doc, RenderingSystemECS renderingSystemECS, boolean guideLines) throws IOException {
        try (PDPageContentStream cs = openContentStream(e,doc, renderingSystemECS)) {
            if (guideLines) {
                renderGuides(e, cs, DEFAULT_GUIDES);
            }
        }
        return true;
    }
}
