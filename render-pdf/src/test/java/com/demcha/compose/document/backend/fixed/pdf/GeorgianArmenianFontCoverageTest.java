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
 * <p>Both families ship upstream as variable fonts and the catalog carries their regular
 * instance, so every decoration resolves to one face. That is asserted rather than
 * assumed: a bold that silently resolved to a <em>different</em> family's face would
 * still draw.</p>
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

    private static final int ARMENIAN_SMALL_FIRST = 0x0561;
    private static final int ARMENIAN_SMALL_LAST = 0x0586;

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
