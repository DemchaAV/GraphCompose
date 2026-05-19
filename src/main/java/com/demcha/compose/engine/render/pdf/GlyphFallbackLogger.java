package com.demcha.compose.engine.render.pdf;

import org.apache.pdfbox.pdmodel.font.PDFont;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        String fontName = font != null ? font.getName() : "<null>";
        long key = ((long) fontName.hashCode() << 32) | (codePoint & 0xFFFFFFFFL);
        if (SEEN.add(key)) {
            LOG.warn("glyph.missing font={} codePoint=U+{} replaced='?'",
                    fontName, String.format("%04X", codePoint));
        }
    }

    /**
     * Visible for tests. Clears the deduplication cache so a fresh test
     * can assert on the warn sequence without process restart.
     */
    static void resetForTesting() {
        SEEN.clear();
    }
}
