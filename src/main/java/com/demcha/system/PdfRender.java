package com.demcha.system;

import com.demcha.components.core.Entity;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

public interface PdfRender extends Render {
    boolean pdfRender(Entity e, PDPageContentStream cs, PDDocument doc, boolean guideLine) throws IOException;
}
