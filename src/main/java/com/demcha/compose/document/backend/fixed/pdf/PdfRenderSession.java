package com.demcha.compose.document.backend.fixed.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Page-scoped PDF render session that reuses one content stream per page.
 *
 * <p>The session owns all page surfaces opened during one document render pass.
 * Handlers may mutate graphics or text state temporarily, but they must restore
 * the state they change before returning so subsequent handlers can safely
 * continue drawing on the same page surface.</p>
 *
 * <p><b>Thread-safety:</b> mutable and not thread-safe.</p>
 */
final class PdfRenderSession implements AutoCloseable {
    private final PDDocument document;
    private final List<PDPage> pages;
    private final Map<Integer, PDPageContentStream> pageSurfaces = new LinkedHashMap<>();
    private boolean closed;

    PdfRenderSession(PDDocument document, List<PDPage> pages) {
        this.document = document;
        this.pages = List.copyOf(pages);
    }

    PDPageContentStream pageSurface(int pageIndex) throws IOException {
        if (closed) {
            throw new IllegalStateException("PdfRenderSession is already closed.");
        }
        if (pageIndex < 0 || pageIndex >= pages.size()) {
            throw new IllegalArgumentException("Page index " + pageIndex + " is outside the render session.");
        }
        PDPageContentStream existing = pageSurfaces.get(pageIndex);
        if (existing != null) {
            return existing;
        }

        PDPageContentStream opened = new PDPageContentStream(document, pages.get(pageIndex));
        pageSurfaces.put(pageIndex, opened);
        return opened;
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        IOException failure = null;
        for (PDPageContentStream stream : pageSurfaces.values()) {
            try {
                stream.close();
            } catch (IOException ex) {
                if (failure == null) {
                    failure = ex;
                } else {
                    failure.addSuppressed(ex);
                }
            }
        }
        pageSurfaces.clear();
        closed = true;
        if (failure != null) {
            throw failure;
        }
    }
}
