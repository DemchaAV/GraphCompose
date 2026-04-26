package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.PlacedFragment;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

/**
 * Renders fixed rectangle-like shape fragments.
 *
 * @author Artem Demchyshyn
 */
public final class PdfShapeFragmentRenderHandler
        implements PdfFragmentRenderHandler<BuiltInNodeDefinitions.ShapeFragmentPayload> {
    private static final float BEZIER_CIRCLE_CONSTANT = 0.552284749831f;

    /**
     * Creates the shape fragment renderer.
     */
    public PdfShapeFragmentRenderHandler() {
    }

    @Override
    public Class<BuiltInNodeDefinitions.ShapeFragmentPayload> payloadType() {
        return BuiltInNodeDefinitions.ShapeFragmentPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       BuiltInNodeDefinitions.ShapeFragmentPayload payload,
                       PdfRenderEnvironment environment) throws IOException {
        if (fragment.width() <= 0 || fragment.height() <= 0) {
            return;
        }

        boolean hasFill = payload.fillColor() != null;
        boolean hasStroke = payload.stroke() != null
                && payload.stroke().strokeColor() != null
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
            float x = (float) fragment.x();
            float y = (float) fragment.y();
            float width = (float) fragment.width();
            float height = (float) fragment.height();
            float radius = (float) Math.min(payload.cornerRadius(), Math.min(fragment.width(), fragment.height()) / 2.0);
            if (radius > 0f) {
                drawRoundedRectangle(stream, x, y, width, height, radius);
            } else {
                stream.addRect(x, y, width, height);
            }
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

    private static void drawRoundedRectangle(PDPageContentStream stream,
                                             float x,
                                             float y,
                                             float width,
                                             float height,
                                             float radius) throws IOException {
        float control = radius * BEZIER_CIRCLE_CONSTANT;
        stream.moveTo(x + radius, y + height);
        stream.lineTo(x + width - radius, y + height);
        stream.curveTo(x + width - radius + control, y + height,
                x + width, y + height - radius + control,
                x + width, y + height - radius);
        stream.lineTo(x + width, y + radius);
        stream.curveTo(x + width, y + radius - control,
                x + width - radius + control, y,
                x + width - radius, y);
        stream.lineTo(x + radius, y);
        stream.curveTo(x + radius - control, y,
                x, y + radius - control,
                x, y + radius);
        stream.lineTo(x, y + height - radius);
        stream.curveTo(x, y + height - radius + control,
                x + radius - control, y + height,
                x + radius, y + height);
        stream.closePath();
    }
}
