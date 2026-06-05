package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.LineNode;
import com.demcha.compose.document.style.DocumentDashPattern;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Covers {@link LineBuilder#dashed} carrying a {@link DocumentDashPattern} into
 * the built {@link LineNode} without disturbing the stroke.
 */
class LineBuilderTest {

    @Test
    void linesAreSolidByDefault() {
        LineNode node = new LineBuilder().horizontal(120).build();
        assertThat(node.dashPattern()).isEqualTo(DocumentDashPattern.NONE);
        assertThat(node.dashPattern().isSolid()).isTrue();
    }

    @Test
    void dashedVarargsCarriesPatternIntoNode() {
        LineNode node = new LineBuilder().horizontal(120).dashed(4, 3).build();
        assertThat(node.dashPattern().segments()).containsExactly(4.0, 3.0);
    }

    @Test
    void dashedDefaultUsesBalancedPattern() {
        LineNode node = new LineBuilder().horizontal(120).dashed().build();
        assertThat(node.dashPattern().segments()).containsExactly(3.0, 2.0);
    }

    @Test
    void dashedNullPatternRestoresSolid() {
        LineNode node = new LineBuilder()
                .horizontal(120)
                .dashed(5, 5)
                .dashed((DocumentDashPattern) null)
                .build();
        assertThat(node.dashPattern().isSolid()).isTrue();
    }

    @Test
    void dashedRejectsInvalidPattern() {
        assertThatIllegalArgumentException().isThrownBy(() -> new LineBuilder().dashed(0));
    }
}
