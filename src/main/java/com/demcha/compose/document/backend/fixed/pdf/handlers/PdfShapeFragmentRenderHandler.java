package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.PlacedFragment;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

/**
 * Renders fixed rectangle-like shape fragments.
 */
public final class PdfShapeFragmentRenderHandler
        implements PdfFragmentRenderHandler<BuiltInNodeDefinitions.ShapeFragmentPayload> {

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
            stream.addRect((float) fragment.x(), (float) fragment.y(), (float) fragment.width(), (float) fragment.height());
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
}
