package com.demcha.system.pdf_systems;

import com.demcha.components.core.Entity;
import com.demcha.system.Render;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

public interface PdfRender extends Render {
    boolean pdfRender(Entity e, PDPageContentStream cs, PDDocument doc, int indexPage, boolean guideLines) throws IOException;
}
       