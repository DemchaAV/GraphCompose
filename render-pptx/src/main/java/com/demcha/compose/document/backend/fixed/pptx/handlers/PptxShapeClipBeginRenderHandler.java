package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.backend.fixed.pptx.PptxFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.PptxRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ShapeClipBeginPayload;

/**
 * Runs only when the backend's clip raster fallback is disabled: clip regions
 * cannot be expressed in PPTX — DrawingML has no graphics-state clipping, only
 * per-picture source crops — so the marker degrades to a one-time capability
 * warning and the children render unclipped, the sanctioned fallback the
 * payload documents. By default the backend never dispatches here; it
 * rasterizes the whole clip region through the PDF backend instead
 * (pixel-exact clipping as one transparent picture).
 *
 * @since 2.1.0
 */
public final class PptxShapeClipBeginRenderHandler
        implements PptxFragmentRenderHandler<ShapeClipBeginPayload> {

    /**
     * Creates the clip-begin handler.
     */
    public PptxShapeClipBeginRenderHandler() {
    }

    @Override
    public Class<ShapeClipBeginPayload> payloadType() {
        return ShapeClipBeginPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       ShapeClipBeginPayload payload,
                       PptxRenderEnvironment environment) {
        PptxCapabilityNotes.clipSkipped();
    }
}
