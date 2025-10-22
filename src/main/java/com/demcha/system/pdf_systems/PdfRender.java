package com.demcha.system.pdf_systems;

import com.demcha.components.core.Entity;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.system.Render;
import com.demcha.system.RenderingSystemECS;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.io.IOException;

public interface PdfRender extends Render {
    boolean pdfRender(Entity e, PDDocument doc,  RenderingSystemECS renderingSystem, boolean guideLines) throws IOException;

    default PDPageContentStream openContentStream(PDDocument doc, RenderingSystemECS renderingSystem, int pageIndex) throws IOException {
        int numberOfPages = doc.getNumberOfPages();
        if (numberOfPages - 1 < pageIndex) {
            for (int i = 0; i < pageIndex + 1; i++) {
                var canvas = renderingSystem.getCanvas();
                doc.addPage(new PDPage(new PDRectangle(canvas.x(), canvas.y(), (float) canvas.width(), (float) canvas.height())));
            }
        }
        return new PDPageContentStream(
                doc, doc.getPage(pageIndex),
                PDPageContentStream.AppendMode.APPEND,   // keep existing content if any
                true,                                    // compress
                true                                     // resetContext: isolates graphics state (PDFBox 3)
        );
    }

    default PDPageContentStream openContentStream(Entity entity, PDDocument doc, RenderingSystemECS renderingSystem) throws IOException {
        int pageIndex = entity.getComponent(Placement.class).orElseThrow().startPage();

        return openContentStream(doc, renderingSystem, pageIndex);
    }
}
       