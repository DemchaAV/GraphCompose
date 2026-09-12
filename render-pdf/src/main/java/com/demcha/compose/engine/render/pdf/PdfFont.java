package com.demcha.compose.engine.render.pdf;

import com.demcha.compose.document.api.Internal;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.font.FontBase;
import com.demcha.compose.engine.font.FontLineMetrics;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

/**
 * PDFBox font handle: loading, embedding, width and vertical metrics for one family.
 *
 * <p><strong>Internal API</strong>, like the rest of this package &mdash;
 * {@code docs/api-stability.md} allows it to change in any release. Note the tension
 * this marker exposes: {@code PdfRenderEnvironment.fonts()} is public on the render
 * handler SPI, and a handler that draws text has to reach a {@code PdfFont} through it.
 * Until that seam gets a supported accessor, a third-party handler depends on this type
 * at its own risk.</p>
 */
@Internal
@Slf4j
@Accessors(fluent = true)
public class PdfFont extends FontBase<PDFont> {


    public PdfFont(PDFont defaultFont, PDFont bold, PDFont italic, PDFont boldItalic, PDFont underline, PDFont strikethrough) {
        super(defaultFont, bold, italic, boldItalic, underline, strikethrough);
    }

    public PdfFont(PDFont defaultFont, PDFont bold, PDFont italic, PDFont boldItalic) {
        super(defaultFont, bold, italic, boldItalic);
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
     * Bridges the PDFBox-derived {@link VerticalMetrics} to the backend-neutral
     * {@link FontLineMetrics} the shared text-measurement system consumes, so the
     * measurement system resolves PDF line metrics polymorphically rather than via
     * an {@code instanceof PdfFont} special case.
     *
     * @param style the resolved text style
     * @return ascent, descent, and leading in document units
     */
    @Override
    public FontLineMetrics lineMetrics(TextStyle style) {
        VerticalMetrics metrics = verticalMetrics(style);
        return new FontLineMetrics(metrics.ascent(), metrics.descent(), metrics.leading());
    }

    /**
     * Returns a stable font identity for text measurement caches.
     *
     * @param style style selecting the concrete font variant
     * @return backend font name used for width and metric calculations
     */
    @Override
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
     * <p>The style's tracking is included, on the same string, by the rule the
     * PDF {@code Tc} operator was measured to follow &mdash; see
     * {@link #trackingAdvance(TextStyle, String)}. Width and pen advance are the
     * same number or every span after the first on a line is drawn somewhere
     * other than where the layout thinks it is.</p>
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
            return width + trackingAdvance(style, measured);
        } catch (Exception e) {
            log.error("Error while getting text width {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * The advance a style's tracking adds to {@code measured}, in points.
     *
     * <p><strong>One unit per code point, trailing unit included</strong> —
     * measured against PDFBox 3.0.8 rather than read off the specification or
     * borrowed from CSS. Drawing a string at {@code Tc = 5} and reading where
     * the pen actually landed: {@code "JANE"} advances 20pt further (4 code
     * points), {@code "JANE DOE"} 40pt (8 — the space is a code point like any
     * other), a single {@code "J"} 5pt, and the empty string not at all. The
     * trailing unit is real: the pen sits one full unit past the last glyph's
     * ink, which is why this counts N and not N-1.
     * {@code PdfCharacterSpacingContractTest} re-measures this and fails if
     * PDFBox ever changes it.</p>
     *
     * <p>Counted in <em>code points</em> of the string that is actually drawn,
     * never in {@code char}s: {@code Tc} is applied once per glyph, and a
     * supplementary code point is one glyph out of two {@code char}s. (With the
     * bundled faces the distinction is currently unobservable — none of them can
     * encode a supplementary code point, so {@code sanitizeForRender} folds one
     * to {@code '?'} before it ever reaches here. Counting code points is what
     * stays correct on the day a face that can encode one is added.)</p>
     *
     * <p>The result is not clamped. Negative tracking is allowed, and the pen in
     * a reader genuinely moves backwards by it; clamping the measurement while
     * being unable to clamp the reader is how the two stop agreeing.</p>
     *
     * @param style    the style whose tracking to apply, already resolved to points
     * @param measured the exact string handed to {@code showText}
     * @return the extra advance in points, {@code 0} when there is no tracking
     */
    private static double trackingAdvance(TextStyle style, String measured) {
        double spacing = style.letterSpacing();
        if (spacing == 0.0 || measured == null || measured.isEmpty()) {
            // Short-circuited rather than added as a zero, so an untracked
            // style returns the identical double it returned before tracking
            // existed.
            return 0.0;
        }
        return measured.codePointCount(0, measured.length()) * spacing;
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
        return GlyphFallbackLogger.sanitize(font, s);
    }

    /**
     * Sanitizes for a backend that emits text and shapes it itself.
     *
     * <p>Same substitution policy as {@link #sanitizeForRender(TextStyle, String)}, except
     * that the Arabic joining controls survive: they are an instruction to the consumer's
     * own shaper, not something to draw.</p>
     *
     * @param style style whose decoration picks the face
     * @param text raw text from the layout span
     * @return text the face can encode, keeping the Arabic joining controls
     * @since 2.2.0
     */
    public String sanitizeForTextExport(TextStyle style, String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }
        return GlyphFallbackLogger.sanitizeKeepingJoiningControls(
                fontType(style.decoration()), textSanitizer(text));
    }

    /**
     * Sanitizes for a backend that resolves the direction itself, as well as the shaping.
     *
     * <p>As {@link #sanitizeForTextExport(TextStyle, String)}, except that the direction
     * marks and isolates survive too. The difference is what the consumer has been left to
     * do: a span handed to PowerPoint has already been ordered by the engine, so its bidi
     * controls have been read and can go, while a table cell is handed over as a whole
     * logical line for PowerPoint's own algorithm to resolve — which makes them part of its
     * input. Dropping them there deletes the author's only way to say which direction a
     * neutral stretch of text belongs to.</p>
     *
     * @param style style whose decoration picks the face
     * @param text raw text in logical order
     * @return text the face can encode, keeping every formatting control
     * @since 2.2.0
     */
    public String sanitizeForLogicalTextExport(TextStyle style, String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }
        return GlyphFallbackLogger.sanitizeKeepingFormattingControls(
                fontType(style.decoration()), textSanitizer(text));
    }

    /**
     * Measures {@code text} exactly as given, for a caller that has already
     * sanitised it.
     *
     * <p>Tracking is applied here too, by the same rule and on the same string,
     * so the two entry points cannot disagree about the width of one run. There
     * is no double application: this does not delegate to
     * {@link #getTextWidth(TextStyle, String)}, and that one does not delegate
     * here &mdash; each adds the tracking once, to the string it measured.</p>
     *
     * @param style style selecting the concrete font variant
     * @param text  already-sanitised text
     * @return rendered width in points
     */
    public double getTextWidthNoSanitize(TextStyle style, String text) {
        double size = style.size();
        try {
            float width = fontType(style.decoration()).getStringWidth(text) / 1000 * (float) size;
            log.debug("Getting text width: " + width);
            return width + trackingAdvance(style, text);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error while getting text width {}", e.getMessage(), e);
            return 0;  // Return 0 if something goes wrong
        }
    }

    private String textSanitizer(String text) {
        // v1.6.3: preserve author-supplied whitespace verbatim. The
        // previous implementation collapsed any run of resulting spaces
        // (original + converted) into one, but downstream geometry
        // (`PdfFont.getTextWidth`, paragraph layout, link-rect emission)
        // measures against the input as written. The collapse therefore
        // shrank the rendered string under measurement, drifting
        // link annotations away from their glyphs and visually merging
        // any text whose author put more than one space in it on purpose.
        // (The case that first showed this was the templates' old
        // spaced-caps transform, which padded a name out to
        // "A R T E M   D E M C H Y S H Y N". That is gone — tracking is a
        // style now — but the rule it exposed is not about that transform:
        // a run of author spaces is content, and measurement and drawing
        // have to agree on how wide it is.)
        // Newlines / NBSP / non-tab control chars still resolve to a
        // single space each \u2014 they no longer collapse adjacent author
        // spaces.
        StringBuilder sanitized = new StringBuilder(text.length());
        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            offset += Character.charCount(codePoint);

            int resolved = switch (codePoint) {
                case '\r', '\n', '\u00A0' -> ' ';
                default -> Character.isISOControl(codePoint) && codePoint != '\t' ? ' ' : codePoint;
            };

            sanitized.appendCodePoint(resolved);
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
