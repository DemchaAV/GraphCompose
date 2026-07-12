package com.demcha.compose.document.layout;

import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.engine.components.style.Margin;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the canonical-insets → engine-margin conversion that the document API
 * relies on. Distinct edge values catch a field-order swap — the exact mistake a
 * future refactor of this bridge could make while the higher-level snapshot suites
 * (which mostly use symmetric margins) stay green.
 */
class LayoutInsetsTest {

    @Test
    void nullInsetsBecomeZeroMargin() {
        assertThat(LayoutInsets.toMargin(null)).isEqualTo(Margin.zero());
    }

    @Test
    void insetsMapToMarginPreservingEachEdge() {
        Margin margin = LayoutInsets.toMargin(new DocumentInsets(1.0, 2.0, 3.0, 4.0));

        assertThat(margin.top()).isEqualTo(1.0);
        assertThat(margin.right()).isEqualTo(2.0);
        assertThat(margin.bottom()).isEqualTo(3.0);
        assertThat(margin.left()).isEqualTo(4.0);
    }
}
