package com.demcha.compose.engine.render.pdf;

import com.demcha.compose.engine.components.content.text.TextStyle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the glyph sanitizer that protects the PDF render path from
 * crashes when the active font cannot encode a code point (arrows ↦
 * U+2192, bullets ↦ U+25CF, emoji, custom unicode).
 *
 * <p>The renderer's contract is: the bytes width measurement sees and
 * the bytes {@code showText(...)} sees must match exactly. These tests
 * pin both the substitution policy ({@code ?} for unknown glyphs) and
 * the safe pass-throughs (ASCII, single spaces) so a future engine
 * change cannot silently break wrap geometry or rendering.</p>
 *
 * @author Artem Demchyshyn
 */
class PdfFontSanitizerTest {

    private PdfFont helvetica;

    @BeforeEach
    void setUp() {
        helvetica = new PdfFont(
                new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),
                new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE),
                new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE));
    }

    @Test
    void sanitizeForRender_replacesUnknownGlyphs() {
        String input = "Star ● Arrow → Done";
        String output = helvetica.sanitizeForRender(TextStyle.DEFAULT_STYLE, input);

        assertThat(output).isEqualTo("Star ? Arrow ? Done");
    }

    @Test
    void sanitizeForRender_preservesAscii() {
        String input = "Hello, World! 1234 abc XYZ";
        String output = helvetica.sanitizeForRender(TextStyle.DEFAULT_STYLE, input);

        assertThat(output).isEqualTo(input);
    }

    @Test
    void sanitizeForRender_preservesSingleSpaces() {
        String input = "one two three";
        String output = helvetica.sanitizeForRender(TextStyle.DEFAULT_STYLE, input);

        assertThat(output).isEqualTo("one two three");
    }

    @Test
    void sanitizeForRender_preservesConsecutiveSpaces() {
        // v1.6.3: textSanitizer no longer collapses author whitespace.
        // The previous collapse (multi-space → one space) shrank the
        // rendered string under measurement and broke spaced-upper
        // titles ("A R T E M  D E M C H Y S H Y N" rendered without
        // its inter-word gap) and "   |   " contact-row separators.
        // Newlines / NBSP / control chars still resolve to a single
        // space each, but adjacent author spaces are kept verbatim so
        // wrap geometry, link-rect emission, and showText all see the
        // same string.
        String input = "spaced     out";
        String output = helvetica.sanitizeForRender(TextStyle.DEFAULT_STYLE, input);

        assertThat(output).isEqualTo("spaced     out");
        assertThat(helvetica.getTextWidth(TextStyle.DEFAULT_STYLE, input))
                .isEqualTo(helvetica.getTextWidth(TextStyle.DEFAULT_STYLE, output));
    }

    @Test
    void sanitizeForRender_preservesWhitespaceOnlyTokensVerbatim() {
        // v1.6.3 regression: paragraph tokenisation produces standalone
        // whitespace-only tokens (e.g. the "   " halves of a "   |   "
        // contact-line separator). Their render width must match what
        // getTextWidth measured so the PDF text matrix advances by the
        // same amount — otherwise link annotation rectangles drift to the
        // right of the glyphs actually drawn (visible on right-/center-
        // aligned contact rows where LinkedIn / GitHub clickable areas
        // ended up past their visible text).
        String triple = "   ";
        String output = helvetica.sanitizeForRender(TextStyle.DEFAULT_STYLE, triple);

        assertThat(output).isEqualTo("   ");
        assertThat(helvetica.getTextWidth(TextStyle.DEFAULT_STYLE, triple))
                .isEqualTo(helvetica.getTextWidth(TextStyle.DEFAULT_STYLE, output));
    }

    @Test
    void sanitizeForRender_handlesEmpty() {
        assertThat(helvetica.sanitizeForRender(TextStyle.DEFAULT_STYLE, "")).isEmpty();
    }

    @Test
    void sanitizeForRender_handlesNull() {
        assertThat(helvetica.sanitizeForRender(TextStyle.DEFAULT_STYLE, null)).isEmpty();
    }

    @Test
    void getTextWidth_returnsConsistentValueWithSanitizedString() {
        // Width must be measured against the sanitized form so wrap
        // geometry matches what the renderer actually draws.
        double widthOfBulletInput = helvetica.getTextWidth(TextStyle.DEFAULT_STYLE, "Hello ●");
        double widthOfQuestionInput = helvetica.getTextWidth(TextStyle.DEFAULT_STYLE, "Hello ?");

        assertThat(widthOfBulletInput).isEqualTo(widthOfQuestionInput);
    }

    @Test
    void sanitizeByFont_directlyReplacesUnsupportedGlyphsOnly() {
        // Public escape hatch for render handlers that already have a
        // resolved PDFont in hand (e.g. PdfParagraphFragmentRenderHandler
        // after setFont). Should NOT do general control-char cleanup —
        // that is sanitizeForRender's job.
        var font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        String output = helvetica.sanitizeByFont(font, "ok ● then");

        assertThat(output).isEqualTo("ok ? then");
    }

    @Test
    void coverageMemo_probesEachDistinctGlyphOnceAcrossRepeatedSanitisation() {
        // Finding 3: glyph coverage is memoized per (font, code point), so the
        // heavy PDFont.encode probe runs once per distinct glyph instead of once
        // per occurrence on every measurement/render pass.
        GlyphFallbackLogger.resetForTesting();
        EncodeCountingFont font = new EncodeCountingFont();

        // "banana banana" repeats only four distinct code points: b, a, n, space.
        String first = GlyphFallbackLogger.sanitize(font, "banana banana");
        int probesAfterFirst = font.encodeCalls();

        String second = GlyphFallbackLogger.sanitize(font, "banana banana");
        int probesAfterSecond = font.encodeCalls();

        assertThat(first).isEqualTo("banana banana");
        assertThat(second).isEqualTo("banana banana");
        assertThat(probesAfterFirst)
                .describedAs("encode probed once per distinct (font, code point), not per occurrence")
                .isEqualTo(4);
        assertThat(probesAfterSecond - probesAfterFirst)
                .describedAs("re-sanitising the same glyphs adds no encode probes")
                .isZero();
    }

    @Test
    void coverageMemo_probesUnencodableGlyphOnceThenReusesSubstitution() {
        // The cache remembers negatives too: a missing glyph is probed once, then
        // every later occurrence is a cache hit that still substitutes '?'.
        GlyphFallbackLogger.resetForTesting();
        EncodeCountingFont font = new EncodeCountingFont();

        String first = GlyphFallbackLogger.sanitize(font, "a●b●c●"); // ● = U+25CF, unencodable
        int probesAfterFirst = font.encodeCalls();

        String second = GlyphFallbackLogger.sanitize(font, "●●●");
        int probesAfterSecond = font.encodeCalls();

        assertThat(first).isEqualTo("a?b?c?");
        assertThat(second).isEqualTo("???");
        // Distinct code points in "a●b●c●": a, ●, b, c = four probes.
        assertThat(probesAfterFirst).isEqualTo(4);
        assertThat(probesAfterSecond - probesAfterFirst)
                .describedAs("the unencodable glyph is probed once, then served from cache")
                .isZero();
    }

    /**
     * Test-only Helvetica that counts how often the glyph sanitizer probes the
     * font, so the memo tests can assert probe counts with no instrumentation in
     * the production {@link GlyphFallbackLogger}. {@link PDFont#encode(int)} is the
     * per-code-point hook the sanitizer reaches through {@code encode(String)}.
     */
    private static final class EncodeCountingFont extends PDType1Font {
        private int encodeCalls;

        EncodeCountingFont() {
            super(Standard14Fonts.FontName.HELVETICA);
        }

        @Override
        protected byte[] encode(int code) throws IOException {
            encodeCalls++;
            return super.encode(code);
        }

        int encodeCalls() {
            return encodeCalls;
        }
    }
}
