package com.demcha.compose.document.style;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Value-level contract of {@link DocumentColor}'s translucency API: the
 * explicit {@code withOpacity} guard and rounding, and the alpha channel
 * carried by {@code rgba}. These are public {@code @since 1.8.0} contracts
 * that otherwise had no direct unit test.
 */
class DocumentColorTest {

    @Test
    void withOpacityRejectsOutOfRangeAndNaN() {
        assertThatThrownBy(() -> DocumentColor.WHITE.withOpacity(-0.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("opacity must be in [0,1]");
        assertThatThrownBy(() -> DocumentColor.WHITE.withOpacity(1.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("opacity must be in [0,1]");
        assertThatThrownBy(() -> DocumentColor.WHITE.withOpacity(Double.NaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("opacity must be in [0,1]");
    }

    @Test
    void withOpacityMapsTheBoundariesToAlphaZeroAndFull() {
        assertThat(DocumentColor.BLACK.withOpacity(0.0).color().getAlpha()).isEqualTo(0);
        assertThat(DocumentColor.BLACK.withOpacity(1.0).color().getAlpha()).isEqualTo(255);
        // 0.5 rounds to 128 (Math.round of 127.5).
        assertThat(DocumentColor.BLACK.withOpacity(0.5).color().getAlpha()).isEqualTo(128);
    }

    @Test
    void withOpacityKeepsTheRgbChannelsAndReturnsAFreshInstance() {
        DocumentColor base = DocumentColor.rgb(10, 20, 30);
        DocumentColor faded = base.withOpacity(0.5);

        assertThat(faded).isNotSameAs(base);
        assertThat(faded.color().getRed()).isEqualTo(10);
        assertThat(faded.color().getGreen()).isEqualTo(20);
        assertThat(faded.color().getBlue()).isEqualTo(30);
        // The source is unchanged (immutable value type).
        assertThat(base.color().getAlpha()).isEqualTo(255);
    }

    @Test
    void rgbaCarriesTheAlphaChannel() {
        DocumentColor color = DocumentColor.rgba(10, 20, 30, 128);

        assertThat(color.color().getAlpha()).isEqualTo(128);
        assertThat(color.color().getRed()).isEqualTo(10);
    }
}
