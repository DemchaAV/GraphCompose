package com.demcha.compose.engine.render.pdf;

import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.font.FontBase;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

@Slf4j
@Accessors(fluent = true)
public class PdfFont extends FontBase<PDFont> {


    public PdfFont(PDFont defaultFont, PDFont bold, PDFont italic, PDFont boldItalic, PDFont underline, PDFont strikethrough) {
        super(defaultFont, bold, italic, boldItalic, underline, strikethrough);
    }

    public PdfFont(PDFont defaultFont, PDFont bold, PDFont italic, PDFont boldItalic) {
        super(defaultFont, bold, italic, boldItalic);
    }


    // Adjust font size automatically based on the width of the text and available space
    public TextStyle adjustFontSizeToFit(String text, TextStyle style, double availableWidth) {
        double textWidth = getTextWidth(style, text);

        // If textWidth exceeds availableWidth, reduce the font size
        double newSize = style.size();
        while (textWidth > availableWidth && newSize > 1) {
            newSize--;  // Reduce size
            textWidth = getTextWidth(style, text);  // Recalculate text width
        }
        PDFont pdFont = fontType(style.decoration());
        return new TextStyle(style.fontName(), newSize, style.decoration(), style.color());
    }

    public VerticalMetrics verticalMetrics(TextStyle style) {
        PDFont pdfFont = fontType(style.decoration());
        PDFontDescriptor descriptor = pdfFont.getFontDescriptor();
        BoundingBox boundingBox = boundingBox(pdfFont);

        double ascent = (descriptor != null ? descriptor.getAscent() : boundingBox.getUpperRightY()) * scale(style.size());
        double descent = Math.abs((descriptor != null ? descriptor.getDescent() : boundingBox.getLowerLeftY()) * scale(style.size()));
        double leading = (descriptor != null ? descriptor.getLeading() : 0) * scale(style.size());
        double lineHeight = ascent + descent + leading;

        VerticalMetrics metrics = new VerticalMetrics(ascent, descent, leading, lineHeight);
        log.debug("Resolved PDF vertical metrics for font={} size={}: {}",
                pdfFont.getName(), style.size(), metrics);
        return metrics;
    }

    /**
     * Returns a stable font identity for text measurement caches.
     *
     * @param style style selecting the concrete font variant
     * @return backend font name used for width and metric calculations
     */
    public String measurementCacheKey(TextStyle style) {
        return fontType(style.decoration()).getName();
    }

    public double getTextHeight(TextStyle style) {
        double size = style.size();
        try {
            float v = fontType(style.decoration()).getBoundingBox().getHeight() / 1000 * (float) size;
            log.debug("Measured PDF text bounding-box height: {}", v);
            return v;
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Error while getting text height {}", e.getMessage(), e);
            return 0;  // Return 0 if something goes wrong
        }
    }

    /**
     * Measures the rendered width of {@code text} in {@code style}'s font.
     *
     * <p>Width is measured against the same string the PDF render path will
     * actually emit — that is, after {@link #sanitizeForRender(TextStyle, String)}
     * replaces any glyph the selected font cannot encode. Keeping width
     * measurement and render in lockstep prevents wrap geometry from
     * drifting when input contains characters outside the font's coverage
     * (arrows, dots, emoji, custom unicode).</p>
     *
     * @param style style selecting the concrete font variant
     * @param text raw text from the document model
     * @return rendered width in points
     */
    @Override
    public double getTextWidth(TextStyle style, String text) {
        if (text == null || text.isEmpty()) return 0;

        double size = style.size();

        try {
            // Preserve all-whitespace runs exactly (used by wrapping for
            // leading/trailing space accounting); sanitize the rest so
            // width matches the bytes actually shown by the renderer.
            boolean whitespaceOnly = text.chars().allMatch(Character::isWhitespace);
            String measured = whitespaceOnly ? text : sanitizeForRender(style, text);

            double width = fontType(style.decoration()).getStringWidth(measured) / 1000d * size;
            return width;
        } catch (Exception e) {
            log.error("Error while getting text width {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Sanitises {@code text} for safe rendering with the font selected by
     * {@code style}. Applies the standard control-character cleanup that
     * {@code getTextWidth} has always done, then substitutes any code point
     * the resolved font cannot encode with {@code '?'}.
     *
     * <p>This is the single entry point shared by the PDF render path
     * (paragraphs, tables, watermarks, header/footer) and the width
     * measurement path. Calling it everywhere keeps wrap geometry
     * consistent with the bytes that are actually drawn on the page,
     * and prevents {@link PDFont#encode(String)} from throwing on
     * characters outside the font's coverage (arrows ↦ U+2192, bullets
     * ↦ U+25CF, emoji, custom unicode).</p>
     *
     * @param style style selecting the concrete font variant
     * @param text  raw text from the document model; {@code null} or empty
     *              is returned unchanged
     * @return text safe to pass to
     *         {@code PDPageContentStream.showText(...)}
     */
    public String sanitizeForRender(TextStyle style, String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }
        String cleaned = textSanitizer(text);
        PDFont font = fontType(style.decoration());
        return sanitizeByFont(font, cleaned);
    }

    /**
     * Replaces every code point in {@code s} that {@code font} cannot
     * encode with {@code '?'}. Newlines are dropped (the renderer handles
     * line breaks at a higher layer); spaces are preserved.
     *
     * <p>Public so render handlers in sibling packages can sanitise text
     * against a specific {@link PDFont} when the active text style is
     * already known. Most callers should prefer
     * {@link #sanitizeForRender(TextStyle, String)} which resolves the
     * font from a {@link TextStyle} and applies the standard control
     * cleanup first.</p>
     *
     * @param font font to validate glyph coverage against
     * @param s    text to sanitise
     * @return text containing only code points the font can encode
     */
    public String sanitizeByFont(PDFont font, String s) {
        StringBuilder sb = new StringBuilder(s.length());
        s.codePoints().forEach(cp -> {
            // keep spaces/newlines logic correct for wrapping
            if (cp == '\n' || cp == '\r') return;

            String ch = new String(Character.toChars(cp));
            if (canEncode(font, ch)) {
                sb.append(ch);
            } else {
                GlyphFallbackLogger.report(font, cp);
                sb.append('?');
            }
        });
        return sb.toString();
    }

    private boolean canEncode(PDFont font, String ch) {
        try {
            font.encode(ch);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public double getTextWidthNoSanitize(TextStyle style, String text) {
        double size = style.size();
        try {
            float width = fontType(style.decoration()).getStringWidth(text) / 1000 * (float) size;
            log.debug("Getting text width: " + width);
            return width;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error while getting text width {}", e.getMessage(), e);
            return 0;  // Return 0 if something goes wrong
        }
    }

    private @NotNull String textSanitizer(String text) {
        StringBuilder sanitized = new StringBuilder(text.length());
        boolean previousSpace = false;
        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            offset += Character.charCount(codePoint);

            int resolved = switch (codePoint) {
                case '\r', '\n', '\u00A0' -> ' ';
                default -> Character.isISOControl(codePoint) && codePoint != '\t' ? ' ' : codePoint;
            };

            if (resolved == ' ') {
                if (!previousSpace) {
                    sanitized.append(' ');
                    previousSpace = true;
                }
                continue;
            }

            sanitized.appendCodePoint(resolved);
            previousSpace = false;
        }

        return sanitized.toString();
    }

    /**
     * Line height based on font metrics (recommended for baseline-to-baseline step).
     * Uses ascent, descent (usually negative), and optional leading if present.
     */
    public double getLineHeight(TextStyle style) {
        return verticalMetrics(style).lineHeight();
    }


    /**
     * Visual height of capitals (useful for tight boxes behind text like buttons/titles).
     */
    @Override
    public double getCapHeight(TextStyle style) {
        PDFontDescriptor fd = defaultFont().getFontDescriptor();
        float cap = (fd != null ? fd.getCapHeight() : 0);
        if (cap == 0) { // fallback: approximate via bbox
            try {
                return defaultFont().getBoundingBox().getHeight() * 0.7f * scale(style.size());
            } catch (IOException e) {
                log.error("Error while getting text height with default font {}", e.getMessage(), e);
                throw new RuntimeException("Error while getting text height ", e);
            }
        }
        return cap * scale(style.size());
    }

    // Scale once: PDF units are per 1000 EM
    public double scale(double size) {
        return (float) size / 1000f;
    }

    /**
     * Tight per-string bounds (slow but exact for hit-areas/links).
     * Computes the glyph path bounds for THIS string.
     */
    public ContentSize getTightBounds(String text, TextStyle style) {
        if (text == null || text.isEmpty()) return new ContentSize(0, 0);
        GeneralPath path = defaultFont().getFontDescriptor().getFontBoundingBox().toGeneralPath();
        AffineTransform at = AffineTransform.getScaleInstance(scale(style.size()), scale(style.size()));
        Shape s = at.createTransformedShape(path);
        Rectangle2D bounds2D = s.getBounds2D();
        var contentSize = new ContentSize(bounds2D.getWidth(), bounds2D.getHeight());

        return contentSize; // width/height in user units; y is relative to baseline
    }

    private BoundingBox boundingBox(PDFont pdfFont) {
        try {
            return pdfFont.getBoundingBox();
        } catch (IOException e) {
            log.error("Error while getting bounding box for font {}", pdfFont, e);
            throw new RuntimeException(e);
        }
    }

    public record VerticalMetrics(double ascent, double descent, double leading, double lineHeight) {
        public double baselineOffsetFromBottom() {
            return descent;
        }
    }

}
