package com.demcha.compose.document.templates.fidelity;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The lines of text a PDF draws, each with its page, its letters, and where its first letter
 * stands.
 *
 * <p>Glyphs are read in the order the page draws them. A line ends where the next glyph stands
 * on another baseline (more than half its size off), steps back to the left, or stands further
 * off than one and a half times its size: two runs of text on one baseline are two lines when
 * the second starts that far past the first, as two columns' text does. The key is the line's
 * letters without white space, lower-cased, so text an editor sets with spaces between tracked
 * letters, or with a trailing space, keys as the page's does. Lines of fewer than three letters
 * are left out.</p>
 */
final class PdfLines {

    /**
     * A line: its page, its letters, and its first letter's left edge and baseline, from the
     * page's left and top, in points.
     */
    record Line(int page, String key, double x, double baseline) {
    }

    private final List<Line> lines;
    private final int pages;

    PdfLines(List<Line> lines, int pages) {
        this.lines = List.copyOf(lines);
        this.pages = pages;
    }

    /** Reads a PDF's lines. */
    static PdfLines of(Path pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            Collector collector = new Collector();
            collector.setSortByPosition(false);
            collector.getText(document);
            collector.endLine();
            return new PdfLines(collector.lines, document.getNumberOfPages());
        }
    }

    int pages() {
        return pages;
    }

    List<Line> lines() {
        return lines;
    }

    private static final class Collector extends PDFTextStripper {

        private final List<Line> lines = new ArrayList<>();
        private final StringBuilder key = new StringBuilder();
        private int page;
        private double x = Double.NaN;
        private double baseline = Double.NaN;
        private double lastEnd = Double.NaN;
        private double lastX = Double.NaN;

        private Collector() throws IOException {
        }

        @Override
        protected void startPage(org.apache.pdfbox.pdmodel.PDPage pdPage) throws IOException {
            endLine();
            page = getCurrentPageNo() - 1;
            super.startPage(pdPage);
        }

        @Override
        protected void processTextPosition(TextPosition glyph) {
            double y = glyph.getYDirAdj();
            double left = glyph.getXDirAdj();
            double size = Math.max(1, glyph.getFontSizeInPt());
            String letters = glyph.getUnicode();
            // A space opens no line: Word writes one for an empty paragraph, and one drawn just
            // before a line of text beside it, on a baseline a few points off, gave that line
            // the space's baseline instead of its letters'.
            boolean blank = letters == null
                            || letters.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c));
            boolean sameLine = !Double.isNaN(baseline)
                               && Math.abs(y - baseline) < size * 0.5
                               && left >= lastX - 1
                               && left - lastEnd < size * 1.5;
            if (!sameLine) {
                endLine();
                if (blank) {
                    return;
                }
                baseline = y;
                x = left;
            }
            if (letters != null) {
                letters.codePoints()
                        .filter(c -> !Character.isWhitespace(c) && !Character.isSpaceChar(c))
                        .forEach(key::appendCodePoint);
            }
            lastX = left;
            lastEnd = left + glyph.getWidthDirAdj();
        }

        private void endLine() {
            String letters = key.toString().toLowerCase(Locale.ROOT);
            if (letters.codePointCount(0, letters.length()) >= 3) {
                lines.add(new Line(page, letters, x, baseline));
            }
            key.setLength(0);
            x = Double.NaN;
            baseline = Double.NaN;
            lastEnd = Double.NaN;
            lastX = Double.NaN;
        }
    }
}
