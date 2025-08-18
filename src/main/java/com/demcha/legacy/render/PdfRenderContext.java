package com.demcha.legacy.render;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.io.IOException;

/**
 * Контекст рендера для PDF.
 * Держит в себе текущий документ, поток рисования и размеры страницы.
 * Используется в рендерерах элементов.
 */
public class PdfRenderContext implements AutoCloseable {
    private final PDDocument document;
    private final PDPage page;
    private final PDRectangle pageSize;
    private final PDPageContentStream contentStream;

    public PdfRenderContext(PDDocument document, PDPage page, PDPageContentStream contentStream) {
        this.document = document;
        this.document.addPage(page);
        this.page = page;
        this.pageSize = page.getMediaBox();
        this.contentStream = contentStream;
    }
    public PdfRenderContext(PDDocument document, PDPage page) throws IOException {
     this(document, page, new PDPageContentStream(document,page));
    }

    public PDDocument getDocument() {
        return document;
    }

    public PDPage getPage() {
        return page;
    }

    public PDRectangle getPageSize() {
        return pageSize;
    }

    public PDPageContentStream getContentStream() {
        return contentStream;
    }

    @Override
    public void close() throws Exception {
        if (contentStream != null) {
            contentStream.close();
        }
    }

    public float getPageWidth() {
        return pageSize.getWidth();
    }

    public float getPageHeight() {
        return pageSize.getHeight();
    }
}

