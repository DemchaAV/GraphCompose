package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.PlacedFragment;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;

/**
 * Renders resolved image fragments using the shared page-scoped image cache.
 */
public final class PdfImageFragmentRenderHandler
        implements PdfFragmentRenderHandler<BuiltInNodeDefinitions.ImageFragmentPayload> {

    @Override
    public Class<BuiltInNodeDefinitions.ImageFragmentPayload> payloadType() {
        return BuiltInNodeDefinitions.ImageFragmentPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       BuiltInNodeDefinitions.ImageFragmentPayload payload,
                       PdfRenderEnvironment environment) throws IOException {
        if (fragment.width() <= 0 || fragment.height() <= 0) {
            return;
        }

        PDImageXObject image = environment.resolveImage(payload.imageData());
        PDPageContentStream stream = environment.pageSurface(fragment.pageIndex());
        stream.drawImage(image, (float) fragment.x(), (float) fragment.y(), (float) fragment.width(), (float) fragment.height());
    }
}
