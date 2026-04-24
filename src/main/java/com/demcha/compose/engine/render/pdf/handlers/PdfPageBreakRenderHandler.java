package com.demcha.compose.engine.render.pdf.handlers;

import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.renderable.PageBreakComponent;
import com.demcha.compose.engine.core.EntityManager;
import com.demcha.compose.engine.render.pdf.PdfRenderingSystemECS;
import com.demcha.compose.engine.render.RenderHandler;

import java.io.IOException;

/**
 * No-op PDF handler for explicit page-break markers.
 *
 * <p>The pagination system consumes the semantic meaning of a page break during
 * layout. By render time there is nothing visible left to draw, but the entity
 * still carries a render marker and therefore needs a registered handler in the
 * PDF backend.</p>
 */
public final class PdfPageBreakRenderHandler implements RenderHandler<PageBreakComponent, PdfRenderingSystemECS> {

    @Override
    public Class<PageBreakComponent> renderType() {
        return PageBreakComponent.class;
    }

    @Override
    public boolean render(EntityManager manager,
                          Entity entity,
                          PageBreakComponent renderComponent,
                          PdfRenderingSystemECS renderingSystem,
                          boolean guideLines) throws IOException {
        return true;
    }
}
