package com.demcha.compose.engine.text.bidi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contextual forms the shaper produces, against hand-checked expectations.
 *
 * <p>The expected strings are presentation-form code points written as escapes, because
 * that is what the assertions are about: which exact code point each position maps to.
 * A wrong form still renders — as the wrong variant of the right letter — so nothing
 * but these comparisons would notice.</p>
 */
class ArabicShaperTest {

    @Test
    void aWordShapesToItsContextualForms() {
        // مرحبا — meem initial, reh final (reh only joins backwards), hah initial,
        // beh medial, alef final. Verified against the same word rendered by a
        // GSUB-capable shaper.
        assertThat(ArabicShaper.shape("مرحبا"))
                .isEqualTo("ﻣﺮﺣﺒﺎ");
    }

    @Test
    void aRightJoiningLetterBreaksTheChain() {
        // كتاب — kaf initial, teh medial, alef final, then beh must start again in
        // isolated form because alef never joins forward.
        assertThat(ArabicShaper.shape("كتاب"))
                .isEqualTo("ﻛﺘﺎﺏ");
    }

    @Test
    void everyLamAlefVariantBecomesOneLigature() {
        assertThat(ArabicShaper.shape("لا")).isEqualTo("ﻻ");
        assertThat(ArabicShaper.shape("لآ")).isEqualTo("ﻵ");
        assertThat(ArabicShaper.shape("لأ")).isEqualTo("ﻷ");
        assertThat(ArabicShaper.shape("لإ")).isEqualTo("ﻹ");
        // After a dual-joining letter the ligature takes its final form: الله ends
        // in lam+heh, and the inner lam-alef of e.g. بلا joins to the beh before it.
        assertThat(ArabicShaper.shape("بلا"))
                .isEqualTo("ﺑﻼ");
    }

    @Test
    void vowelPointsAreTransparentToJoining() {
        // مَرْحَبًا — the same word as above with harakat between the letters; the
        // letter forms must be identical, the points preserved.
        assertThat(ArabicShaper.shape("مَرْحَبًا"))
                .isEqualTo("ﻣَﺮْﺣَﺒًﺎ");
    }

    @Test
    void aDirectionMarkDoesNotBreakAJoin() {
        // An RLM between two letters survives sanitizing until the layout has read
        // it; if the shaper treated it as opaque, the letters on either side would
        // fall back to isolated forms and the word would visibly break apart.
        String withMark = "م" + "‏" + "ر";

        assertThat(ArabicShaper.shape(withMark))
                .isEqualTo("ﻣ‏ﺮ");
    }

    @Test
    void shapingIsIdempotent() {
        String once = ArabicShaper.shape("مرحبا بالعالم");

        assertThat(ArabicShaper.shape(once))
                .describedAs("presentation forms lie outside the base block, so a second "
                        + "pass must find nothing to do")
                .isEqualTo(once);
    }

    @Test
    void textWithoutArabicIsReturnedAsTheSameInstance() {
        String plain = "Hello שלום 123";

        assertThat(ArabicShaper.shape(plain)).isSameAs(plain);
        assertThat(ArabicShaper.shape("")).isEmpty();
        assertThat(ArabicShaper.shape(null)).isEmpty();
    }

    @Test
    void digitsAndLatinInsideArabicStayThemselves() {
        String mixed = ArabicShaper.shape("مرحبا 2026 ok");

        assertThat(mixed).contains("2026").contains("ok");
        assertThat(mixed).isEqualTo("ﻣﺮﺣﺒﺎ 2026 ok");
    }

    @Test
    void everyProducedFormMapsBackToItsBaseLetters() {
        // The degradation path at the glyph seam relies on this inverse: a font
        // without a form draws the base letter instead of losing the text to '?'.
        String shaped = ArabicShaper.shape("مرحبا لا والله");

        StringBuilder recovered = new StringBuilder();
        for (int index = 0; index < shaped.length(); index++) {
            char character = shaped.charAt(index);
            String base = ArabicShaper.baseLettersOf(character);
            recovered.append(base != null ? base : String.valueOf(character));
        }
        assertThat(recovered.toString()).isEqualTo("مرحبا لا والله");
        assertThat(ArabicShaper.baseLettersOf('A')).isNull();
        assertThat(ArabicShaper.baseLettersOf(0x05D0)).isNull();
    }
}
