package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.backend.fixed.pptx.PptxFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.PptxRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ShapeClipEndPayload;

/**
 * Matching end marker for {@link PptxShapeClipBeginRenderHandler}; a no-op,
 * since the begin marker opens no state in PPTX.
 *
 * @since 2.1.0
 */
public final class PptxShapeClipEndRenderHandler
        implements PptxFragmentRenderHandler<ShapeClipEndPayload> {

    /**
     * Creates the clip-end handler.
     */
    public PptxShapeClipEndRenderHandler() {
    }

    @Override
    public Class<ShapeClipEndPayload> payloadType() {
        return ShapeClipEndPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       ShapeClipEndPayload payload,
                       PptxRenderEnvironment environment) {
        // Intentionally empty: clip-begin degrades to a warning, no state opens.
    }
}
