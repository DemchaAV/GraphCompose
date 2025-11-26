package com.demcha.system.implemented_systems.pdf_systems;

import com.demcha.components.components_builders.Canvas;
import com.demcha.components.core.Entity;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.system.interfaces.RenderStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.awt.*;
import java.io.IOException;

@Slf4j
public record PdfStream(PDDocument doc, Canvas canvas) implements RenderStream<PDPageContentStream> {


    @Override
    public PDPageContentStream openContentStream(int pageIndex) throws IOException {
        int numberOfPages = doc.getNumberOfPages();
        if (numberOfPages - 1 < pageIndex) {
            log.info("Page index is: {} available page is 0 to {} create a Page in the document", pageIndex, numberOfPages - 1);
            for (int i = numberOfPages - 1; i < pageIndex; i++) {
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

    public PDPageContentStream reopenContentStreamForTextData(PDPageContentStream cs, int currentPage, PDFont font, float fontSize, Color color) throws IOException {
        if (cs != null) {
            cs.endText();
            cs.restoreGraphicsState();
            cs.close();
        }
        return openContentSteamForTextData(currentPage, font, fontSize, color);
    }

    public PDPageContentStream openContentSteamForTextData(int currentPage, PDFont font, float fontSize, Color color) throws IOException {
        PDPageContentStream newStream = openContentStream(currentPage);
        newStream.saveGraphicsState();
        newStream.setFont(font, fontSize);
        newStream.setNonStrokingColor(color);
        newStream.beginText();
        return newStream;
    }
}
