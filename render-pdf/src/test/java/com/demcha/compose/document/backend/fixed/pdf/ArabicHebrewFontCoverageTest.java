package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.font.FontName;

import org.apache.pdfbox.pdmodel.font.PDFont;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the glyph coverage the bundled right-to-left families are carried for.
 *
 * <p>Arabic letters change shape by position, and PDFBox draws a string through the
 * font's {@code cmap} without executing OpenType {@code GSUB}. Contextual joining is
 * therefore reachable only by mapping each letter to its Arabic Presentation Forms-B
 * code point — which works if, and only if, the font's {@code cmap} actually contains
 * that block. Coverage of the base Arabic block says nothing about it: Scheherazade New
 * and Rubik both cover Arabic letters fully and carry <em>no</em> presentation forms at
 * all, so Arabic drawn in them can never join.</p>
 *
 * <p>That is the property Amiri was chosen for, and it is invisible in every other test:
 * swapping the family for one that shapes only through {@code GSUB} would still render
 * Arabic, still measure, still produce a valid PDF — and silently draw every letter in
 * its isolated form. This test is the guard that makes such a swap fail loudly.</p>
 *
 * <p>Hebrew has no contextual forms, so David Libre is held to the plain Hebrew block.</p>
 */
class ArabicHebrewFontCoverageTest {

    /**
     * The measurement library is the one that carries the bundled families and needs no
     * owning document; encodability is a property of the font program, so the measurement
     * face answers it exactly as the embedded render face would.
     */
    private static final FontLibrary LIBRARY = PdfFontLibraryFactory.measurementLibrary(List.of());

    /**
     * Presentation forms for the Arabic <em>letters</em> and the lam-alef ligatures.
     * Deliberately starts at U+FE80 rather than at the U+FE70 block boundary: U+FE70..FE7F
     * are tashkeel-with-tatweel forms that contextual shaping never produces, and Amiri
     * omits two of them.
     */
    private static final int ARABIC_PRESENTATION_FORMS_FIRST = 0xFE80;
    private static final int ARABIC_PRESENTATION_FORMS_LAST = 0xFEFC;

    private static final int ARABIC_LETTERS_FIRST = 0x0621;
    private static final int ARABIC_LETTERS_LAST = 0x064A;

    private static final int HEBREW_LETTERS_FIRST = 0x05D0;
    private static final int HEBREW_LETTERS_LAST = 0x05EA;

    private static final List<TextDecoration> FACES =
            List.of(TextDecoration.DEFAULT, TextDecoration.BOLD, TextDecoration.ITALIC);

    @Test
    void everyArabicPresentationFormIsEncodableInEveryAmiriFace() {
        for (TextDecoration face : FACES) {
            assertThat(unencodable(FontName.AMIRI, face,
                    ARABIC_PRESENTATION_FORMS_FIRST, ARABIC_PRESENTATION_FORMS_LAST))
                    .describedAs("Amiri %s must carry the presentation forms contextual "
                            + "Arabic shaping maps to — a font that shapes only through GSUB "
                            + "would draw every letter isolated", face)
                    .isEmpty();
        }
    }

    @Test
    void amiriAlsoCarriesTheBaseArabicLettersAndLatin() {
        assertThat(unencodable(FontName.AMIRI, TextDecoration.DEFAULT,
                ARABIC_LETTERS_FIRST, ARABIC_LETTERS_LAST))
                .describedAs("the base block is what unshaped text and the degraded "
                        + "fallback path draw")
                .isEmpty();
        assertThat(unencodable(FontName.AMIRI, TextDecoration.DEFAULT, 'A', 'z'))
                .describedAs("a run mixing Arabic with Latin is drawn in one font — "
                        + "the engine does not fall back across families")
                .isEmpty();
    }

    @Test
    void davidLibreCarriesHebrewAndLatin() {
        for (TextDecoration face : List.of(TextDecoration.DEFAULT, TextDecoration.BOLD)) {
            assertThat(unencodable(FontName.DAVID_LIBRE, face,
                    HEBREW_LETTERS_FIRST, HEBREW_LETTERS_LAST))
                    .describedAs("David Libre %s must cover the Hebrew block", face)
                    .isEmpty();
            assertThat(unencodable(FontName.DAVID_LIBRE, face, 'A', 'z')).isEmpty();
        }
    }

    @Test
    void davidLibreItalicResolvesToTheRegularFaceBecauseUpstreamShipsNone() {
        assertThat(face(FontName.DAVID_LIBRE, TextDecoration.ITALIC).getName())
                .describedAs("the builder collapses a missing face to the regular one, so "
                        + "italic Hebrew renders upright rather than failing")
                .isEqualTo(face(FontName.DAVID_LIBRE, TextDecoration.DEFAULT).getName());
    }

    private static PDFont face(FontName name, TextDecoration decoration) {
        PdfFont font = LIBRARY.getFont(name, PdfFont.class).orElseThrow(
                () -> new AssertionError("family not in the bundled catalog: " + name));
        return font.fontType(decoration);
    }

    /** Returns the code points in the range the face cannot encode, formatted for the failure message. */
    private static List<String> unencodable(FontName name, TextDecoration decoration, int first, int last) {
        PDFont font = face(name, decoration);
        List<String> missing = new ArrayList<>();
        for (int codePoint = first; codePoint <= last; codePoint++) {
            if (!canEncode(font, codePoint)) {
                missing.add(String.format("U+%04X", codePoint));
            }
        }
        return missing;
    }

    private static boolean canEncode(PDFont font, int codePoint) {
        try {
            font.encode(new String(Character.toChars(codePoint)));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
