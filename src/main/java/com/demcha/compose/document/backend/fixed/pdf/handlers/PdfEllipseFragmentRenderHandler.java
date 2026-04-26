package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.PlacedFragment;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

/**
 * Renders fixed ellipse and circle fragments.
 *
 * @author Artem Demchyshyn
 */
public final class PdfEllipseFragmentRenderHandler
        implements PdfFragmentRenderHandler<BuiltInNodeDefinitions.EllipseFragmentPayload> {
    private static final float BEZIER_CIRCLE_CONSTANT = 0.552284749831f;

    /**
     * Creates the ellipse fragment renderer.
     */
    public PdfEllipseFragmentRenderHandler() {
    }

    @Override
    public Class<BuiltInNodeDefinitions.EllipseFragmentPayload> payloadType() {
        return BuiltInNodeDefinitions.EllipseFragmentPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       BuiltInNodeDefinitions.EllipseFragmentPayload payload,
                       PdfRenderEnvironment environment) throws IOException {
        if (fragment.width() <= 0 || fragment.height() <= 0) {
            return;
        }

        boolean hasFill = payload.fillColor() != null;
        boolean hasStroke = payload.stroke() != null
                && payload.stroke().strokeColor() != null
                && payload.stroke().strokeColor().color() != null
                && payload.stroke().width() > 0;
        if (!hasFill && !hasStroke) {
            return;
        }

        PDPageContentStream stream = environment.pageSurface(fragment.pageIndex());
        stream.saveGraphicsState();
        try {
            if (hasStroke) {
                stream.setStrokingColor(payload.stroke().strokeColor().color());
                stream.setLineWidth((float) payload.stroke().width());
            }
            if (hasFill) {
                stream.setNonStrokingColor(payload.fillColor());
            }
            drawEllipse(stream, (float) fragment.x(), (float) fragment.y(), (float) fragment.width(), (float) fragment.height());
            if (hasFill && hasStroke) {
                stream.fillAndStroke();
            } else if (hasFill) {
                stream.fill();
            } else {
                stream.stroke();
            }
        } finally {
            stream.restoreGraphicsState();
        }
    }

    private static void drawEllipse(PDPageContentStream stream,
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
