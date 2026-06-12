package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.PolygonFragmentPayload;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

/**
 * Renders fixed polygon fragments (diamond, triangle, star, arbitrary rings).
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public final class PdfPolygonFragmentRenderHandler
        implements PdfFragmentRenderHandler<PolygonFragmentPayload> {

    /**
     * Creates the polygon fragment renderer.
     */
    public PdfPolygonFragmentRenderHandler() {
    }

    @Override
    public Class<PolygonFragmentPayload> payloadType() {
        return PolygonFragmentPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       PolygonFragmentPayload payload,
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
                s -> PdfShapeGeometry.addPolygonPath(s, x, y, width, height, payload.points()));
    }
}
