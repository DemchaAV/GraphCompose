package com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.handlers;

import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.renderable.Element;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import com.demcha.compose.layout_core.system.interfaces.guides.GuidesRenderer;
import com.demcha.compose.layout_core.system.rendering.RenderHandler;

import java.io.IOException;
import java.util.EnumSet;

public final class PdfElementRenderHandler implements RenderHandler<Element, PdfRenderingSystemECS> {
    private static final EnumSet<GuidesRenderer.Guide> DEFAULT_GUIDES =
            EnumSet.of(GuidesRenderer.Guide.MARGIN, GuidesRenderer.Guide.PADDING, GuidesRenderer.Guide.BOX);

    @Override
    public Class<Element> renderType() {
        return Element.class;
    }

    @Override
    public boolean render(EntityManager manager,
                          Entity entity,
                          Element renderComponent,
                          PdfRenderingSystemECS renderingSystem,
                          boolean guideLines) throws IOException {
        if (guideLines) {
            renderingSystem.guidesRenderer().guidesRender(entity, DEFAULT_GUIDES);
        }
        return true;
    }
}
