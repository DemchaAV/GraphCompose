package com.demcha.compose.document.templates.cv.v2.theme;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CvDecorationTest {

    @Test
    void classic_carries_the_canonical_glyphs() {
        CvDecoration d = CvDecoration.classic();
        assertThat(d.bulletGlyph()).isEqualTo("• ");
        assertThat(d.stackedIndent()).isEqualTo("  ");
        assertThat(d.contactSeparator()).isEqualTo("   |   ");
    }

    @Test
    void rejects_null_field() {
        assertThatThrownBy(() -> new CvDecoration(null, "  ", " | "))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("bulletGlyph");
    }

    @Test
    void custom_decoration_is_carried_verbatim() {
        CvDecoration d = new CvDecoration("▶ ", "   ", "  ·  ");
        assertThat(d.bulletGlyph()).isEqualTo("▶ ");
        assertThat(d.stackedIndent()).isEqualTo("   ");
        assertThat(d.contactSeparator()).isEqualTo("  ·  ");
    }

    @Test
    @SuppressWarnings("deprecation")
    void deprecated_three_arg_theme_constructor_supplies_classic_decoration() {
        // The pre-decoration call shape must keep working and yield
        // the classic decoration, so older callers keep rendering
        // pixel-identical output.
        CvTheme theme = new CvTheme(
                CvPalette.classic(),
                CvTypography.classic(),
                CvSpacing.classic());
        assertThat(theme.decoration()).isEqualTo(CvDecoration.classic());
    }

    @Test
    void canonical_four_arg_theme_constructor_carries_custom_decoration() {
        CvDecoration custom = new CvDecoration("· ", "  ", "  /  ");
        CvTheme theme = new CvTheme(
                CvPalette.classic(),
                CvTypography.classic(),
                CvSpacing.classic(),
                custom);
        assertThat(theme.decoration()).isSameAs(custom);
    }

    @Test
    void boxedClassic_carries_classic_decoration() {
        assertThat(CvTheme.boxedClassic().decoration())
                .isEqualTo(CvDecoration.classic());
    }
}
