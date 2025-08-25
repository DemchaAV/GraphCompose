package com.demcha.system;

import com.demcha.components.core.Entity;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

public interface PdfEntityRender {
    boolean render( PDPageContentStream cs) throws IOException;
}
