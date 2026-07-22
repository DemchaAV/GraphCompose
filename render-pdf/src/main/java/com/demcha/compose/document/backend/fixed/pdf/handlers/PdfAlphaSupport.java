package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;

import java.awt.*;
import java.io.IOException;

/**
 * Applies the alpha channel of AWT colours to the PDF graphics state.
 *
 * <p>PDFBox's {@code setNonStrokingColor}/{@code setStrokingColor} silently
 * drop the alpha component, so translucent fills/strokes need an
 * {@link PDExtendedGraphicsState} alpha constant. Both colour helpers are
 * no-ops for fully opaque colours, which keeps existing documents
 * byte-identical. Callers must wrap usage in {@code saveGraphicsState()} /
 * {@code restoreGraphicsState()} so the alpha never leaks into later
 * fragments.</p>
 *
 * <p>The graphics states come from the render environment's per-pass cache —
 * one shared instance per distinct (channel, alpha) pair — so a page's
 * {@code /ExtGState} resources stay bounded by the number of distinct alpha
 * values instead of growing with every translucent draw.</p>
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
     * @param environment render environment owning the shared graphics states
     * @param stream      page content stream inside a saved graphics state
     * @param color       fill colour, possibly carrying alpha
     * @throws IOException when the graphics-state write fails
     */
    static void applyFillAlpha(PdfRenderEnvironment environment,
                               PDPageContentStream stream,
                               Color color) throws IOException {
        if (color == null || color.getAlpha() >= 255) {
            return;
        }
        setFillAlpha(environment, stream, color.getAlpha() / 255f);
    }

    /**
     * Writes an explicit non-stroking alpha constant, including {@code 1.0} —
     * used by the paragraph text state, which must also RESTORE opacity (the
     * {@code gs} survives {@code ET} and nested draws inherit it).
     *
     * @param environment render environment owning the shared graphics states
     * @param stream      page content stream
     * @param alpha       alpha constant in [0, 1]
     * @throws IOException when the graphics-state write fails
     */
    static void setFillAlpha(PdfRenderEnvironment environment,
                             PDPageContentStream stream,
                             float alpha) throws IOException {
        stream.setGraphicsStateParameters(environment.fillAlphaState(alpha));
    }

    /**
     * Sets the stroking alpha constant when the colour is translucent.
     *
     * @param environment render environment owning the shared graphics states
     * @param stream      page content stream inside a saved graphics state
     * @param color       stroke colour, possibly carrying alpha
     * @throws IOException when the graphics-state write fails
     */
    static void applyStrokeAlpha(PdfRenderEnvironment environment,
                                 PDPageContentStream stream,
                                 Color color) throws IOException {
        if (color == null || color.getAlpha() >= 255) {
            return;
        }
        stream.setGraphicsStateParameters(environment.strokeAlphaState(color.getAlpha() / 255f));
    }
}
