package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.AnchorMarkerPayload;

/**
 * Records an in-document navigation destination for an
 * {@link AnchorMarkerPayload} fragment. The marker draws nothing — it only
 * registers the anchor's resolved page and top-left with the render environment
 * so deferred internal links can resolve to it in the post-pass.
 *
 * @author Artem Demchyshyn
 */
public final class PdfAnchorMarkerRenderHandler
        implements PdfFragmentRenderHandler<AnchorMarkerPayload> {

    /**
     * Creates the anchor-marker handler.
     */
    public PdfAnchorMarkerRenderHandler() {
    }

    @Override
    public Class<AnchorMarkerPayload> payloadType() {
        return AnchorMarkerPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       AnchorMarkerPayload payload,
                       PdfRenderEnvironment environment) {
        environment.registerAnchor(fragment, payload.anchor());
    }
}
