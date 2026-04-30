package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.PlacedFragment;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

/**
 * Closes the graphics-state transform region opened by
 * {@link PdfTransformBeginRenderHandler}. Always issues a single
 * {@code restoreGraphicsState()} on the page surface — the layout
 * compiler guarantees that this fragment lands on the same page as the
 * matching begin fragment with the same {@code ownerPath}.
 *
 * @author Artem Demchyshyn
 */
public final class PdfTransformEndRenderHandler
        implements PdfFragmentRenderHandler<BuiltInNodeDefinitions.TransformEndPayload> {

    /**
     * Creates the transform-end handler.
     */
    public PdfTransformEndRenderHandler() {
    }

    @Override
    public Class<BuiltInNodeDefinitions.TransformEndPayload> payloadType() {
        return BuiltInNodeDefinitions.TransformEndPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       BuiltInNodeDefinitions.TransformEndPayload payload,
                       PdfRenderEnvironment environment) throws IOException {
        PDPageContentStream stream = environment.pageSurface(fragment.pageIndex());
        stream.restoreGraphicsState();
    }
}
