package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.LayoutAnchorPayload;

/**
 * Draws nothing for a {@link LayoutAnchorPayload} fragment.
 *
 * <p>The payload reports where an anchored subtree landed so a resolved-layout pass can
 * read it; the ink is drawn by whatever the pass contributes, not here. A handler is
 * required all the same — {@code PdfFixedLayoutBackend.handlerFor} throws on a payload
 * class it does not know, so an unhandled marker would fail every render that contains
 * one.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
public final class PdfLayoutAnchorRenderHandler
        implements PdfFragmentRenderHandler<LayoutAnchorPayload> {

    /**
     * Creates the layout-anchor handler.
     */
    public PdfLayoutAnchorRenderHandler() {
    }

    @Override
    public Class<LayoutAnchorPayload> payloadType() {
        return LayoutAnchorPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       LayoutAnchorPayload payload,
                       PdfRenderEnvironment environment) {
        // Intentionally empty: an anchor is metadata, not ink.
    }
}
