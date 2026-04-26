package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.document.image.DocumentImageFitMode;
import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.PlacedFragment;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;

/**
 * Renders resolved image fragments using the shared page-scoped image cache.
 *
 * @author Artem Demchyshyn
 */
public final class PdfImageFragmentRenderHandler
        implements PdfFragmentRenderHandler<BuiltInNodeDefinitions.ImageFragmentPayload> {

    /**
     * Creates the image fragment renderer.
     */
    public PdfImageFragmentRenderHandler() {
    }

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
        DrawBounds bounds = resolveDrawBounds(fragment, payload.fitMode(), image.getWidth(), image.getHeight());
        if (payload.fitMode() == DocumentImageFitMode.COVER) {
            stream.saveGraphicsState();
            stream.addRect((float) fragment.x(), (float) fragment.y(), (float) fragment.width(), (float) fragment.height());
            stream.clip();
            stream.drawImage(image, bounds.x(), bounds.y(), bounds.width(), bounds.height());
            stream.restoreGraphicsState();
        } else {
            stream.drawImage(image, bounds.x(), bounds.y(), bounds.width(), bounds.height());
        }
    }

    private static DrawBounds resolveDrawBounds(PlacedFragment fragment,
                                                DocumentImageFitMode fitMode,
                                                int imageWidth,
                                                int imageHeight) {
        double boxWidth = fragment.width();
        double boxHeight = fragment.height();
        double sourceWidth = Math.max(1, imageWidth);
        double sourceHeight = Math.max(1, imageHeight);

        if (fitMode == DocumentImageFitMode.STRETCH) {
            return new DrawBounds(
                    (float) fragment.x(),
                    (float) fragment.y(),
                    (float) boxWidth,
                    (float) boxHeight);
        }

        double scale = fitMode == DocumentImageFitMode.COVER
                ? Math.max(boxWidth / sourceWidth, boxHeight / sourceHeight)
                : Math.min(boxWidth / sourceWidth, boxHeight / sourceHeight);
        double drawWidth = sourceWidth * scale;
        double drawHeight = sourceHeight * scale;
        double drawX = fragment.x() + (boxWidth - drawWidth) / 2.0;
        double drawY = fragment.y() + (boxHeight - drawHeight) / 2.0;
        return new DrawBounds((float) drawX, (float) drawY, (float) drawWidth, (float) drawHeight);
    }

    private record DrawBounds(float x, float y, float width, float height) {
    }
}
