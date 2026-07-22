package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.backend.fixed.pptx.PptxFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.PptxRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.TransformEndPayload;

/**
 * Closes the innermost transform group opened by
 * {@link PptxTransformBeginRenderHandler}. The layout compiler guarantees the
 * begin/end pairing on one page, so the pop always matches the push.
 *
 * @since 2.1.0
 */
public final class PptxTransformEndRenderHandler
        implements PptxFragmentRenderHandler<TransformEndPayload> {

    /**
     * Creates the transform-end handler.
     */
    public PptxTransformEndRenderHandler() {
    }

    @Override
    public Class<TransformEndPayload> payloadType() {
        return TransformEndPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       TransformEndPayload payload,
                       PptxRenderEnvironment environment) {
        environment.popGroup();
    }
}
