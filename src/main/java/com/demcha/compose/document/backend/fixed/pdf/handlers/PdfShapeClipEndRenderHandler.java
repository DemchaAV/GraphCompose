package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.PlacedFragment;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

/**
 * Closes the graphics-state clip region opened by
 * {@link PdfShapeClipBeginRenderHandler}. Always issues a single
 * {@code restoreGraphicsState()} on the page surface — the layout
 * compiler guarantees that this fragment lands on the same page as the
 * matching begin fragment with the same {@code ownerPath}.
 *
 * @author Artem Demchyshyn
 */
public final class PdfShapeClipEndRenderHandler
        implements PdfFragmentRenderHandler<BuiltInNodeDefinitions.ShapeClipEndPayload> {

    /**
     * Creates the clip-end handler.
     */
    public PdfShapeClipEndRenderHandler() {
    }

    @Override
    public Class<BuiltInNodeDefinitions.ShapeClipEndPayload> payloadType() {
        return BuiltInNodeDefinitions.ShapeClipEndPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       BuiltInNodeDefinitions.ShapeClipEndPayload payload,
                       PdfRenderEnvironment environment) throws IOException {
        PDPageContentStream stream = environment.pageSurface(fragment.pageIndex());
        stream.restoreGraphicsState();
    }
}
