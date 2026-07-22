package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.backend.fixed.pptx.PptxCoordinates;
import com.demcha.compose.document.backend.fixed.pptx.PptxFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.PptxRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.PathFragmentPayload;
import org.apache.poi.xslf.usermodel.XSLFFreeformShape;

import java.awt.Color;

/**
 * Renders free path fragments as freeform shapes: segments scale to the
 * fragment box (y-up normalized coordinates flipped into slide space), solid
 * fills and strokes apply natively, and gradient fills or gradient strokes
 * stamp DrawingML {@code gradFill} — natively, unlike PDF's shading-pattern
 * workaround. Dash patterns, caps, and joins map through the shared style
 * helper.
 *
 * @since 2.1.0
 */
public final class PptxPathFragmentRenderHandler
        implements PptxFragmentRenderHandler<PathFragmentPayload> {

    /**
     * Creates the path fragment renderer.
     */
    public PptxPathFragmentRenderHandler() {
    }

    @Override
    public Class<PathFragmentPayload> payloadType() {
        return PathFragmentPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       PathFragmentPayload payload,
                       PptxRenderEnvironment environment) {
        if (fragment.width() <= 0 || fragment.height() <= 0 || payload.segments().isEmpty()) {
            return;
        }
        boolean hasFill = payload.fillColor() != null || payload.fillPaint() != null;
        // The payload contract keeps the width on the plain stroke even when a
        // gradient paints it, matching the PDF painter: no stroke width, no ink.
        boolean hasGradientStroke = payload.strokePaint() != null
                && payload.stroke() != null && payload.stroke().width() > 0;
        boolean hasStroke = PptxShapeStyle.drawable(payload.stroke()) || hasGradientStroke;
        if (!hasFill && !hasStroke) {
            return;
        }

        Color solidFill = payload.fillPaint() == null ? payload.fillColor() : null;
        XSLFFreeformShape shape = PptxInlineGeometry.drawPath(
                environment.surface(fragment.pageIndex()),
                PptxInlineGeometry.path(payload.segments(),
                        PptxCoordinates.anchorOf(environment.canvasHeight(), fragment)),
                solidFill,
                hasGradientStroke ? null : payload.stroke());
        if (payload.fillPaint() != null) {
            PptxGradientFill.applyGradientFill(shape, payload.fillPaint());
        }
        if (hasGradientStroke) {
            PptxGradientFill.applyGradientStroke(
                    shape, payload.strokePaint(), payload.stroke().width());
        }
        PptxShapeStyle.applyLineCap(shape, payload.lineCap());
        PptxShapeStyle.applyLineJoin(shape, payload.lineJoin());
        PptxShapeStyle.applyDashPattern(shape, payload.dashPattern());
    }
}
