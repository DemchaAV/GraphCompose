package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.PathFragmentPayload;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

/**
 * Renders fixed vector-path fragments with native PDF line and cubic-Bézier
 * operators — curves stay smooth at any zoom level.
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public final class PdfPathFragmentRenderHandler
        implements PdfFragmentRenderHandler<PathFragmentPayload> {

    /**
     * Creates the path fragment renderer.
     */
    public PdfPathFragmentRenderHandler() {
    }

    @Override
    public Class<PathFragmentPayload> payloadType() {
        return PathFragmentPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       PathFragmentPayload payload,
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
                s -> PdfShapeGeometry.addPathSegments(s, x, y, width, height, payload.segments()));
    }
}
