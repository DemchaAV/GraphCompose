package com.demcha.Templatese;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CvThemeTest {

    @Test
    @SuppressWarnings("deprecation")
    void timesRomanAliasShouldMatchLegacyFactory() {
        CvTheme legacy = CvTheme.timeRoman();
        CvTheme alias = CvTheme.timesRoman();

        assertThat(alias).isEqualTo(legacy);
    }

    @Test
    void moduleMarginAliasShouldExposeLegacyRecordComponent() {
        CvTheme theme = CvTheme.defaultTheme();

        assertThat(theme.moduleMargin()).isEqualTo(theme.modulMargin());
    }
}
