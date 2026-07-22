package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.backend.fixed.pptx.PptxFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.PptxRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.BookmarkMarkerPayload;

/**
 * Dispatch target for bookmark markers; nothing is drawn here — the backend's
 * generic post-render bookkeeping records the bookmark, mirroring the PDF
 * backend's no-op marker handler.
 *
 * @since 2.1.0
 */
public final class PptxBookmarkMarkerRenderHandler
        implements PptxFragmentRenderHandler<BookmarkMarkerPayload> {

    /**
     * Creates the bookmark-marker handler.
     */
    public PptxBookmarkMarkerRenderHandler() {
    }

    @Override
    public Class<BookmarkMarkerPayload> payloadType() {
        return BookmarkMarkerPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       BookmarkMarkerPayload payload,
                       PptxRenderEnvironment environment) {
        // Intentionally empty: finishRenderedFragment registers the bookmark.
    }
}
