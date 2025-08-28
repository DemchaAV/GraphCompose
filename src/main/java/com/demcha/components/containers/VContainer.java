package com.demcha.components.containers;

import com.demcha.components.containers.abstract_builders.AbstractContainerRenderer;
import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.layout.GuidesRenderer;
import com.demcha.system.PdfRender;
import lombok.Data;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.EnumSet;

@Data
public class VContainer implements Component, PdfRender, GuidesRenderer {

    private static final EnumSet<Guide> DEFAULT_GUIDES =
            EnumSet.of(Guide.MARGIN, Guide.PADDING, Guide.BOX);


    @Override
    public boolean render(Entity e, PDPageContentStream cs, boolean guideLines) throws IOException {
        if (guideLines) {
            renderGuides(e, cs, DEFAULT_GUIDES);
        }
        return true;
    }
}
