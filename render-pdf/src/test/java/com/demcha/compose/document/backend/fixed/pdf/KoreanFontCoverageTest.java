package com.demcha.compose.document.backend.fixed.pdf;

import static com.demcha.compose.document.backend.fixed.pdf.FontCoverageProbe.face;
import static com.demcha.compose.document.backend.fixed.pdf.FontCoverageProbe.unencodable;
import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.font.FontName;

import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Pins the glyph coverage the bundled Korean family is carried for.
 *
 * <p>Korean is written in precomposed syllables — 11 172 of them, one per
 * consonant/vowel/final combination — so "covers Hangul" is a claim about a range no
 * one reads by eye. It is asserted whole here rather than sampled, because a family
 * missing a slice of it renders most of a document and loses the words that happen to
 * use those syllables.</p>
 *
 * <p>Two properties beyond the syllables decided this family over the better-known
 * alternatives, and both are invisible in a document that only ever holds Korean:</p>
 *
 * <ul>
 *   <li>The conjoining jamo a <em>decomposed</em> (NFD) string is made of. A string
 *       that has been through {@code Normalizer.normalize(s, NFD)} — which anything
 *       doing case-insensitive or accent-insensitive comparison may hand back — holds
 *       no precomposed syllables at all. Carrying the jamo makes such a string draw
 *       rather than vanish into substitution marks; it draws as a row of separate
 *       jamo, because a PDF composes nothing, so NFC is still the form to hand in.</li>
 *   <li>Latin-1, Latin Extended-A and Cyrillic. One paragraph is drawn in one family,
 *       so a Korean sentence containing a European name is drawn entirely in this
 *       font; a family with ASCII but no accents turns <em>Müller</em> into
 *       <em>M?ller</em> while the Korean around it renders perfectly.</li>
 * </ul>
 */
class KoreanFontCoverageTest {

    /** The precomposed syllables, which is how Korean text is normally written. */
    private static final int HANGUL_SYLLABLES_FIRST = 0xAC00;
    private static final int HANGUL_SYLLABLES_LAST = 0xD7A3;

    /** Conjoining jamo — what a decomposed (NFD) Korean string is made of. */
    private static final int HANGUL_JAMO_FIRST = 0x1100;
    private static final int HANGUL_JAMO_LAST = 0x11FF;

    /** Compatibility jamo — a single consonant or vowel quoted on its own. */
    private static final int HANGUL_COMPAT_JAMO_FIRST = 0x3131;
    private static final int HANGUL_COMPAT_JAMO_LAST = 0x318E;

    private static final int LATIN_1_SUPPLEMENT_FIRST = 0x00C0;
    private static final int LATIN_1_SUPPLEMENT_LAST = 0x00FF;

    private static final int CYRILLIC_FIRST = 0x0410;
    private static final int CYRILLIC_LAST = 0x044F;

    private static final int LATIN_EXTENDED_A_FIRST = 0x0100;
    private static final int LATIN_EXTENDED_A_LAST = 0x017F;

    // U+03A2 is unassigned, so no font carries it and a plain sweep would never be empty.
    private static final int GREEK_FIRST = 0x0391;
    private static final int GREEK_LAST = 0x03A1;

    private static final List<TextDecoration> FACES =
            List.of(TextDecoration.DEFAULT, TextDecoration.BOLD);

    @Test
    void everyHangulSyllableIsEncodableInBothShippedFaces() {
        for (TextDecoration decoration : FACES) {
            assertThat(unencodable(FontName.GOTHIC_A1, decoration,
                    HANGUL_SYLLABLES_FIRST, HANGUL_SYLLABLES_LAST))
                    .describedAs("Gothic A1 %s must carry all 11 172 precomposed syllables — "
                            + "a gap in the range loses whichever words use it, with the rest "
                            + "of the document rendering normally", decoration)
                    .isEmpty();
        }
    }

    @Test
    void bothJamoFormsAreEncodableSoDecomposedKoreanStillRenders() {
        assertThat(unencodable(FontName.GOTHIC_A1, TextDecoration.DEFAULT,
                HANGUL_JAMO_FIRST, HANGUL_JAMO_LAST))
                .describedAs("a decomposed (NFD) Korean string holds no precomposed "
                        + "syllables at all, only these")
                .isEmpty();
        assertThat(unencodable(FontName.GOTHIC_A1, TextDecoration.DEFAULT,
                HANGUL_COMPAT_JAMO_FIRST, HANGUL_COMPAT_JAMO_LAST))
                .isEmpty();
    }

    @Test
    void theAccentedLatinAndCyrillicARunMayMixInAreEncodable() {
        for (TextDecoration decoration : FACES) {
            assertThat(unencodable(FontName.GOTHIC_A1, decoration, 'A', 'z'))
                    .isEmpty();
            assertThat(unencodable(FontName.GOTHIC_A1, decoration,
                    LATIN_1_SUPPLEMENT_FIRST, LATIN_1_SUPPLEMENT_LAST))
                    .describedAs("a paragraph is drawn in one family, so a Korean sentence "
                            + "holding a European name is drawn in this font too")
                    .isEmpty();
            assertThat(unencodable(FontName.GOTHIC_A1, decoration, CYRILLIC_FIRST, CYRILLIC_LAST))
                    .isEmpty();
            assertThat(unencodable(FontName.GOTHIC_A1, decoration,
                    LATIN_EXTENDED_A_FIRST, LATIN_EXTENDED_A_LAST))
                    .describedAs("the public Javadoc and both docs promise Latin Extended-A")
                    .isEmpty();
            assertThat(unencodable(FontName.GOTHIC_A1, decoration, GREEK_FIRST, GREEK_LAST))
                    .describedAs("and Greek")
                    .isEmpty();
        }
    }

    @Test
    void boldIsItsOwnFaceRatherThanTheRegularOneUnderAnotherName() {
        assertThat(face(FontName.GOTHIC_A1, TextDecoration.BOLD).getName())
                .describedAs("upstream ships a drawn bold; collapsing it onto regular would "
                            + "render every heading at body weight with nothing failing")
                .isNotEqualTo(face(FontName.GOTHIC_A1, TextDecoration.DEFAULT).getName());
    }

    @Test
    void italicResolvesToTheRegularFaceBecauseUpstreamShipsNone() {
        assertThat(face(FontName.GOTHIC_A1, TextDecoration.ITALIC).getName())
                .describedAs("the builder collapses a missing face to the regular one, so "
                        + "italic Korean renders upright rather than failing")
                .isEqualTo(face(FontName.GOTHIC_A1, TextDecoration.DEFAULT).getName());
    }
}
