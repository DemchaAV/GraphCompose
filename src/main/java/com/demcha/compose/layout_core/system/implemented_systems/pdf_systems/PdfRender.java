package com.demcha.compose.layout_core.system.implemented_systems.pdf_systems;

import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.exceptions.RenderGuideLinesException;
import com.demcha.compose.layout_core.system.interfaces.Render;

import java.io.IOException;

/**
 * Legacy PDF render contract.
 * <p>
 * New renderer work should prefer renderer-side handlers registered through the
 * active rendering system. This interface remains as a migration fallback while
 * older renderables are still being moved out of engine components.
 * </p>
 */
public interface PdfRender extends Render {

    boolean pdf(EntityManager manager, Entity e, PdfRenderingSystemECS pdfRenderingSystem, boolean guideLines) throws IOException, RenderGuideLinesException;
}
