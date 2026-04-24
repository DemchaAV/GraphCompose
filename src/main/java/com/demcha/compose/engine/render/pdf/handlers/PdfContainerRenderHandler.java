package com.demcha.compose.engine.render.pdf.handlers;

import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.renderable.Container;
import com.demcha.compose.engine.core.EntityManager;
import com.demcha.compose.engine.render.pdf.PdfRenderingSystemECS;
import com.demcha.compose.engine.render.RenderHandler;

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
