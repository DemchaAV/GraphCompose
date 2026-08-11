package com.demcha.testing.visual;

import com.demcha.compose.font.FontName;
import com.demcha.testing.VisualTestOutputs;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static com.demcha.testing.visual.ScriptGlyphDemo.assertNoGlyphSubstitution;
import static com.demcha.testing.visual.ScriptGlyphDemo.assertScriptOnPage;
import static com.demcha.testing.visual.ScriptGlyphDemo.assertValidPdf;
import static com.demcha.testing.visual.ScriptGlyphDemo.render;

/**
 * Renders Georgian and Armenian through the families added in {@code graph-compose-fonts}
 * 1.1.0 and proves the glyphs reached the page.
 *
 * <p>Both scripts run left to right and neither joins, so there is nothing here about
 * ordering — only that the text a document asks for is the text that gets drawn. Georgian
 * is written in both of its cases: Mtavruli, the capitals a heading is set in, lives in a
 * Unicode block of its own, so a family can cover everything a body paragraph needs and
 * still draw a title as question marks.</p>
 */
class GeorgianArmenianGlyphsDemoTest {

    private static final String GEORGIAN = "გამარჯობა მსოფლიო";
    private static final String GEORGIAN_MTAVRULI = "ᲒᲐᲛᲐᲠᲯᲝᲑᲐ";
    private static final String ARMENIAN = "Բարև աշխարհ";

    @Test
    void georgianRendersInBothOfItsCases() throws Exception {
        Path output = VisualTestOutputs.preparePdf("georgian-notosans", "script-fonts");

        render(output, FontName.NOTO_SANS_GEORGIAN,
                GEORGIAN + " · " + GEORGIAN_MTAVRULI + " — Noto Sans Georgian 2026");

        assertValidPdf(output);
        assertNoGlyphSubstitution(output);
        assertScriptOnPage(output, 0x10D0, 0x10FA, GEORGIAN.codePointCount(0, GEORGIAN.length()) - 1);
        assertScriptOnPage(output, 0x1C90, 0x1CBA,
                GEORGIAN_MTAVRULI.codePointCount(0, GEORGIAN_MTAVRULI.length()));
    }

    @Test
    void armenianRendersThroughTheBundledArmenianFamily() throws Exception {
        Path output = VisualTestOutputs.preparePdf("armenian-notosans", "script-fonts");

        render(output, FontName.NOTO_SANS_ARMENIAN, ARMENIAN + " — Noto Sans Armenian 2026");

        assertValidPdf(output);
        assertNoGlyphSubstitution(output);
        // Through U+0587 rather than U+0586: the ech-yiwn ligature և is a letter of
        // ordinary Armenian text — it is the word "and" — and sits past the small letters.
        assertScriptOnPage(output, 0x0531, 0x0587,
                ARMENIAN.codePointCount(0, ARMENIAN.length()) - 1);
    }
}
