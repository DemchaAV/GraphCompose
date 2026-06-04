package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.EllipseFragmentPayload;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

/**
 * Renders fixed ellipse and circle fragments.
 *
 * @author Artem Demchyshyn
 */
public final class PdfEllipseFragmentRenderHandler
        implements PdfFragmentRenderHandler<EllipseFragmentPayload> {
    private static final float BEZIER_CIRCLE_CONSTANT = 0.552284749831f;

    /**
     * Creates the ellipse fragment renderer.
     */
    public PdfEllipseFragmentRenderHandler() {
    }

    @Override
    public Class<EllipseFragmentPayload> payloadType() {
        return EllipseFragmentPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       EllipseFragmentPayload payload,
                       PdfRenderEnvironment environment) throws IOException {
        if (fragment.width() <= 0 || fragment.height() <= 0) {
            return;
        }
        PDPageContentStream stream = environment.pageSurface(fragment.pageIndex());
        float x = (float) fragment.x();
        float y = (float) fragment.y();
        float width = (float) fragment.width();
        float height = (float) fragment.height();
        PdfShapeGeometry.fillAndStrokePath(stream, payload.fillColor(), payload.stroke(),
                s -> drawEllipse(s, x, y, width, height));
    }

    static void drawEllipse(PDPageContentStream stream,
                                    float x,
                                    float y,
                                    float width,
                                    float height) throws IOException {
        float centerX = x + width / 2.0f;
        float centerY = y + height / 2.0f;
        float radiusX = width / 2.0f;
        float radiusY = height / 2.0f;
        float controlX = radiusX * BEZIER_CIRCLE_CONSTANT;
        float controlY = radiusY * BEZIER_CIRCLE_CONSTANT;

        stream.moveTo(centerX + radiusX, centerY);
        stream.curveTo(centerX + radiusX, centerY + controlY,
                centerX + controlX, centerY + radiusY,
                centerX, centerY + radiusY);
        stream.curveTo(centerX - controlX, centerY + radiusY,
                centerX - radiusX, centerY + controlY,
                centerX - radiusX, centerY);
        stream.curveTo(centerX - radiusX, centerY - controlY,
                centerX - controlX, centerY - radiusY,
                centerX, centerY - radiusY);
        stream.curveTo(centerX + controlX, centerY - radiusY,
                centerX + radiusX, centerY - controlY,
                centerX + radiusX, centerY);
        stream.closePath();
    }
}
