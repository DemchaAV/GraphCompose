package com.demcha.compose.engine.text.bidi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contextual forms the shaper produces, against hand-checked expectations.
 *
 * <p>The literals are the real Arabic characters — the repo's tests use readable
 * script by convention, and unlike the invisible bidi controls a wrong letter is
 * visible in review. A wrong form still renders, as the wrong variant of the right
 * letter, so nothing but these comparisons would notice.</p>
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
        // After a dual-joining letter the ligature takes its final form: the inner
        // lam-alef of بلا joins to the beh before it.
        assertThat(ArabicShaper.shape("بلا"))
                .isEqualTo("ﺑﻼ");
    }

    @Test
    void aVocalizedLamAlefStaysTwoJoinedLettersSoTheMarkKeepsItsPlace() {
        // A vowel point between lam and alef belongs to the lam. Folding the pair
        // into one glyph would strand the mark after the ligature — and the inverse
        // mapping the slide backend relies on could no longer restore the author's
        // text. Two joined letters, exactly restorable, is the deliberate trade.
        String vocalized = "لَا";

        String shaped = ArabicShaper.shape(vocalized);
        assertThat(shaped).isEqualTo("ﻟَﺎ");
        assertThat(ArabicShaper.toBaseLetters(shaped)).isEqualTo(vocalized);
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
                .isEqualTo("ﻣ" + "‏" + "ﺮ");
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

    // U+200C and U+200D are written as escapes on purpose: a raw zero-width control in a
    // test source is invisible in review, and this file is about exactly those characters.
    private static final String ZWNJ = "‌";
    private static final String ZWJ = "‍";

    @Test
    void aZeroWidthNonJoinerBreaksAJoinTheLettersWouldOtherwiseMake() {
        // beh + heh join; with ZWNJ between them the author is saying they must not.
        String joined = ArabicShaper.shape("به");
        String broken = ArabicShaper.shape("ب" + ZWNJ + "ه");

        assertThat(joined).isEqualTo("ﺑﻪ");
        assertThat(broken)
                .describedAs("beh takes its isolated form and heh its own, as if they were "
                        + "not adjacent — and the control stays where the author put it")
                .isEqualTo("ﺏ" + ZWNJ + "ﻩ");
    }

    @Test
    void aZeroWidthJoinerMakesALetterTakeAConnectedFormWithNothingToConnectTo() {
        String isolated = ArabicShaper.shape("ب");
        String joinedForward = ArabicShaper.shape("ب" + ZWJ);

        assertThat(isolated).isEqualTo("ﺏ");
        assertThat(joinedForward)
                .describedAs("the joiner stands in for a following letter, so beh takes its "
                        + "initial form — the way an author quotes a connected form on its own")
                .isEqualTo("ﺑ" + ZWJ);
    }

    @Test
    void aJoiningControlSurvivesShapingSoABackendWithItsOwnShaperCanSeeIt() {
        // Consuming them here would leave the slide backend, which maps the forms back to
        // base letters and lets PowerPoint shape them, with no way to know the author had
        // forbidden a join: it would hand PowerPoint two letters that join right back up.
        // Nothing draws them — the PDF seam that measures and the one that draws share the
        // sanitizing that removes them — so keeping them costs nothing there.
        String shaped = ArabicShaper.shape("م" + ZWNJ + "ر" + ZWJ + "ح");

        assertThat(shaped).contains(ZWNJ).contains(ZWJ);
        assertThat(ArabicShaper.toBaseLetters(shaped))
                .describedAs("and the inverse the slide backend takes keeps them too")
                .isEqualTo("م" + ZWNJ + "ر" + ZWJ + "ح");
    }

    @Test
    void aJoiningControlBetweenLamAndAlefKeepsThemOutOfTheLigature() {
        String ligated = ArabicShaper.shape("لا");
        String separated = ArabicShaper.shape("ل" + ZWNJ + "ا");

        assertThat(ligated).hasSize(1);
        assertThat(separated)
                .describedAs("an author who broke the join must not get the ligature back")
                .isEqualTo("ﻝ" + ZWNJ + "ﺍ");
    }

    @Test
    void anAnnotationMarkBetweenTwoLettersIsTransparentToTheJoin() {
        // U+0610 is one of the Arabic annotation marks: general category Mn, and so
        // joining type T by Unicode's rule for anything it does not list explicitly. A
        // hand-written list of transparent ranges covered the vowel points and missed
        // these, and a mark that is not transparent unjoins the letters around it — the
        // word comes apart where an author added an honorific.
        assertThat(ArabicShaper.shape("بؐت"))
                .describedAs("beh keeps its initial form and teh its final one, with the "
                        + "mark still between them")
                .isEqualTo("ﺑؐﺖ");
    }

    @Test
    void aVowelPointBetweenTwoLettersIsStillTransparent() {
        assertThat(ArabicShaper.shape("بًت"))
                .describedAs("the case the range list did cover, held while the rule changed")
                .isEqualTo("ﺑًﺖ");
    }
}
