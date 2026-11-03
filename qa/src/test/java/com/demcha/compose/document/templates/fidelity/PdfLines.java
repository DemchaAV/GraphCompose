package com.demcha.compose.document.templates.fidelity;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The lines of text a PDF draws and does not clip away, each with its page, its letters, and
 * where its first letter stands.
 *
 * <p>Glyphs are read in the order the page draws them. A line ends where the next glyph stands
 * on another baseline (more than half its size off), steps back to the left, or stands further
 * off than one and a half times its size: two runs of text on one baseline are two lines when
 * the second starts that far past the first, as two columns' text does. The key is the line's
 * letters without white space, lower-cased, so text an editor sets with spaces between tracked
 * letters, or with a trailing space, keys as the page's does. Lines of fewer than three letters
 * are left out.</p>
 *
 * <p>A glyph is read only where the page's clip lets it show: one whose middle stands outside the
 * clipping path in force when it is drawn is left out, as a reader never sees it. A PDF keeps the
 * text it clips: Word writes every line of a table row held to an exact height its text does not
 * fit, and the end of a line it runs past its cell, and clips them away. Read whole, those lines
 * are found where the page has them. Only a path's clip is followed: a glyph drawn invisible
 * (rendering mode 3), or painted over, is still read.</p>
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
        private List<Boolean> shown = List.of();
        private int drawn;

        private Collector() throws IOException {
        }

        @Override
        protected void startPage(PDPage pdPage) throws IOException {
            endLine();
            page = getCurrentPageNo() - 1;
            Shown visibility = new Shown(pdPage);
            visibility.processPage(pdPage);
            shown = visibility.shown;
            drawn = 0;
            super.startPage(pdPage);
        }

        @Override
        protected void endPage(PDPage pdPage) throws IOException {
            // Both readings walk the page's content in the order it draws; a count that differs
            // means they did not see the same glyphs, and none of the visibility read can be trusted.
            if (drawn != shown.size()) {
                throw new IllegalStateException("page " + (page + 1) + ": the text read " + drawn
                                                + " glyphs where the page draws " + shown.size());
            }
            super.endPage(pdPage);
        }

        @Override
        protected void showGlyph(Matrix trm, PDFont font, int code, Vector displacement) throws IOException {
            if (drawn >= shown.size()) {
                throw new IllegalStateException("page " + (page + 1) + ": the text read more glyphs than the page "
                                                + "draws (" + shown.size() + ")");
            }
            if (shown.get(drawn++)) {
                super.showGlyph(trm, font, code, displacement);
            }
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

    /**
     * Whether each glyph a page draws, in the order it draws them, shows: its middle — half its
     * advance along the baseline, three tenths of its size up — stands inside the clipping path
     * in force when it is drawn. The page's paths are built here only to be clipped by: a path is
     * dropped once painted, after any clip marked on it is applied.
     */
    static final class Shown extends PDFGraphicsStreamEngine {

        final List<Boolean> shown = new ArrayList<>();
        private final GeneralPath path = new GeneralPath();
        private int clipRule = -1;

        Shown(PDPage page) {
            super(page);
        }

        @Override
        protected void showGlyph(Matrix trm, PDFont font, int code, Vector displacement) {
            Point2D.Float middle = trm.transformPoint(displacement.getX() / 2, 0.3f);
            shown.add(getGraphicsState().getCurrentClippingPath().contains(middle.x, middle.y));
        }

        @Override
        public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) {
            path.moveTo((float) p0.getX(), (float) p0.getY());
            path.lineTo((float) p1.getX(), (float) p1.getY());
            path.lineTo((float) p2.getX(), (float) p2.getY());
            path.lineTo((float) p3.getX(), (float) p3.getY());
            path.closePath();
        }

        @Override
        public void clip(int windingRule) {
            // W marks the path being built as a clip; it takes effect when the path is painted
            // or ended, as the PDF's painting operators define.
            clipRule = windingRule;
        }

        @Override
        public void moveTo(float x, float y) {
            path.moveTo(x, y);
        }

        @Override
        public void lineTo(float x, float y) {
            path.lineTo(x, y);
        }

        @Override
        public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
            path.curveTo(x1, y1, x2, y2, x3, y3);
        }

        @Override
        public Point2D getCurrentPoint() {
            return path.getCurrentPoint();
        }

        @Override
        public void closePath() {
            path.closePath();
        }

        @Override
        public void endPath() {
            painted();
        }

        @Override
        public void strokePath() {
            painted();
        }

        @Override
        public void fillPath(int windingRule) {
            painted();
        }

        @Override
        public void fillAndStrokePath(int windingRule) {
            painted();
        }

        @Override
        public void drawImage(PDImage pdImage) {
        }

        @Override
        public void shadingFill(COSName shadingName) {
        }

        private void painted() {
            if (clipRule != -1) {
                path.setWindingRule(clipRule);
                getGraphicsState().intersectClippingPath(path);
                clipRule = -1;
            }
            path.reset();
        }
    }
}
