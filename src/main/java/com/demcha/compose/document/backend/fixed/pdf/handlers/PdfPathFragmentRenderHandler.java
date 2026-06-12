package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.PathFragmentPayload;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;

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
        float x = (float) fragment.x();
        float y = (float) fragment.y();
        float width = (float) fragment.width();
        float height = (float) fragment.height();

        if (payload.fillPaint() == null && payload.strokePaint() == null) {
            PdfShapeGeometry.fillAndStrokePath(stream, payload.fillColor(), payload.stroke(),
                    payload.dashPattern(),
                    s -> PdfShapeGeometry.addPathSegments(s, x, y, width, height, payload.segments()));
            return;
        }

        // Gradient route: fill and stroke are separate passes because each
        // may independently be a flat colour or a shading.
        stream.saveGraphicsState();
        try {
            if (payload.fillPaint() != null) {
                // Clip in a nested state so the clip never leaks into the
                // stroke pass (mirrors the shape handler).
                stream.saveGraphicsState();
                try {
                    PdfShapeGeometry.addPathSegments(stream, x, y, width, height, payload.segments());
                    stream.clip();
                    stream.shadingFill(PdfShadingSupport.build(payload.fillPaint(), x, y, width, height));
                } finally {
                    stream.restoreGraphicsState();
                }
            } else if (payload.fillColor() != null) {
                PdfShapeGeometry.fillAndStrokePath(stream, payload.fillColor(), null, null,
                        s -> PdfShapeGeometry.addPathSegments(s, x, y, width, height, payload.segments()));
            }

            boolean hasStrokeWidth = payload.stroke() != null && payload.stroke().width() > 0;
            if (payload.strokePaint() != null && hasStrokeWidth) {
                PDResources resources = environment.document()
                        .getPage(fragment.pageIndex()).getResources();
                stream.setStrokingColor(PdfShadingSupport.strokePattern(
                        payload.strokePaint(), resources, x, y, width, height));
                stream.setLineWidth((float) payload.stroke().width());
                PdfShapeGeometry.applyDashPattern(stream, payload.dashPattern());
                PdfShapeGeometry.addPathSegments(stream, x, y, width, height, payload.segments());
                stream.stroke();
            } else if (hasStrokeWidth && payload.stroke().strokeColor() != null) {
                PdfShapeGeometry.fillAndStrokePath(stream, null, payload.stroke(),
                        payload.dashPattern(),
                        s -> PdfShapeGeometry.addPathSegments(s, x, y, width, height, payload.segments()));
            }
        } finally {
            stream.restoreGraphicsState();
        }
    }
}
