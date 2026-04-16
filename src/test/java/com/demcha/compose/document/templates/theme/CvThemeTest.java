package com.demcha.compose.document.templates.theme;

import com.demcha.compose.font_library.FontName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CvThemeTest {

    @Test
    void timesRomanFactoryShouldUseTimesRomanForHeaderAndBodyFonts() {
        CvTheme theme = CvTheme.timesRoman();

        assertThat(theme.headerFont()).isEqualTo(FontName.TIMES_ROMAN);
        assertThat(theme.bodyFont()).isEqualTo(FontName.TIMES_ROMAN);
    }

    @Test
    void moduleMarginAliasShouldExposeRecordComponent() {
        CvTheme theme = CvTheme.defaultTheme();

        assertThat(theme.moduleMargin()).isEqualTo(theme.modulMargin());
    }
}
