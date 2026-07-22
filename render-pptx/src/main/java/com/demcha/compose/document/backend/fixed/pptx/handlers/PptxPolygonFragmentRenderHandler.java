package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.backend.fixed.pptx.PptxCoordinates;
import com.demcha.compose.document.backend.fixed.pptx.PptxFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.PptxRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.PolygonFragmentPayload;

/**
 * Renders fixed polygon fragments as freeform shapes: the payload's
 * normalized vertex ring scales to the fragment box with the engine's y-up
 * convention flipped into slide space — the same scaling the PDF handler
 * applies through {@code PdfShapeGeometry.addPolygonPath}.
 *
 * @since 2.1.0
 */
public final class PptxPolygonFragmentRenderHandler
        implements PptxFragmentRenderHandler<PolygonFragmentPayload> {

    /**
     * Creates the polygon fragment renderer.
     */
    public PptxPolygonFragmentRenderHandler() {
    }

    @Override
    public Class<PolygonFragmentPayload> payloadType() {
        return PolygonFragmentPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       PolygonFragmentPayload payload,
                       PptxRenderEnvironment environment) {
        if (fragment.width() <= 0 || fragment.height() <= 0 || payload.points().size() < 3) {
            return;
        }
        if (payload.fillColor() == null && !PptxShapeStyle.drawable(payload.stroke())) {
            return;
        }
        PptxInlineGeometry.drawPolygon(
                environment.surface(fragment.pageIndex()),
                payload.points(),
                PptxCoordinates.anchorOf(environment.canvasHeight(), fragment),
                payload.fillColor(),
                payload.stroke());
    }
}
