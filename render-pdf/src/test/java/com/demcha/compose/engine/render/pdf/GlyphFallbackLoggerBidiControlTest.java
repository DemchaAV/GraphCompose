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
    void ordinaryUnencodableCharactersStillBecomeQuestionMarks() {
        PDType1Font helvetica = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        assertThat(GlyphFallbackLogger.sanitize(helvetica, "a\u05D0b"))
                .describedAs("the mark exemption must not widen into swallowing real text")
                .isEqualTo("a?b");
    }
}
