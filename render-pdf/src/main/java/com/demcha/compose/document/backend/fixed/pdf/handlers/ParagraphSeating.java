package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.api.Internal;
import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.layout.payloads.ParagraphSpan;
import com.demcha.compose.document.layout.payloads.ParagraphTextSpan;
import com.demcha.compose.document.node.TextVerticalAlign;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.font.FontLibrary;

/**
 * Where a paragraph's line seats its text by its cap band, off the baseline the layout gave it.
 *
 * <p>Shared by the PDF and PPTX backends, which draw the line there, and the DOCX backend, which
 * raises the same text in a Word line by as much — so the three cannot drift apart.</p>
 */
@Internal
public final class ParagraphSeating {

    private ParagraphSeating() {
    }

    /**
     * Baseline correction that seats a line by its cap band within the line box, used for the
     * non-default {@link TextVerticalAlign} modes. Derived purely from font metrics — no magic
     * offset — so it scales with font size:
     *
     * <ul>
     *   <li>{@code TOP} — raise the cap top to the line-box top
     *       ({@code ascent + leading - capHeight}).</li>
     *   <li>{@code CENTER} — centre the cap band {@code [baseline, baseline + capHeight]}
     *       on the line-box middle (the midpoint of {@code TOP} and {@code BOTTOM}).</li>
     *   <li>{@code BOTTOM} — lower the baseline to the line-box bottom
     *       ({@code -descent}); descenders extend below the box.</li>
     * </ul>
     *
     * <p>The cap height is read from the line's first text span; an image-only line is left
     * untouched.</p>
     *
     * @param line  the measured line
     * @param fonts the fonts the line was measured with
     * @param align how the paragraph seats its text
     * @return points to add to the baseline Y (positive raises the text)
     */
    public static double shift(ParagraphLine line, FontLibrary fonts, TextVerticalAlign align) {
        for (ParagraphSpan span : line.spans()) {
            if (span instanceof ParagraphTextSpan textSpan) {
                PdfFont font = fonts.getFont(textSpan.textStyle().fontName(), PdfFont.class).orElse(null);
                if (font == null) {
                    return 0.0;
                }
                double capHeight = font.getCapHeight(textSpan.textStyle());
                double ascent = line.textAscent();
                double descent = line.baselineOffsetFromBottom();
                double leading = Math.max(0.0, line.textLineHeight() - ascent - descent);
                double capTopToBoxTop = ascent + leading - capHeight;
                return switch (align) {
                    case TOP -> capTopToBoxTop;
                    case CENTER -> (capTopToBoxTop - descent) / 2.0;
                    case BOTTOM -> -descent;
                    case DEFAULT -> 0.0;
                };
            }
        }
        return 0.0;
    }
}
