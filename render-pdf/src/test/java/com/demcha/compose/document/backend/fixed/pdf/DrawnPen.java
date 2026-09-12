package com.demcha.compose.document.backend.fixed.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.pdmodel.PDDocument;

import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Where the pen actually put each glyph.
 *
 * <p>{@code PDFTextStripper} answers a different question — what a <em>reader</em>
 * makes of the page — and two things this codebase does deliberately get in the
 * way of using it for geometry. It invents a word break wherever glyphs sit far
 * apart, which is what tracking is; and where a run states its own
 * {@code ActualText} it reports that string instead of the glyphs, so the
 * positions stop lining up one to one.</p>
 *
 * <p>This reads the drawing operators instead. {@code showGlyph} is the callback
 * PDFBox makes for each glyph it paints, with the text-rendering matrix at that
 * moment, so the x it reports is the pen — {@code Tc} and all — and no
 * extraction heuristic or marked-content section stands in between.</p>
 */
final class DrawnPen {

    private DrawnPen() {
    }

    /** One painted glyph: the pen position it was placed at, and how far it advanced. */
    record Placement(double x, double y, double advance) {

        @Override
        public String toString() {
            return String.format("@%.2f(+%.2f)", x, advance);
        }
    }

    /**
     * Every glyph the first page paints, in painting order.
     *
     * @param pdf a rendered document
     * @return the pen positions
     * @throws IOException if the document cannot be read
     */
    static List<Placement> placements(byte[] pdf) throws IOException {
        return placements(pdf, 0);
    }

    /**
     * Every glyph a page paints, in painting order.
     *
     * @param pdf   a rendered document
     * @param index zero-based page index
     * @return the pen positions
     * @throws IOException if the document cannot be read
     */
    static List<Placement> placements(byte[] pdf, int index) throws IOException {
        List<Placement> placements = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            // Driven through PDFTextStripper because a bare PDFStreamEngine has
            // no operators registered and would process nothing. Only showGlyph
            // is taken from it — that callback sits below the ActualText
            // substitution the stripper's own text output goes through.
            PDFTextStripper engine = new PDFTextStripper() {
                @Override
                protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code,
                                         Vector displacement) throws IOException {
                    placements.add(new Placement(
                            textRenderingMatrix.getTranslateX(),
                            textRenderingMatrix.getTranslateY(),
                            displacement.getX() * textRenderingMatrix.getScalingFactorX()));
                    super.showGlyph(textRenderingMatrix, font, code, displacement);
                }
            };
            engine.setStartPage(index + 1);
            engine.setEndPage(index + 1);
            engine.getText(document);
        }
        return placements;
    }

    /** The x of the first glyph painted on the page. */
    static double firstX(byte[] pdf) throws IOException {
        return placements(pdf).get(0).x();
    }

    /** The x of the last glyph painted on the page. */
    static double lastX(byte[] pdf) throws IOException {
        List<Placement> placements = placements(pdf);
        return placements.get(placements.size() - 1).x();
    }

    /**
     * Pen distance from the first glyph to the last.
     *
     * <p>Note this is N-1 gaps, not the run's full advance: the pen's final
     * trailing step lands past the last glyph and no glyph records it.</p>
     */
    static double firstToLastX(byte[] pdf) throws IOException {
        return lastX(pdf) - firstX(pdf);
    }

    /** Distance from glyph {@code i-1}'s pen position to glyph {@code i}'s. */
    static double step(List<Placement> placements, int i) {
        return placements.get(i).x() - placements.get(i - 1).x();
    }
}
