package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.backend.fixed.pptx.PptxCoordinates;
import com.demcha.compose.document.backend.fixed.pptx.PptxFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.PptxRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.EllipseFragmentPayload;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;

/**
 * Renders fixed ellipse and circle fragments as the native {@code ellipse}
 * preset inscribed in the fragment box — the same geometry the PDF handler
 * builds from four Bézier arcs.
 *
 * @since 2.1.0
 */
public final class PptxEllipseFragmentRenderHandler
        implements PptxFragmentRenderHandler<EllipseFragmentPayload> {

    /**
     * Creates the ellipse fragment renderer.
     */
    public PptxEllipseFragmentRenderHandler() {
    }

    @Override
    public Class<EllipseFragmentPayload> payloadType() {
        return EllipseFragmentPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       EllipseFragmentPayload payload,
                       PptxRenderEnvironment environment) {
        if (fragment.width() <= 0 || fragment.height() <= 0) {
            return;
        }
        if (payload.fillColor() == null && !PptxShapeStyle.drawable(payload.stroke())) {
            return;
        }
        XSLFAutoShape shape = environment.slide(fragment.pageIndex()).createAutoShape();
        shape.setShapeType(ShapeType.ELLIPSE);
        shape.setAnchor(PptxCoordinates.anchorOf(environment.canvasHeight(), fragment));
        PptxShapeStyle.applyFill(shape, payload.fillColor());
        PptxShapeStyle.applyStroke(shape, payload.stroke());
    }
}
