package com.demcha.compose.document.templates.core.text;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link TextOrnaments#spacedUpper(String)} to the exact strings it
 * produced in 2.3.0, character for character.
 *
 * <p>The method is deprecated and no built-in preset calls it any more,
 * which is precisely why it needs this: a deprecated method with no
 * callers inside the project is one nobody would notice breaking. It is
 * published Stable API in {@code graph-compose-templates}, so until a
 * major release removes it, "still compiles" is not enough &mdash; it has
 * to keep returning the same strings.</p>
 *
 * <p>These are not the outputs the replacement produces. Real tracking
 * spreads letters without touching the string, so
 * {@link TextOrnaments#upper(String)} returns {@code "JANE DOE"} where
 * this returns {@code "J A N E   D O E"}. That difference is the whole
 * point of the deprecation; the assertions below deliberately describe
 * the old behaviour.</p>
 */
@SuppressWarnings({"deprecation", "removal"})
class TextOrnamentsSpacedUpperCompatibilityTest {

    @Test
    void nullBecomesEmptyRatherThanThrowing() {
        assertThat(TextOrnaments.spacedUpper(null)).isEmpty();
    }

    @Test
    void emptyStaysEmpty() {
        assertThat(TextOrnaments.spacedUpper("")).isEmpty();
    }

    @Test
    void lettersAreSeparatedBySingleSpacesAndWordsByThree() {
        // One space between adjacent letters; the real word space keeps
        // itself and gains two more, so words read as separated.
        assertThat(TextOrnaments.spacedUpper("Jane Doe")).isEqualTo("J A N E   D O E");
    }

    @Test
    void digitsSpaceLikeLetters() {
        // isLetterOrDigit, so "R2" spaces between R and 2, and a digit
        // adjacent to a letter gets the same treatment either way round.
        assertThat(TextOrnaments.spacedUpper("R2 D2")).isEqualTo("R 2   D 2");
        assertThat(TextOrnaments.spacedUpper("A1B2")).isEqualTo("A 1 B 2");
    }

    @Test
    void punctuationBreaksTheRunAndIsNeverPaddedOnEitherSide() {
        // The space is emitted only when BOTH the current and the next
        // character are letters or digits, so punctuation sits tight
        // against its neighbours on both sides.
        assertThat(TextOrnaments.spacedUpper("O'Neill-Smith"))
                .isEqualTo("O'N E I L L-S M I T H");
        assertThat(TextOrnaments.spacedUpper("A.B")).isEqualTo("A.B");
        assertThat(TextOrnaments.spacedUpper("C++")).isEqualTo("C++");
    }

    @Test
    void everyWhitespaceCharacterKeepsItselfAndGainsTwoMore() {
        assertThat(TextOrnaments.spacedUpper("A B")).isEqualTo("A   B");
        // Each whitespace character is expanded independently, so a double
        // space becomes six.
        assertThat(TextOrnaments.spacedUpper("A  B")).isEqualTo("A      B");
        // Any Character.isWhitespace, not just the space glyph.
        assertThat(TextOrnaments.spacedUpper("A\tB")).isEqualTo("A\t  B");
        assertThat(TextOrnaments.spacedUpper("A\nB")).isEqualTo("A\n  B");
    }

    @Test
    void leadingAndTrailingWhitespaceIsExpandedRatherThanTrimmed() {
        assertThat(TextOrnaments.spacedUpper(" A ")).isEqualTo("   A   ");
    }

    @Test
    void aTrailingLetterOrDigitGetsNoSpaceAfterIt() {
        // The trailing unit is exactly what real tracking adds and this
        // transform does not — the reason 0.18em matches the old total
        // width rather than the old per-gap width.
        assertThat(TextOrnaments.spacedUpper("AB")).isEqualTo("A B");
        assertThat(TextOrnaments.spacedUpper("A")).isEqualTo("A");
    }

    @Test
    void theReplacementDeliberatelyDoesNotReproduceThis() {
        assertThat(TextOrnaments.upper("Jane Doe")).isEqualTo("JANE DOE");
        assertThat(TextOrnaments.spacedUpper("Jane Doe")).isEqualTo("J A N E   D O E");
    }
}
