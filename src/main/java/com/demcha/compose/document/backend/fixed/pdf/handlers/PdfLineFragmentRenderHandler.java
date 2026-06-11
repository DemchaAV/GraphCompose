package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.LineFragmentPayload;
import com.demcha.compose.document.style.DocumentDashPattern;
import com.demcha.compose.engine.components.content.shape.Stroke;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.List;

/**
 * Renders fixed semantic line fragments.
 *
 * @author Artem Demchyshyn
 */
public final class PdfLineFragmentRenderHandler
        implements PdfFragmentRenderHandler<LineFragmentPayload> {

    /**
     * Creates the line fragment renderer.
     */
    public PdfLineFragmentRenderHandler() {
    }

    private static void applyDashPattern(PDPageContentStream stream, DocumentDashPattern dash) throws IOException {
        if (dash == null || dash.isSolid()) {
            return;
        }
        List<Double> segments = dash.segments();
        float[] dashArray = new float[segments.size()];
        for (int i = 0; i < dashArray.length; i++) {
            dashArray[i] = segments.get(i).floatValue();
        }
        stream.setLineDashPattern(dashArray, 0f);
    }

    @Override
    public Class<LineFragmentPayload> payloadType() {
        return LineFragmentPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       LineFragmentPayload payload,
                       PdfRenderEnvironment environment) throws IOException {
        Stroke stroke = payload.stroke();
        if (stroke == null || stroke.strokeColor() == null || stroke.strokeColor().color() == null || stroke.width() <= 0) {
            return;
        }

        PDPageContentStream stream = environment.pageSurface(fragment.pageIndex());
        stream.saveGraphicsState();
        try {
            stream.setStrokingColor(stroke.strokeColor().color());
            stream.setLineWidth((float) stroke.width());
            applyDashPattern(stream, payload.dashPattern());
            stream.moveTo((float) (fragment.x() + payload.startX()), (float) (fragment.y() + payload.startY()));
            stream.lineTo((float) (fragment.x() + payload.endX()), (float) (fragment.y() + payload.endY()));
            stream.stroke();
        } finally {
            stream.restoreGraphicsState();
        }
    }
}
