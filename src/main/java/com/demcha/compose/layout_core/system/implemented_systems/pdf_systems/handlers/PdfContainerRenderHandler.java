package com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.handlers;

import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.renderable.Container;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import com.demcha.compose.layout_core.system.rendering.RenderHandler;

import java.io.IOException;

public final class PdfContainerRenderHandler implements RenderHandler<Container, PdfRenderingSystemECS> {

    @Override
    public Class<Container> renderType() {
        return Container.class;
    }

    @Override
    public boolean render(EntityManager manager,
                          Entity entity,
                          Container renderComponent,
                          PdfRenderingSystemECS renderingSystem,
                          boolean guideLines) throws IOException {
        if (guideLines) {
            return renderingSystem.guidesRenderer().guidesRender(entity, Container.DEFAULT_GUIDES);
        }
        return false;
    }
}
