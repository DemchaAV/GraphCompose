package com.demcha.compose.document.templates.core.theme;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DecorationTest {

    @Test
    void classic_carries_the_canonical_glyphs() {
        Decoration d = Decoration.classic();
        assertThat(d.bulletGlyph()).isEqualTo("• ");
        assertThat(d.stackedIndent()).isEqualTo("  ");
        assertThat(d.contactSeparator()).isEqualTo("   |   ");
    }

    @Test
    void rejects_null_field() {
        assertThatThrownBy(() -> new Decoration(null, "  ", " | "))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("bulletGlyph");
    }

    @Test
    void custom_decoration_is_carried_verbatim() {
        Decoration d = new Decoration("▶ ", "   ", "  ·  ");
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
        BrandTheme theme = new BrandTheme(
                Palette.classic(),
                Typography.classic(),
                Spacing.classic());
        assertThat(theme.decoration()).isEqualTo(Decoration.classic());
    }

    @Test
    void canonical_four_arg_theme_constructor_carries_custom_decoration() {
        Decoration custom = new Decoration("· ", "  ", "  /  ");
        BrandTheme theme = new BrandTheme(
                Palette.classic(),
                Typography.classic(),
                Spacing.classic(),
                custom);
        assertThat(theme.decoration()).isSameAs(custom);
    }

    @Test
    void boxedClassic_carries_classic_decoration() {
        assertThat(BrandTheme.boxedClassic().decoration())
                .isEqualTo(Decoration.classic());
    }
}
