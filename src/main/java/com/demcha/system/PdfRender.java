package com.demcha.system;

import com.demcha.components.core.Entity;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

public interface PdfRender {
    boolean render(Entity e, PDPageContentStream cs) throws IOException;
}
