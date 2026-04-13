package com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.handlers;

import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.renderable.PageBreakComponent;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import com.demcha.compose.layout_core.system.rendering.RenderHandler;

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
