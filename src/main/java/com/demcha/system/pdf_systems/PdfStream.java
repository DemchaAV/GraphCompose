package com.demcha.system.pdf_systems;

import com.demcha.components.components_builders.Canvas;
import com.demcha.components.core.Entity;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.system.RenderStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.io.IOException;

@Slf4j
public record PdfStream(PDDocument doc, Canvas canvas) implements RenderStream<PDPageContentStream> {
    @Override
    public PDPageContentStream openContentStream(int pageIndex) throws IOException {
        int numberOfPages = doc.getNumberOfPages();
        if (numberOfPages - 1 < pageIndex) {
            for (int i = numberOfPages-1; i < pageIndex ; i++) {
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

    public PDPageContentStream openContentStream(Entity entity) throws IOException {
        int startPageIndex = entity.getComponent(Placement.class).orElseThrow().startPage();
        int endPageIndex = entity.getComponent(Placement.class).orElseThrow().endPage();
        if (endPageIndex != startPageIndex) {
            log.error("{} start page: {}  endPage: {}", entity, startPageIndex, endPageIndex);
            throw new IllegalStateException("endPageIndex should be the same as page startPageIndex");
        }
        return openContentStream(startPageIndex);

    }
}
