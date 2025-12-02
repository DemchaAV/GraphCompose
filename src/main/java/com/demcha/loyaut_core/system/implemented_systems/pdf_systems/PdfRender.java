package com.demcha.loyaut_core.system.implemented_systems.pdf_systems;

import com.demcha.loyaut_core.components.core.Entity;
import com.demcha.loyaut_core.exceptions.RenderGuideLinesException;
import com.demcha.loyaut_core.system.interfaces.Render;

import java.io.IOException;

public interface PdfRender extends Render {

    boolean pdf(Entity e, PdfRenderingSystemECS pdfRenderingSystem, boolean guideLines) throws IOException, RenderGuideLinesException;
}
