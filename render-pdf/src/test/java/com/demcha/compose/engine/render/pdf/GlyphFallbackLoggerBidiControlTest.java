package com.demcha.compose.engine.render.pdf;

import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what happens to a bidirectional formatting character at the glyph seam.
 *
 * <p>The marks survive control-character sanitizing so the layout can read them, and
 * this seam — shared by measurement and every render site — is where they leave. The
 * wrong behaviours on either side are both quiet: substituting them puts a visible
 * {@code '?'} on the page and gives a zero-width character a width the wrapping never
 * accounted for; keeping them hands PDFBox a code point Helvetica cannot encode.</p>
 */
class GlyphFallbackLoggerBidiControlTest {

    @Test
    void aDirectionMarkIsDroppedNotSubstituted() {
        PDType1Font helvetica = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        assertThat(GlyphFallbackLogger.sanitize(helvetica, "a\u200Fb\u200Ec\u2066d"))
                .isEqualTo("abcd");
    }

    @Test
    void aPresentationFormDegradesToItsBaseLetterWhenTheFontHasOnlyThat() throws Exception {
        // The shape of a GSUB-only Arabic font: the base letters are in the cmap, the
        // presentation forms are not. Losing the form must cost the joining, not the
        // text — the base letter renders isolated but readable.
        org.apache.pdfbox.pdmodel.font.PDFont gsubOnly =
                org.mockito.Mockito.mock(org.apache.pdfbox.pdmodel.font.PDFont.class);
        org.mockito.Mockito.when(gsubOnly.getName()).thenReturn("GsubOnlyProbe");
        org.mockito.Mockito.when(gsubOnly.encode(org.mockito.Mockito.anyString()))
                .thenAnswer(invocation -> {
                    String value = invocation.getArgument(0);
                    if (value.chars().anyMatch(cp -> cp >= 0xFE70 && cp <= 0xFEFC)) {
                        throw new IllegalArgumentException("no glyph");
                    }
                    return new byte[]{0};
                });

        assertThat(GlyphFallbackLogger.sanitize(gsubOnly, "ﺍ"))
                .describedAs("isolated-alef form falls back to the alef itself")
                .isEqualTo("ا");
        assertThat(GlyphFallbackLogger.sanitize(gsubOnly, "ﻻ"))
                .describedAs("a lam-alef ligature decomposes back into its two letters")
                .isEqualTo("لا");
    }

    @Test
    void ordinaryUnencodableCharactersStillBecomeQuestionMarks() {
        PDType1Font helvetica = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        assertThat(GlyphFallbackLogger.sanitize(helvetica, "a\u05D0b"))
                .describedAs("the mark exemption must not widen into swallowing real text")
                .isEqualTo("a?b");
    }
}
