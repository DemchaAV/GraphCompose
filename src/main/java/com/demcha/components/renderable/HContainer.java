package com.demcha.components.renderable;

import com.demcha.components.core.Entity;
import com.demcha.system.RenderingSystemECS;
import lombok.ToString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.EnumSet;

@ToString
public class HContainer extends Container {

    private static final EnumSet<Guide> DEFAULT_GUIDES =
            EnumSet.of(Guide.MARGIN, Guide.PADDING, Guide.BOX);


    @Override
    public boolean pdfRender(Entity e, PDDocument doc, RenderingSystemECS renderingSystemECS, boolean guideLines) throws IOException {
        try (PDPageContentStream pdPageContentStream = openContentStream(e, doc, renderingSystemECS)) {
            if (guideLines) renderGuides(e,pdPageContentStream, DEFAULT_GUIDES);
        }
        return true;
    }
}
