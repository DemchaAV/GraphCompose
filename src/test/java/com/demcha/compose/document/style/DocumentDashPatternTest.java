package com.demcha.compose.document.style;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class DocumentDashPatternTest {

    @Test
    void noneIsSolidAndEmpty() {
        assertThat(DocumentDashPattern.NONE.isSolid()).isTrue();
        assertThat(DocumentDashPattern.NONE.segments()).isEmpty();
    }

    @Test
    void ofBuildsNonSolidPatternInOrder() {
        DocumentDashPattern dash = DocumentDashPattern.of(3, 2, 1);
        assertThat(dash.isSolid()).isFalse();
        assertThat(dash.segments()).containsExactly(3.0, 2.0, 1.0);
    }

    @Test
    void ofRejectsEmpty() {
        assertThatIllegalArgumentException().isThrownBy(DocumentDashPattern::of);
    }

    @Test
    void ofRejectsNonPositiveOrNonFiniteSegments() {
        assertThatIllegalArgumentException().isThrownBy(() -> DocumentDashPattern.of(3, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> DocumentDashPattern.of(-1));
        assertThatIllegalArgumentException().isThrownBy(() -> DocumentDashPattern.of(Double.NaN));
        assertThatIllegalArgumentException().isThrownBy(() -> DocumentDashPattern.of(Double.POSITIVE_INFINITY));
    }

    @Test
    void patternsAreValueEqual() {
        assertThat(DocumentDashPattern.of(4, 4)).isEqualTo(DocumentDashPattern.of(4, 4));
        assertThat(DocumentDashPattern.of(4, 4)).isNotEqualTo(DocumentDashPattern.of(4, 2));
    }

    @Test
    void constructorCopiesCallerListDefensively() {
        List<Double> mutable = new ArrayList<>(List.of(2.0, 2.0));
        DocumentDashPattern dash = new DocumentDashPattern(mutable);
        mutable.set(0, 99.0);
        assertThat(dash.segments()).containsExactly(2.0, 2.0);
    }
}
