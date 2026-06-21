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
 * <p>Gradient fills clip to the path and paint a native shading; gradient
 * strokes set a shading-pattern stroking colour (pattern type 2) so the
 * outline itself carries the gradient. Flat-colour paths bypass both and
 * take the exact pre-gradient code path, byte for byte.</p>
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
        PdfPathPainter.paintPath(stream, environment, fragment.pageIndex(),
                (float) fragment.x(), (float) fragment.y(),
                (float) fragment.width(), (float) fragment.height(),
                payload.segments(), payload.fillColor(), payload.fillPaint(),
                payload.stroke(), payload.strokePaint(),
                payload.dashPattern(), payload.lineCap(), payload.lineJoin(), null);
    }
}
