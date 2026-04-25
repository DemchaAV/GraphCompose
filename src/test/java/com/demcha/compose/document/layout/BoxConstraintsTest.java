package com.demcha.compose.document.layout;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoxConstraintsTest {

    @Test
    void naturalShouldUseTheUnboundedHeightSentinel() {
        BoxConstraints constraints = BoxConstraints.natural(420.0);

        assertThat(constraints.availableWidth()).isEqualTo(420.0);
        assertThat(constraints.availableHeight()).isEqualTo(BoxConstraints.UNBOUNDED_HEIGHT);
    }

    @Test
    void unboundedHeightShouldRemainACompatibilityAliasForNatural() {
        assertThat(BoxConstraints.unboundedHeight(320.0))
                .isEqualTo(BoxConstraints.natural(320.0));
    }

    @Test
    void naturalShouldUseRecordValidationForInvalidWidths() {
        assertThatThrownBy(() -> BoxConstraints.natural(-1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("availableWidth");

        assertThatThrownBy(() -> BoxConstraints.natural(Double.NaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("availableWidth");

        assertThatThrownBy(() -> BoxConstraints.natural(Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("availableWidth");
    }
}
