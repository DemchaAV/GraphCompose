package com.demcha.compose.document.style;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

/**
 * The tracking value itself: what it accepts, what it refuses, and how it
 * resolves against the size the text is actually set at.
 */
class DocumentLetterSpacingTest {

    @Test
    void noneResolvesToZeroAtEverySize() {
        assertThat(DocumentLetterSpacing.NONE.isNone()).isTrue();
        assertThat(DocumentLetterSpacing.NONE.resolve(9)).isZero();
        assertThat(DocumentLetterSpacing.NONE.resolve(24)).isZero();
        assertThat(DocumentLetterSpacing.NONE.resolve(0)).isZero();
    }

    @Test
    void pointsResolveToThemselvesRegardlessOfFontSize() {
        DocumentLetterSpacing spacing = DocumentLetterSpacing.points(1.2);

        assertThat(spacing.type()).isEqualTo(DocumentLetterSpacing.Type.POINTS);
        assertThat(spacing.resolve(9)).isEqualTo(1.2);
        assertThat(spacing.resolve(24)).isEqualTo(1.2);
    }

    @Test
    void aFontSizeShareScalesWithTheType() {
        DocumentLetterSpacing spacing = DocumentLetterSpacing.ofFontSize(0.12);

        assertThat(spacing.type()).isEqualTo(DocumentLetterSpacing.Type.FONT_SIZE);
        assertThat(spacing.resolve(24)).isEqualTo(2.88);
        assertThat(spacing.resolve(10)).isEqualTo(1.2);
    }

    @Test
    void theTwoUnitsAreDistinguishableAtTheSameNumber() {
        // The whole reason this is a value type and not a bare double: 1.2 as
        // points and 1.2 as a share of the font size are wildly different, and
        // a double could not say which was meant.
        assertThat(DocumentLetterSpacing.points(1.2)).isNotEqualTo(DocumentLetterSpacing.ofFontSize(1.2));
        // Points pass straight through, so this one is exact. The share is a
        // product of two doubles (1.2 * 24 lands on 28.799999999999997), so it
        // is asserted the way a float product has to be.
        assertThat(DocumentLetterSpacing.points(1.2).resolve(24)).isEqualTo(1.2);
        assertThat(DocumentLetterSpacing.ofFontSize(1.2).resolve(24)).isCloseTo(28.8, within(1e-9));
    }

    @Test
    void negativeTrackingIsAllowedAndTightens() {
        assertThat(DocumentLetterSpacing.points(-0.5).resolve(12)).isEqualTo(-0.5);
        assertThat(DocumentLetterSpacing.ofFontSize(-0.05).resolve(20)).isEqualTo(-1.0);
    }

    @Test
    void zeroInEitherUnitFoldsOntoTheNeutralValue() {
        assertThat(DocumentLetterSpacing.points(0)).isSameAs(DocumentLetterSpacing.NONE);
        assertThat(DocumentLetterSpacing.ofFontSize(0)).isSameAs(DocumentLetterSpacing.NONE);
        assertThat(DocumentLetterSpacing.points(-0.0)).isSameAs(DocumentLetterSpacing.NONE);
    }

    @Test
    void negativeZeroFoldsOntoPositiveZeroSoOneBehaviourHasOneValue() {
        DocumentLetterSpacing minusZero = new DocumentLetterSpacing(DocumentLetterSpacing.Type.FONT_SIZE, -0.0);

        assertThat(minusZero.value()).isEqualTo(0.0);
        assertThat(minusZero.isNone()).isTrue();
        assertThat(minusZero).isEqualTo(new DocumentLetterSpacing(DocumentLetterSpacing.Type.FONT_SIZE, 0.0));
    }

    @Test
    void aNonFiniteAmountIsRefused() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> DocumentLetterSpacing.points(Double.NaN));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> DocumentLetterSpacing.ofFontSize(Double.POSITIVE_INFINITY));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> DocumentLetterSpacing.points(Double.NEGATIVE_INFINITY));
    }

    @Test
    void aNullUnitIsRefused() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DocumentLetterSpacing(null, 1.0));
    }

    @Test
    void aNonFiniteFontSizeMakesTheTrackingTermZeroNotNaN() {
        // Bounds this term only. The rest of the measurement still multiplies
        // glyph widths by the same bad font size, so this does not claim to
        // make the resulting text width finite.
        assertThat(DocumentLetterSpacing.ofFontSize(0.12).resolve(Double.NaN)).isZero();
        assertThat(DocumentLetterSpacing.ofFontSize(0.12).resolve(Double.POSITIVE_INFINITY)).isZero();
    }
}
