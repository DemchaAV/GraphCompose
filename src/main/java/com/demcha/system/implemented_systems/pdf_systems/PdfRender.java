package com.demcha.system.implemented_systems.pdf_systems;

import com.demcha.components.core.Entity;
import com.demcha.system.interfaces.Render;

import java.io.IOException;

public interface PdfRender extends Render {

    boolean pdf(Entity e, PdfRenderingSystemECS pdfRenderingSystem, boolean guideLines) throws IOException;
}
