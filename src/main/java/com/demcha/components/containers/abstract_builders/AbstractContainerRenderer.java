package com.demcha.components.containers.abstract_builders;



import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.layout.GuidesRenderer;
import com.demcha.system.PdfRender;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.EnumSet;

/**
 * An abstract base class for container rendering components.
 * It provides a default implementation for rendering guides.
 */
public abstract class AbstractContainerRenderer implements Component, PdfRender, GuidesRenderer {

    // Common constant for all subclasses
    protected static final EnumSet<Guide> DEFAULT_GUIDES =
            EnumSet.of(Guide.MARGIN, Guide.PADDING, Guide.BOX);

    // Common render method for all subclasses
    @Override
    public boolean render(Entity e, PDPageContentStream cs, boolean guideLines) throws IOException {
        if (guideLines) {
            renderGuides(e, cs, DEFAULT_GUIDES);
        }
        return true;
    }
}