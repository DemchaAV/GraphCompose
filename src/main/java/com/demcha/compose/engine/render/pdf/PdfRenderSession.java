package com.demcha.compose.engine.render.pdf;

import com.demcha.compose.engine.render.RenderPassSession;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PDF render-pass session that reuses one content stream per page.
 *
 * <p>One {@link PdfRenderSession} exists for one renderer {@code process(...)}
 * invocation. Handlers obtain session-owned page surfaces from here and must
 * restore any graphics/text state they change before returning.</p>
 */
final class PdfRenderSession implements RenderPassSession<PDPageContentStream> {
    private final PdfStream streamFactory;
    private final Map<Integer, PDPageContentStream> pageSurfaces = new LinkedHashMap<>();
    private boolean closed;

    PdfRenderSession(PdfStream streamFactory) {
        this.streamFactory = streamFactory;
    }

    @Override
    public void ensurePage(int pageIndex) throws IOException {
        assertOpen();
        streamFactory.ensurePage(pageIndex);
    }

    @Override
    public PDPageContentStream pageSurface(int pageIndex) throws IOException {
        assertOpen();
        PDPageContentStream surface = pageSurfaces.get(pageIndex);
        if (surface != null) {
            return surface;
        }

        ensurePage(pageIndex);
        PDPageContentStream opened = streamFactory.openPageSurface(pageIndex);
        pageSurfaces.put(pageIndex, opened);
        return opened;
    }

    /**
     * Returns the number of page surfaces currently cached by this session.
     * Visible for testing only.
     */
    int cachedPageCount() {
        return pageSurfaces.size();
    }

    /**
     * Returns whether this session has been closed. After closing, no further
     * page surfaces can be obtained. Visible for testing only.
     */
    boolean isClosed() {
        return closed;
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        IOException failure = null;
        for (PDPageContentStream surface : pageSurfaces.values()) {
            try {
                surface.close();
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

    private void assertOpen() {
        if (closed) {
            throw new IllegalStateException("PdfRenderSession is already closed");
        }
    }
}
