package com.demcha.compose.engine.render.pdf;

import com.demcha.compose.engine.components.content.text.TextStyle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    void sanitizeForRender_collapsesConsecutiveSpaces() {
        // Pins existing textSanitizer behaviour: multiple spaces collapse
        // to a single space. If this contract ever changes, wrap geometry
        // assumptions across the engine must be revisited.
        String input = "spaced     out";
        String output = helvetica.sanitizeForRender(TextStyle.DEFAULT_STYLE, input);

        assertThat(output).isEqualTo("spaced out");
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
}
