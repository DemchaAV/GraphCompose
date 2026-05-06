package com.demcha.compose.document.templates.decorations;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.SpacerNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpacerTest {

    @Test
    void smallShouldEmitFourPointVerticalSpacer() {
        DocumentNode node = Spacer.small();

        assertThat(node).isInstanceOf(SpacerNode.class);
        SpacerNode spacer = (SpacerNode) node;
        assertThat(spacer.height()).isEqualTo(Spacer.SMALL).isEqualTo(4.0);
        assertThat(spacer.width()).isZero();
        assertThat(spacer.name()).isEqualTo("Spacer.small");
    }

    @Test
    void mediumShouldEmitEightPointVerticalSpacer() {
        SpacerNode spacer = (SpacerNode) Spacer.medium();
        assertThat(spacer.height()).isEqualTo(Spacer.MEDIUM).isEqualTo(8.0);
        assertThat(spacer.width()).isZero();
        assertThat(spacer.name()).isEqualTo("Spacer.medium");
    }

    @Test
    void largeShouldEmitSixteenPointVerticalSpacer() {
        SpacerNode spacer = (SpacerNode) Spacer.large();
        assertThat(spacer.height()).isEqualTo(Spacer.LARGE).isEqualTo(16.0);
        assertThat(spacer.width()).isZero();
        assertThat(spacer.name()).isEqualTo("Spacer.large");
    }

    @Test
    void heightShouldEmitVerticalSpacerOfRequestedHeight() {
        SpacerNode spacer = (SpacerNode) Spacer.height(13.5);
        assertThat(spacer.height()).isEqualTo(13.5);
        assertThat(spacer.width()).isZero();
    }

    @Test
    void sizeShouldEmitFullySizedSpacer() {
        SpacerNode spacer = (SpacerNode) Spacer.size(7.0, 11.0);
        assertThat(spacer.width()).isEqualTo(7.0);
        assertThat(spacer.height()).isEqualTo(11.0);
    }

    @Test
    void heightZeroShouldBeAcceptedSpacerNodeAllowsZero() {
        SpacerNode spacer = (SpacerNode) Spacer.height(0.0);
        assertThat(spacer.height()).isZero();
    }

    @Test
    void heightNegativeShouldThrow() {
        assertThatThrownBy(() -> Spacer.height(-1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("height");
    }

    @Test
    void heightInfiniteShouldThrow() {
        assertThatThrownBy(() -> Spacer.height(Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void heightNanShouldThrow() {
        assertThatThrownBy(() -> Spacer.height(Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
