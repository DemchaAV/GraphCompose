package com.demcha.compose.devtool;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PreviewScaleResolverTest {

    @Test
    void shouldUseDefaultScaleForMissingOrInvalidValues() {
        assertThat(PreviewScaleResolver.fromProperty(null)).isEqualTo(PreviewScaleResolver.DEFAULT_SCALE);
        assertThat(PreviewScaleResolver.fromProperty("")).isEqualTo(PreviewScaleResolver.DEFAULT_SCALE);
        assertThat(PreviewScaleResolver.fromProperty("abc")).isEqualTo(PreviewScaleResolver.DEFAULT_SCALE);
        assertThat(PreviewScaleResolver.fromProperty("Infinity")).isEqualTo(PreviewScaleResolver.DEFAULT_SCALE);
    }

    @Test
    void shouldClampScaleIntoSupportedRange() {
        assertThat(PreviewScaleResolver.fromProperty("0.25")).isEqualTo(PreviewScaleResolver.MIN_SCALE);
        assertThat(PreviewScaleResolver.fromProperty("1.5")).isEqualTo(1.5f);
        assertThat(PreviewScaleResolver.fromProperty("3.0")).isEqualTo(PreviewScaleResolver.MAX_SCALE);
    }
}
