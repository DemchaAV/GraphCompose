package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.layout.payloads.ParagraphSpan;
import com.demcha.compose.document.layout.payloads.ParagraphTextSpan;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.font.FontLibrary;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDVectorFont;

import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * How far a line's letters reach above and below its baseline: the outlines of its glyphs, in
 * the fonts the layout measured it with.
 *
 * <p>Word draws the text of an exact line on screen only inside the line: a letter reaching past
 * its top or foot is cut off there, though its PDF export draws it whole. A face's ascent and
 * descent are the room its tallest and deepest glyphs could need; the letters of one line need
 * less, and lines a title sets closer than its face's line fit only by what they hold.</p>
 */
final class DocxInk {

    private DocxInk() {
    }

    /**
     * The reach of a line's letters, in points: {@code {above, below}} its baseline, each at
     * least 0; {@code null} when a span is not text, a font is not known or a glyph has no
     * outline to read.
     *
     * @param line  the laid-out line
     * @param fonts the fonts the layout measured it with
     */
    static double[] of(ParagraphLine line, FontLibrary fonts) {
        double above = 0;
        double below = 0;
        for (ParagraphSpan span : line.spans()) {
            if (!(span instanceof ParagraphTextSpan text)) {
                return null;
            }
            PdfFont font = fonts.getFont(text.textStyle().fontName(), PdfFont.class).orElse(null);
            if (font == null) {
                return null;
            }
            PDFont face = font.fontType(text.textStyle().decoration());
            if (!(face instanceof PDVectorFont outlines)) {
                return null;
            }
            double scale = text.textStyle().size() / 1000.0;
            String shown = font.sanitizeForRender(text.textStyle(), text.text());
            for (int i = 0; i < shown.length(); ) {
                int codePoint = shown.codePointAt(i);
                i += Character.charCount(codePoint);
                if (Character.isWhitespace(codePoint)) {
                    continue;
                }
                try {
                    byte[] bytes = face.encode(new String(Character.toChars(codePoint)));
                    int code = face.readCode(new ByteArrayInputStream(bytes));
                    Rectangle2D bounds = outlines.getNormalizedPath(code).getBounds2D();
                    if (bounds.isEmpty()) {
                        continue;
                    }
                    above = Math.max(above, bounds.getMaxY() * scale);
                    below = Math.max(below, -bounds.getMinY() * scale);
                } catch (IOException | IllegalArgumentException unreadable) {
                    return null;
                }
            }
        }
        return new double[]{above, below};
    }
}
