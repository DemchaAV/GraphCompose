package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.engine.components.content.text.TextDecoration;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

/**
 * Draws underline and strikethrough marks for decorated text runs. The
 * decoration flags previously resolved only to font faces (which alias to the
 * regular program), so decorated text rendered as plain glyphs; the PPTX
 * backend draws PowerPoint's native marks, and this brings the PDF reference
 * to visual parity.
 *
 * <p>Marks are filled rectangles collected while the glyphs stream out (path
 * operators are illegal inside a {@code BT}/{@code ET} block) and painted
 * after the text, one em-proportional band per decorated run.</p>
 */
final class PdfTextDecorations {

    /**
     * Distance below the baseline, as a fraction of the font size — the
     * classic Type&nbsp;1 convention (Helvetica AFM: UnderlinePosition
     * −100/1000).
     */
    private static final double UNDERLINE_OFFSET_EM = 0.10;

    /**
     * Distance above the baseline, as a fraction of the font size. No AFM
     * field standardizes it; roughly half the x-height matches common faces
     * and PowerPoint's own strikeout placement.
     */
    private static final double STRIKETHROUGH_OFFSET_EM = 0.28;

    /** Mark thickness (Helvetica AFM: UnderlineThickness 50/1000). */
    private static final double THICKNESS_EM = 0.05;

    private PdfTextDecorations() {
    }

    /**
     * One decorated run: where its glyphs sit and how to mark them.
     *
     * @param x          left edge of the run, in page points
     * @param baselineY  baseline of the run, in page points
     * @param width      advance width of the run
     * @param fontSize   run font size (scales offset and thickness)
     * @param color      run colour (alpha honoured)
     * @param decoration which mark to draw
     */
    record Segment(double x,
                   double baselineY,
                   double width,
                   double fontSize,
                   Color color,
                   TextDecoration decoration) {
    }

    /** Whether this decoration draws a mark (as opposed to a face choice). */
    static boolean drawsMark(TextDecoration decoration) {
        return decoration == TextDecoration.UNDERLINE
                || decoration == TextDecoration.STRIKETHROUGH;
    }

    /**
     * Paints every collected mark. Each segment runs in its own graphics
     * state, so the fill colour and alpha never leak into later content.
     */
    static void draw(PdfRenderEnvironment environment,
                     PDPageContentStream stream,
                     List<Segment> segments) throws IOException {
        for (Segment segment : segments) {
            if (segment.width() <= 0) {
                continue;
            }
            double thickness = THICKNESS_EM * segment.fontSize();
            double markY = segment.decoration() == TextDecoration.UNDERLINE
                    ? segment.baselineY() - UNDERLINE_OFFSET_EM * segment.fontSize()
                    : segment.baselineY() + STRIKETHROUGH_OFFSET_EM * segment.fontSize();
            stream.saveGraphicsState();
            try {
                PdfAlphaSupport.applyFillAlpha(environment, stream, segment.color());
                stream.setNonStrokingColor(segment.color());
                stream.addRect((float) segment.x(),
                        (float) (markY - thickness / 2.0),
                        (float) segment.width(),
                        (float) thickness);
                stream.fill();
            } finally {
                stream.restoreGraphicsState();
            }
        }
    }
}
