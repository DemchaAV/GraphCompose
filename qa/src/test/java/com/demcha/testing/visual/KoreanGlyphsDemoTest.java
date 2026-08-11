package com.demcha.testing.visual;

import com.demcha.compose.font.FontName;
import com.demcha.testing.VisualTestOutputs;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.text.Normalizer;

import static com.demcha.testing.visual.ScriptGlyphDemo.assertNoGlyphSubstitution;
import static com.demcha.testing.visual.ScriptGlyphDemo.assertScriptOnPage;
import static com.demcha.testing.visual.ScriptGlyphDemo.assertValidPdf;
import static com.demcha.testing.visual.ScriptGlyphDemo.render;

/**
 * Renders Korean through the family added in {@code graph-compose-fonts} 1.1.0 and
 * proves the glyphs reached the page.
 *
 * <p>The two cases here are the ones a Korean-only sample would pass while a real
 * document failed: a line mixing Korean with an accented European name, which the
 * engine draws entirely in the Korean family because it does not fall back across
 * families, and a decomposed string, which holds conjoining jamo instead of the
 * precomposed syllables everything else in the pipeline sees.</p>
 *
 * <p>What the decomposed case asserts is that the glyphs exist, not that they compose:
 * a PDF draws code points through the font's {@code cmap} and runs no OpenType
 * composition, so NFD Korean comes out as a row of separate jamo. That is the
 * difference between legible-but-wrong and a row of question marks, and NFC remains
 * the form to hand in.</p>
 */
class KoreanGlyphsDemoTest {

    private static final String KOREAN = "안녕하세요 세계";

    @Test
    void koreanRendersAlongsideTheAccentedLatinAParagraphMayMixIn() throws Exception {
        Path output = VisualTestOutputs.preparePdf("korean-gothica1", "script-fonts");

        render(output, FontName.GOTHIC_A1, KOREAN + " — Gothic A1, Müller & Ångström 2026");

        assertValidPdf(output);
        assertNoGlyphSubstitution(output);
        assertScriptOnPage(output, 0xAC00, 0xD7A3, 7);
        assertScriptOnPage(output, 0x00C0, 0x00FF, 3);
    }

    @Test
    void decomposedKoreanStillDrawsInsteadOfBecomingSubstitutionMarks() throws Exception {
        Path output = VisualTestOutputs.preparePdf("korean-gothica1-decomposed", "script-fonts");

        String decomposed = Normalizer.normalize(KOREAN, Normalizer.Form.NFD);

        render(output, FontName.GOTHIC_A1, decomposed);

        assertValidPdf(output);
        assertNoGlyphSubstitution(output);
        assertScriptOnPage(output, 0x1100, 0x11FF,
                decomposed.codePointCount(0, decomposed.length()) - 1);
    }
}
