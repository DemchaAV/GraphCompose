package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.engine.components.content.ImageData;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared per-render-pass state for the canonical PDF backend.
 *
 * <p>The environment owns immutable document-wide dependencies such as the
 * resolved font library and mutable caches such as decoded images. It is
 * created once per {@link PdfFixedLayoutBackend#render} invocation and is not
 * shared across render passes.</p>
 *
 * <p><b>Thread-safety:</b> mutable and confined to one render pass.</p>
 */
public final class PdfRenderEnvironment {
    private final PDDocument document;
    private final FontLibrary fonts;
    private final PdfRenderSession session;
    private final Map<String, PDImageXObject> imageCache = new HashMap<>();
    private final List<BookmarkRecord> bookmarkRecords = new ArrayList<>();

    PdfRenderEnvironment(PDDocument document, FontLibrary fonts, PdfRenderSession session) {
        this.document = document;
        this.fonts = fonts;
        this.session = session;
    }

    /**
     * Returns the live PDFBox document for the current render pass.
     *
     * @return mutable PDFBox document owned by the backend
     */
    public PDDocument document() {
        return document;
    }

    /**
     * Returns the render-pass font library shared by all handlers.
     *
     * @return resolved font library
     */
    public FontLibrary fonts() {
        return fonts;
    }

    /**
     * Returns the page-scoped drawing surface for one resolved page.
     *
     * @param pageIndex zero-based page index
     * @return reusable page content stream owned by the current render session
     * @throws IOException if the page surface cannot be opened
     */
    public PDPageContentStream pageSurface(int pageIndex) throws IOException {
        return session.pageSurface(pageIndex);
    }

    /**
     * Resolves an image XObject through the render-pass image cache.
     *
     * @param imageData semantic image payload
     * @return decoded PDFBox image object shared by matching fragments
     */
    public PDImageXObject resolveImage(ImageData imageData) {
        return imageCache.computeIfAbsent(
                imageData.getFingerprint(),
                ignored -> createImage(document, imageData));
    }

    private PDImageXObject createImage(PDDocument document, ImageData imageData) {
        try {
            return PDImageXObject.createFromByteArray(document, imageData.getBytes(), imageData.getSourceKey());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to decode image '" + imageData.getSourceKey() + "'", e);
        }
    }

    void registerBookmark(PlacedFragment fragment, DocumentBookmarkOptions bookmarkOptions) {
        bookmarkRecords.add(new BookmarkRecord(
                bookmarkOptions.title(),
                bookmarkOptions.level(),
                fragment.pageIndex(),
                fragment.y() + fragment.height()));
    }

    List<BookmarkRecord> bookmarkRecords() {
        return List.copyOf(bookmarkRecords);
    }

    record BookmarkRecord(String title, int level, int pageIndex, double y) {
    }
}
