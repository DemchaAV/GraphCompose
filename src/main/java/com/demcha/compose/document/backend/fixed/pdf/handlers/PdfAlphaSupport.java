package com.demcha.compose.document.backend.fixed.pdf.handlers;

import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;

import java.awt.*;
import java.io.IOException;

/**
 * Applies the alpha channel of AWT colours to the PDF graphics state.
 *
 * <p>PDFBox's {@code setNonStrokingColor}/{@code setStrokingColor} silently
 * drop the alpha component, so translucent fills/strokes need an
 * {@link PDExtendedGraphicsState} alpha constant. Both helpers are no-ops for
 * fully opaque colours, which keeps existing documents byte-identical. Callers
 * must wrap usage in {@code saveGraphicsState()} / {@code restoreGraphicsState()}
 * so the alpha never leaks into later fragments.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
final class PdfAlphaSupport {

    private PdfAlphaSupport() {
    }

    /**
     * Sets the non-stroking (fill) alpha constant when the colour is translucent.
     *
     * @param stream page content stream inside a saved graphics state
     * @param color  fill colour, possibly carrying alpha
     * @throws IOException when the graphics-state write fails
     */
    static void applyFillAlpha(PDPageContentStream stream, Color color) throws IOException {
        if (color == null || color.getAlpha() >= 255) {
            return;
        }
        PDExtendedGraphicsState state = new PDExtendedGraphicsState();
        state.setNonStrokingAlphaConstant(color.getAlpha() / 255f);
        stream.setGraphicsStateParameters(state);
    }

    /**
     * Sets the stroking alpha constant when the colour is translucent.
     *
     * @param stream page content stream inside a saved graphics state
     * @param color  stroke colour, possibly carrying alpha
     * @throws IOException when the graphics-state write fails
     */
    static void applyStrokeAlpha(PDPageContentStream stream, Color color) throws IOException {
        if (color == null || color.getAlpha() >= 255) {
            return;
        }
        PDExtendedGraphicsState state = new PDExtendedGraphicsState();
        state.setStrokingAlphaConstant(color.getAlpha() / 255f);
        stream.setGraphicsStateParameters(state);
    }
}
