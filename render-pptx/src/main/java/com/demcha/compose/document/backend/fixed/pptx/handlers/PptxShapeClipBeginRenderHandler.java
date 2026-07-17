package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.backend.fixed.pptx.PptxFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.PptxRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ShapeClipBeginPayload;

/**
 * Clip regions cannot be expressed in PPTX — DrawingML has no graphics-state
 * clipping, only per-picture source crops — so the marker degrades to a
 * one-time capability warning and the children render unclipped, the same
 * sanctioned fallback the payload documents and the DOCX backend uses.
 * Content that must be clipped exactly renders through the PDF backend or
 * the raster-slide mode.
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
