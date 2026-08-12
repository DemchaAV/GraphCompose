package com.demcha.compose.engine.text.bidi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the one property {@code BidiText} exists for: reversal by grapheme cluster,
 * not by character.
 *
 * <p>Without these tests the {@code BreakIterator} could be replaced with
 * {@code new StringBuilder(text).reverse()} and every other test would stay green —
 * the rest of the suite uses unpointed Hebrew, which the naive reversal happens to
 * get right. Pointed text and emoji are where the two part ways.</p>
 */
class BidiTextTest {

    private static final String ALEPH = "א";
    private static final String QAMATS = "ָ";
    private static final String BET = "ב";

    @Test
    void emptyAndSingleCharacterPassThrough() {
        assertThat(BidiText.reverseForDisplay("")).isEmpty();
        assertThat(BidiText.reverseForDisplay(null)).isEmpty();
        assertThat(BidiText.reverseForDisplay("a")).isEqualTo("a");
    }

    @Test
    void plainTextReversesCharacterByCharacter() {
        assertThat(BidiText.reverseForDisplay("abc")).isEqualTo("cba");
    }

    @Test
    void aCombiningMarkStaysOnItsBaseLetter() {
        // Aleph with qamats, then bet. The vowel point belongs to the aleph; reversing
        // characters would move it in front of the letter, which changes what the
        // reader sees rather than only where it sits.
        String pointed = ALEPH + QAMATS + BET;

        assertThat(BidiText.reverseForDisplay(pointed))
                .isEqualTo(BET + ALEPH + QAMATS);
        assertThat(BidiText.reverseForDisplay(pointed))
                .describedAs("this is exactly the case a character-wise reversal gets wrong")
                .isNotEqualTo(new StringBuilder(pointed).reverse().toString());
    }

    @Test
    void aSurrogatePairSurvivesReversal() {
        // A character-wise reversal would swap the surrogate halves and corrupt the
        // emoji into two unpaired surrogates.
        assertThat(BidiText.reverseForDisplay("a🎉b"))
                .isEqualTo("b🎉a");
    }

    @Test
    void reversalIsItsOwnInverse() {
        String mixed = ALEPH + QAMATS + BET + " x🎉";

        assertThat(BidiText.reverseForDisplay(BidiText.reverseForDisplay(mixed)))
                .isEqualTo(mixed);
    }
}
