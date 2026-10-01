package com.demcha.compose.document.templates.fidelity;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The lines of text a PDF draws, each keyed by its page and its letters, with the baseline its
 * first letter stands on.
 *
 * <p>Glyphs are read in the order the page draws them. A line ends where the next glyph stands
 * on another baseline, steps back to the left, or stands further off than one and a half of its
 * size: two columns on one baseline are two lines, as they are two runs of text. The key is the
 * line's letters without white space, lower-cased, so text an editor sets with spaces between
 * tracked letters, or with a trailing space, keys as the page's does. Lines of fewer than three
 * letters are left out, and only the first line of each key on a page is kept.</p>
 */
final class PdfLines {

    /** A line: its page, its letters, and the baseline of its first letter from the page's top. */
    record Line(int page, String key, double baseline) {
    }

    private final List<Line> lines;
    private final int pages;

    private PdfLines(List<Line> lines, int pages) {
        this.lines = lines;
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

    /** The lines by page and key, the first of each kept. */
    Map<String, Line> byKey() {
        Map<String, Line> keyed = new LinkedHashMap<>();
        for (Line line : lines) {
            keyed.putIfAbsent(line.page() + "/" + line.key(), line);
        }
        return keyed;
    }

    private static final class Collector extends PDFTextStripper {

        private final List<Line> lines = new ArrayList<>();
        private final StringBuilder key = new StringBuilder();
        private int page;
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
            double x = glyph.getXDirAdj();
            double size = Math.max(1, glyph.getFontSizeInPt());
            boolean sameLine = !Double.isNaN(baseline)
                               && Math.abs(y - baseline) < size * 0.5
                               && x >= lastX - 1
                               && x - lastEnd < size * 1.5;
            if (!sameLine) {
                endLine();
                baseline = y;
            }
            String letters = glyph.getUnicode();
            if (letters != null) {
                for (int i = 0; i < letters.length(); i++) {
                    char c = letters.charAt(i);
                    if (!Character.isWhitespace(c) && c != ' ') {
                        key.append(c);
                    }
                }
            }
            lastX = x;
            lastEnd = x + glyph.getWidthDirAdj();
        }

        private void endLine() {
            String letters = key.toString().toLowerCase(Locale.ROOT);
            if (letters.codePointCount(0, letters.length()) >= 3) {
                lines.add(new Line(page, letters, baseline));
            }
            key.setLength(0);
            baseline = Double.NaN;
            lastEnd = Double.NaN;
            lastX = Double.NaN;
        }
    }
}
