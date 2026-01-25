package com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems;

import com.demcha.compose.loyaut_core.components.core.Entity;
import com.demcha.compose.loyaut_core.core.EntityManager;
import com.demcha.compose.loyaut_core.exceptions.RenderGuideLinesException;
import com.demcha.compose.loyaut_core.system.interfaces.Render;

import java.io.IOException;

public interface PdfRender extends Render {

    boolean pdf(EntityManager manager, Entity e, PdfRenderingSystemECS pdfRenderingSystem, boolean guideLines) throws IOException, RenderGuideLinesException;
}
