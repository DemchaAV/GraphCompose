package com.demcha.components.renderable;

import com.demcha.components.containers.abstract_builders.GuidesRenderer;
import com.demcha.components.core.Entity;
import com.demcha.system.RenderingSystemECS;
import com.demcha.system.pdf_systems.PdfRenderingSystemECS;
import lombok.ToString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.EnumSet;

@ToString
public class HContainer extends Container {

    private static final EnumSet<GuidesRenderer.Guide> DEFAULT_GUIDES =
            EnumSet.of(GuidesRenderer.Guide.MARGIN, GuidesRenderer.Guide.PADDING, GuidesRenderer.Guide.BOX);


    @Override
    public boolean pdf(Entity e, PdfRenderingSystemECS renderingSystemECS, boolean guideLines) throws IOException {
        try (PDPageContentStream pdPageContentStream = renderingSystemECS.getStream().openContentStream(e)) {
            if (guideLines) renderingSystemECS.renderGuides(e,pdPageContentStream, DEFAULT_GUIDES);
        }
        return true;
    }
}
