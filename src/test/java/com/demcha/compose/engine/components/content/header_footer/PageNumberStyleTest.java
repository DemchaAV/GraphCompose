package com.demcha.compose.engine.components.content.header_footer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the numeral formatting owned by {@link PageNumberStyle}: decimal, Roman,
 * and alphabetic styles, plus graceful fallback for out-of-range values.
 */
class PageNumberStyleTest {

    @Test
    void decimalIsPlainArabic() {
        assertThat(PageNumberStyle.DECIMAL.format(7)).isEqualTo("7");
        assertThat(PageNumberStyle.DECIMAL.format(2026)).isEqualTo("2026");
    }

    @Test
    void romanCoversBothCases() {
        assertThat(PageNumberStyle.LOWER_ROMAN.format(4)).isEqualTo("iv");
        assertThat(PageNumberStyle.UPPER_ROMAN.format(9)).isEqualTo("IX");
        assertThat(PageNumberStyle.UPPER_ROMAN.format(2024)).isEqualTo("MMXXIV");
        assertThat(PageNumberStyle.LOWER_ROMAN.format(1)).isEqualTo("i");
    }

    @Test
    void alphaIsBijectiveBase26() {
        assertThat(PageNumberStyle.LOWER_ALPHA.format(1)).isEqualTo("a");
        assertThat(PageNumberStyle.LOWER_ALPHA.format(26)).isEqualTo("z");
        assertThat(PageNumberStyle.LOWER_ALPHA.format(27)).isEqualTo("aa");
        assertThat(PageNumberStyle.UPPER_ALPHA.format(1)).isEqualTo("A");
        assertThat(PageNumberStyle.UPPER_ALPHA.format(28)).isEqualTo("AB");
    }

    @Test
    void outOfRangeFallsBackToDecimalWithoutThrowing() {
        assertThat(PageNumberStyle.UPPER_ROMAN.format(0)).isEqualTo("0");
        assertThat(PageNumberStyle.LOWER_ROMAN.format(4000)).isEqualTo("4000");
        assertThat(PageNumberStyle.LOWER_ALPHA.format(0)).isEqualTo("0");
        assertThat(PageNumberStyle.UPPER_ALPHA.format(-3)).isEqualTo("-3");
    }
}
