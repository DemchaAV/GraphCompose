package com.demcha.compose.engine.render.pdf;

import com.demcha.compose.engine.core.Canvas;
import com.demcha.compose.engine.render.RenderPassSession;
import com.demcha.compose.engine.render.RenderStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import java.io.IOException;

@Slf4j
public record PdfStream(PDDocument doc, Canvas canvas) implements RenderStream<PDPageContentStream> {

    @Override
    public RenderPassSession<PDPageContentStream> openRenderPass() {
        return new PdfRenderSession(this);
    }

    PDPage ensurePage(int pageIndex) {
        int numberOfPages = doc.getNumberOfPages();
        if (numberOfPages <= pageIndex) {
            log.info("Page index is: {} available pages: {} - creating new page(s)", pageIndex, numberOfPages);
            for (int i = numberOfPages; i <= pageIndex; i++) {
                doc.addPage(new PDPage(
                        new PDRectangle(canvas.x(), canvas.y(), (float) canvas.width(), (float) canvas.height())));
            }
        }
        return doc.getPage(pageIndex);
    }

    PDPageContentStream openPageSurface(int pageIndex) throws IOException {
        ensurePage(pageIndex);
        return new PDPageContentStream(
                doc, doc.getPage(pageIndex),
                PDPageContentStream.AppendMode.APPEND, // keep existing content if any
                true, // compress
                true // resetContext: isolates graphics state (PDFBox 3)
        );
    }
}

