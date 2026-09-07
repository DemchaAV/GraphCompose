package com.demcha.compose.document.style;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * The flow-width value itself: what it accepts, what it refuses, and how it
 * resolves against the width a parent offers.
 */
class DocumentFlowWidthTest {

    @Test
    void naturalIsUnconstrainedAndResolvesToWhateverTheParentOffers() {
        DocumentFlowWidth natural = DocumentFlowWidth.natural();

        assertThat(natural.isFixed()).isFalse();
        assertThat(natural.points()).isZero();
        assertThat(natural.resolve(500)).isEqualTo(500);
        assertThat(natural.resolve(0)).isZero();
    }

    @Test
    void naturalIsASingleton() {
        assertThat(DocumentFlowWidth.natural()).isSameAs(DocumentFlowWidth.natural());
    }

    @Test
    void fixedWidthResolvesToTheRequestedWidthWhenTheParentCanGiveIt() {
        DocumentFlowWidth fixed = DocumentFlowWidth.of(240);

        assertThat(fixed.isFixed()).isTrue();
        assertThat(fixed.points()).isEqualTo(240.0);
        assertThat(fixed.resolve(500)).isEqualTo(240.0);
        assertThat(fixed.resolve(240)).isEqualTo(240.0);
    }

    @Test
    void fixedWidthIsClampedToTheParentAvailableWidth() {
        assertThat(DocumentFlowWidth.of(900).resolve(360)).isEqualTo(360.0);
        assertThat(DocumentFlowWidth.of(900).resolve(0)).isZero();
    }

    @Test
    void resolveIsIdempotentSoASecondNarrowingCannotShrinkTheBoxAgain() {
        DocumentFlowWidth fixed = DocumentFlowWidth.of(180);

        double once = fixed.resolve(400);

        assertThat(fixed.resolve(once)).isEqualTo(once);
    }

    @Test
    void ofRejectsZero() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> DocumentFlowWidth.of(0))
                .withMessageContaining("greater than zero");
    }

    @Test
    void ofRejectsNegativeWidth() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> DocumentFlowWidth.of(-1))
                .withMessageContaining("greater than zero");
    }

    @Test
    void ofRejectsNaN() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> DocumentFlowWidth.of(Double.NaN))
                .withMessageContaining("finite");
    }

    @Test
    void ofRejectsInfinity() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> DocumentFlowWidth.of(Double.POSITIVE_INFINITY))
                .withMessageContaining("finite");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> DocumentFlowWidth.of(Double.NEGATIVE_INFINITY))
                .withMessageContaining("finite");
    }

    @Test
    void theCanonicalConstructorRejectsNonFiniteAndNegativeValuesToo() {
        assertThatIllegalArgumentException().isThrownBy(() -> new DocumentFlowWidth(Double.NaN));
        assertThatIllegalArgumentException().isThrownBy(() -> new DocumentFlowWidth(Double.POSITIVE_INFINITY));
        assertThatIllegalArgumentException().isThrownBy(() -> new DocumentFlowWidth(-3));
    }
}
