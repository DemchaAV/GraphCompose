package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.backend.fixed.pptx.PptxFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.PptxRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.AnchorMarkerPayload;

/**
 * Registers a named anchor destination; nothing is drawn. Slide-jump
 * hyperlinks resolve against these records when navigation emission lands.
 *
 * @since 2.1.0
 */
public final class PptxAnchorMarkerRenderHandler
        implements PptxFragmentRenderHandler<AnchorMarkerPayload> {

    /**
     * Creates the anchor-marker handler.
     */
    public PptxAnchorMarkerRenderHandler() {
    }

    @Override
    public Class<AnchorMarkerPayload> payloadType() {
        return AnchorMarkerPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       AnchorMarkerPayload payload,
                       PptxRenderEnvironment environment) {
        environment.registerAnchor(fragment, payload.anchor());
    }
}
