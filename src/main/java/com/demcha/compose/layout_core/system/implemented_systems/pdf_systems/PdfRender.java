package com.demcha.compose.layout_core.system.implemented_systems.pdf_systems;

import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.exceptions.RenderGuideLinesException;
import com.demcha.compose.layout_core.system.interfaces.Render;

import java.io.IOException;

public interface PdfRender extends Render {

    boolean pdf(EntityManager manager, Entity e, PdfRenderingSystemECS pdfRenderingSystem, boolean guideLines) throws IOException, RenderGuideLinesException;
}
