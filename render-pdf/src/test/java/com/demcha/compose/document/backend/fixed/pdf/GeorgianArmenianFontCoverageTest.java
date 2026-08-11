package com.demcha.compose.document.backend.fixed.pdf;

import static com.demcha.compose.document.backend.fixed.pdf.FontCoverageProbe.face;
import static com.demcha.compose.document.backend.fixed.pdf.FontCoverageProbe.unencodable;
import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.font.FontName;

import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Pins the glyph coverage the bundled Georgian and Armenian families are carried for.
 *
 * <p>Both scripts are alphabetic and unicameral in the ways that matter here — no
 * contextual forms, no joining — so coverage of their letter blocks is the whole
 * requirement. What is easy to lose is the second case: Georgian writes headings and
 * emphasis in <em>Mtavruli</em>, which Unicode encodes in its own block far away from
 * Mkhedruli (U+1C90 rather than U+10D0), and Armenian has a genuine upper/lower pair.
 * A font covering only the lowercase range renders body text and drops every title,
 * which no other test in this module would notice.</p>
 *
 * <p>Both binaries are the variable fonts upstream publishes — there is no static face to
 * take — so every decoration resolves to the one face inside them, and which weight that
 * is depends entirely on the file's {@code fvar} default. A PDF applies no variable-font
 * instancing: it draws the default instance and nothing else. That is why the weight is
 * asserted here rather than assumed, and it is not a hypothetical — the Noto CJK variable
 * fonts default to 100, so the same vendoring done with one of those would have shipped a
 * family that renders every page in hairline Thin, with full coverage and no failure
 * anywhere to point at it.</p>
 */
class GeorgianArmenianFontCoverageTest {

    /** Mkhedruli — the case Georgian is normally written in. */
    private static final int GEORGIAN_MKHEDRULI_FIRST = 0x10D0;
    private static final int GEORGIAN_MKHEDRULI_LAST = 0x10FA;

    /** Mtavruli — the capitals Georgian sets titles and emphasis in. */
    private static final int GEORGIAN_MTAVRULI_FIRST = 0x1C90;
    private static final int GEORGIAN_MTAVRULI_LAST = 0x1CBA;

    private static final int ARMENIAN_CAPITAL_FIRST = 0x0531;
    private static final int ARMENIAN_CAPITAL_LAST = 0x0556;

    /**
     * Through U+0587 rather than U+0586: the ech-yiwn ligature և is the Armenian word
     * for "and", so a family without it loses a word from nearly every sentence.
     */
    private static final int ARMENIAN_SMALL_FIRST = 0x0561;
    private static final int ARMENIAN_SMALL_LAST = 0x0587;

    private static final List<TextDecoration> FACES = List.of(
            TextDecoration.DEFAULT, TextDecoration.BOLD, TextDecoration.ITALIC, TextDecoration.BOLD_ITALIC);

    @Test
    void theGeorgianFamilyCarriesBothCasesAndLatin() {
        assertThat(unencodable(FontName.NOTO_SANS_GEORGIAN, TextDecoration.DEFAULT,
                GEORGIAN_MKHEDRULI_FIRST, GEORGIAN_MKHEDRULI_LAST))
                .isEmpty();
        assertThat(unencodable(FontName.NOTO_SANS_GEORGIAN, TextDecoration.DEFAULT,
                GEORGIAN_MTAVRULI_FIRST, GEORGIAN_MTAVRULI_LAST))
                .describedAs("Georgian capitals live in their own block — a family without "
                        + "them sets body text and loses every heading")
                .isEmpty();
        assertThat(unencodable(FontName.NOTO_SANS_GEORGIAN, TextDecoration.DEFAULT, 'A', 'z'))
                .describedAs("a run mixing Georgian with Latin is drawn in one font — "
                        + "the engine does not fall back across families")
                .isEmpty();
    }

    @Test
    void theArmenianFamilyCarriesBothCasesAndLatin() {
        assertThat(unencodable(FontName.NOTO_SANS_ARMENIAN, TextDecoration.DEFAULT,
                ARMENIAN_CAPITAL_FIRST, ARMENIAN_CAPITAL_LAST))
                .isEmpty();
        assertThat(unencodable(FontName.NOTO_SANS_ARMENIAN, TextDecoration.DEFAULT,
                ARMENIAN_SMALL_FIRST, ARMENIAN_SMALL_LAST))
                .isEmpty();
        assertThat(unencodable(FontName.NOTO_SANS_ARMENIAN, TextDecoration.DEFAULT, 'A', 'z'))
                .isEmpty();
    }

    @Test
    void theWeightAVariableFontDefaultsToIsTheRegularOne() {
        for (FontName family : List.of(FontName.NOTO_SANS_GEORGIAN, FontName.NOTO_SANS_ARMENIAN)) {
            assertThat(face(family, TextDecoration.DEFAULT).getName())
                    .describedAs("%s: the drawn weight is the file's fvar default, so the "
                            + "face must name itself Regular — a refresh to a build that "
                            + "defaults to another weight would render every page in it", family)
                    .endsWith("-Regular");
        }
    }

    @Test
    void everyDecorationResolvesToTheRegularFaceBecauseOnlyThatInstanceIsBundled() {
        for (FontName family : List.of(FontName.NOTO_SANS_GEORGIAN, FontName.NOTO_SANS_ARMENIAN)) {
            String regular = face(family, TextDecoration.DEFAULT).getName();
            for (TextDecoration decoration : FACES) {
                assertThat(face(family, decoration).getName())
                        .describedAs("%s %s: upstream ships one static instance, so the "
                                + "builder collapses the other faces onto it — bold renders "
                                + "unemboldened rather than failing", family, decoration)
                        .isEqualTo(regular);
            }
        }
    }
}
