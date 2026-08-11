package com.demcha.compose.engine.render.pdf;

import com.demcha.compose.engine.text.TextControlSanitizer;
import com.demcha.compose.engine.text.bidi.ArabicShaper;

import org.apache.pdfbox.pdmodel.font.PDFont;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Emits a single WARN per unique {@code (font, codePoint)} pair the PDF
 * glyph sanitizer had to substitute. Used by
 * {@link PdfFont#sanitizeByFont(PDFont, String)} so document authors see
 * exactly which characters were swapped for {@code '?'} during rendering
 * without flooding the log when the same character appears many times.
 *
 * <p>Deduplication is JVM-scoped via a {@link ConcurrentHashMap}-backed
 * set. The cache is intentionally never invalidated — a substitution is
 * a property of the loaded font, and within one process the same
 * {@code (font, codePoint)} pair will always be missing, so a single
 * lifetime warning is the right cadence.</p>
 *
 * <p>Logger category: {@code com.demcha.compose.engine.render.pdf.glyph-fallback}.
 * Set to DEBUG in {@code logback-test.xml} to inspect every substitution
 * (not just first-of-kind) during a test run.</p>
 *
 * @author Artem Demchyshyn
 */
public final class GlyphFallbackLogger {

    private static final Logger LOG = LoggerFactory.getLogger(
            "com.demcha.compose.engine.render.pdf.glyph-fallback");

    /**
     * Set of {@code (fontName, codePoint)} pairs already warned about.
     * Packed into a single {@code long}: upper 32 bits hold the font name
     * hash, lower 32 bits hold the code point. Collisions on font name
     * hash are possible but harmless — at worst one substitution is
     * silently coalesced with an unrelated font.
     */
    private static final Set<Long> SEEN = ConcurrentHashMap.newKeySet();

    /**
     * Fonts already reported as lacking the Arabic presentation forms. Separate from
     * {@link #SEEN} because this fact is per font, not per code point.
     */
    private static final Set<String> DEGRADED_FONTS = ConcurrentHashMap.newKeySet();

    /**
     * Per-font glyph-coverage memo: a font's PostScript base name to the set of
     * code points it can ({@code true}) or cannot ({@code false}) encode.
     *
     * <p>Glyph coverage is an immutable property of the loaded font program, so
     * the first {@link PDFont#encode(String)} result for a {@code (font, code
     * point)} pair holds for the lifetime of the process. Memoizing it turns the
     * heavy probe — which also throws an exception for every unencodable glyph —
     * into a map lookup, so {@code encode} runs once per <em>distinct</em>
     * {@code (font, code point)} instead of once per glyph occurrence on every
     * measurement and render pass. Two {@code PDType0Font} instances of the same
     * embedded font share a base name (the subset prefix is only added at save,
     * after sanitisation), so the measurement font and each render font reuse the
     * same memo. Bounded in practice by (distinct fonts × distinct code points
     * actually drawn).</p>
     */
    private static final Map<String, Map<Integer, Boolean>> ENCODABLE_BY_FONT = new ConcurrentHashMap<>();

    private GlyphFallbackLogger() {
    }

    /**
     * Records a glyph substitution. Emits one WARN if the
     * {@code (font, codePoint)} pair has not been seen before, no-op
     * otherwise.
     *
     * @param font      the font that could not encode the code point;
     *                  {@code null} treated as {@code "<null>"}
     * @param codePoint the Unicode code point that was substituted
     */
    public static void report(PDFont font, int codePoint) {
        String fontName = fontKey(font);
        long key = ((long) fontName.hashCode() << 32) | (codePoint & 0xFFFFFFFFL);
        if (SEEN.add(key)) {
            LOG.warn("glyph.missing font={} codePoint=U+{} replaced='?'",
                    fontName, String.format("%04X", codePoint));
        }
    }

    /**
     * Sanitises {@code text} against {@code font}, substituting every
     * code point the font cannot encode with {@code '?'} and reporting
     * each substitution through {@link #report(PDFont, int)}. Newlines
     * are dropped (the renderer handles line breaks at a higher layer);
     * spaces are preserved.
     *
     * <p>Static convenience for render helpers that hold a raw
     * {@link PDFont} (watermarks, header/footer chrome) and have no
     * {@code PdfFont} wrapper handy. {@link PdfFont#sanitizeByFont(PDFont, String)}
     * delegates here so both paths use the exact same substitution
     * policy and produce identical bytes.</p>
     *
     * @param font font to validate glyph coverage against
     * @param text raw text to sanitise; {@code null} returns empty
     * @return text containing only code points the font can encode
     */
    public static String sanitize(PDFont font, String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }
        Map<Integer, Boolean> coverage = ENCODABLE_BY_FONT.computeIfAbsent(fontKey(font), key -> new ConcurrentHashMap<>());
        StringBuilder sb = new StringBuilder(text.length());
        int length = text.length();
        for (int offset = 0; offset < length; ) {
            int codePoint = text.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (codePoint == '\n' || codePoint == '\r'
                    || TextControlSanitizer.isFormattingControl(codePoint)) {
                // A formatting character steers the layout and draws nothing —
                // bidirectional controls steer the direction, joining controls the
                // shaping. The shaper consumes its own, but only when it runs at all,
                // so text with no Arabic in it still arrives carrying them.
                // Substituting either would put a visible '?' on the page and give a
                // zero-width character a width, which the wrapping already committed to
                // not having. Dropping it keeps measurement and rendering in step.
                continue;
            }
            if (isEncodable(font, coverage, codePoint)) {
                sb.appendCodePoint(codePoint);
            } else if (!appendBaseLetters(font, coverage, codePoint, sb)) {
                report(font, codePoint);
                sb.append('?');
            }
        }
        return sb.toString();
    }

    /**
     * Degrades an Arabic presentation form a font cannot encode to its base letters.
     *
     * <p>A font that shapes only through {@code GSUB} carries the letters but not the
     * forms. Substituting {@code '?'} would lose the text; the base letters render
     * unjoined but readable, which is the honest middle. Only fires when every base
     * letter is itself encodable — otherwise the ordinary substitution reports it.</p>
     */
    private static boolean appendBaseLetters(PDFont font,
                                             Map<Integer, Boolean> coverage,
                                             int codePoint,
                                             StringBuilder sb) {
        String base = ArabicShaper.baseLettersOf(codePoint);
        if (base == null) {
            return false;
        }
        for (int index = 0; index < base.length(); index++) {
            if (!isEncodable(font, coverage, base.charAt(index))) {
                return false;
            }
        }
        sb.append(base);
        String fontName = fontKey(font);
        // Keyed on the font alone rather than on (font, code point): a font that carries
        // no presentation forms carries none of them, so the second form says nothing the
        // first did not. Per-form keying turned one fact about the font into a warning for
        // every distinct letter the document happened to use.
        if (DEGRADED_FONTS.add(fontName)) {
            LOG.warn("glyph.degraded font={} joined forms unavailable, drawing base "
                            + "letters — Arabic renders unjoined in this font (first at "
                            + "U+{})",
                    fontName, String.format("%04X", codePoint));
        }
        return true;
    }

    private static boolean isEncodable(PDFont font, Map<Integer, Boolean> coverage, int codePoint) {
        Boolean cached = coverage.get(codePoint);
        if (cached != null) {
            return cached;
        }
        boolean encodable = canEncode(font, codePoint);
        coverage.put(codePoint, encodable);
        return encodable;
    }

    private static boolean canEncode(PDFont font, int codePoint) {
        try {
            font.encode(new String(Character.toChars(codePoint)));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String fontKey(PDFont font) {
        return font != null ? font.getName() : "<null>";
    }

    /**
     * Visible for tests. Clears the deduplication cache so a fresh test
     * can assert on the warn sequence without process restart.
     */
    static void resetForTesting() {
        SEEN.clear();
        ENCODABLE_BY_FONT.clear();
    }
}
