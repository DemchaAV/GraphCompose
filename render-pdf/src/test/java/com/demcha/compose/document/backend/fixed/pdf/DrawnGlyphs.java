package com.demcha.compose.document.backend.fixed.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * What a page actually drew: each glyph, where it landed, and what it stands for.
 *
 * <p>The obvious way to ask that is {@link TextPosition#getUnicode()}, and it stops working
 * on a reordered line. Such a line carries its own text as {@code ActualText}, and a reader
 * that honours it — PDFBox does — reports that string against the line's first glyph and
 * nothing against the rest, because the point of {@code ActualText} is that it stands in
 * for the marks rather than describing them one by one.</p>
 *
 * <p>The character each glyph stands for is still in the file, in the font's own map, which
 * is where this reads it. That is also the more direct question for a test about drawing:
 * {@code getUnicode} is what an extractor made of the page, and this is what the page
 * says.</p>
 */
final class DrawnGlyphs {

    private DrawnGlyphs() {
    }

    /** One drawn glyph: what it stands for, and the box it occupies. */
    record Glyph(String character, double left, double right, double baselineY) {

        /** Whether this glyph stands for a single character a predicate accepts. */
        boolean is(IntPredicate accepts) {
            return !character.isBlank() && accepts.test(character.codePointAt(0));
        }

        @Override
        public String toString() {
            return String.format("%s@%.1f", character, left);
        }
    }

    /**
     * Every glyph on the page, ordered left to right.
     *
     * <p>Ordered by position rather than by when it was painted, so the sequence is what a
     * reader sees rather than what the engine did — which is the thing a reordered line is
     * supposed to change.</p>
     *
     * @param pdf a rendered document
     * @return the page's glyphs
     * @throws IOException if the document cannot be read
     */
    static List<Glyph> leftToRight(byte[] pdf) throws IOException {
        List<Glyph> glyphs = new ArrayList<>();
        for (List<Glyph> line : byLine(pdf)) {
            glyphs.addAll(line);
        }
        glyphs.sort(Comparator.comparingDouble(Glyph::left));
        return glyphs;
    }

    /** The page's glyphs as the extractor grouped them into lines, in painting order. */
    static List<List<Glyph>> byLine(byte[] pdf) throws IOException {
        List<List<Glyph>> lines = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> positions) throws IOException {
                    List<Glyph> line = new ArrayList<>(positions.size());
                    for (TextPosition position : positions) {
                        line.add(new Glyph(
                                meaningOf(position),
                                position.getXDirAdj(),
                                position.getXDirAdj() + position.getWidthDirAdj(),
                                position.getYDirAdj()));
                    }
                    lines.add(line);
                    super.writeString(text, positions);
                }
            }.getText(document);
        }
        return lines;
    }

    /** The concatenated characters of every glyph, in the order they sit on the page. */
    static String readLeftToRight(byte[] pdf) throws IOException {
        StringBuilder drawn = new StringBuilder();
        for (Glyph glyph : leftToRight(pdf)) {
            drawn.append(glyph.character());
        }
        return drawn.toString().trim();
    }

    /** Every glyph standing for a character the predicate accepts. */
    static List<Glyph> matching(byte[] pdf, IntPredicate accepts) throws IOException {
        List<Glyph> hits = new ArrayList<>();
        for (Glyph glyph : leftToRight(pdf)) {
            if (glyph.is(accepts)) {
                hits.add(glyph);
            }
        }
        return hits;
    }

    /**
     * What one drawn glyph stands for, according to the font that drew it.
     *
     * <p>A position may cover more than one code — a font is free to draw several at once —
     * so all of them are read, which is also how a ligature standing for two letters comes
     * back as both.</p>
     */
    private static String meaningOf(TextPosition position) throws IOException {
        StringBuilder characters = new StringBuilder();
        for (int code : position.getCharacterCodes()) {
            String character = position.getFont().toUnicode(code);
            if (character != null) {
                characters.append(character);
            }
        }
        return characters.toString();
    }
}
