package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.BookmarkMarkerPayload;

/**
 * No-op render handler for a {@link BookmarkMarkerPayload} fragment. The marker
 * draws nothing — the outline entry is registered generically in the backend's
 * semantic post-pass (the payload implements {@code PdfSemanticFragmentPayload}),
 * so this handler exists only so fragment dispatch finds a handler for the type.
 *
 * @author Artem Demchyshyn
 */
public final class PdfBookmarkMarkerRenderHandler
        implements PdfFragmentRenderHandler<BookmarkMarkerPayload> {

    /**
     * Creates the bookmark-marker handler.
     */
    public PdfBookmarkMarkerRenderHandler() {
    }

    @Override
    public Class<BookmarkMarkerPayload> payloadType() {
        return BookmarkMarkerPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       BookmarkMarkerPayload payload,
                       PdfRenderEnvironment environment) {
        // Intentionally empty — registration happens in finishRenderedFragment.
    }
}
