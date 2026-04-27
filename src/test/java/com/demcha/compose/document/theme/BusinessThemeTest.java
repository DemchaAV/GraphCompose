package com.demcha.compose.document.theme;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class BusinessThemeTest {

    private static final double EPS = 1e-6;

    @Test
    void spacingScaleDefaultIsMonotonicAndProducesSymmetricInsets() {
        SpacingScale scale = SpacingScale.defaultScale();
        assertThat(scale.xs()).isEqualTo(4.0, within(EPS));
        assertThat(scale.sm()).isEqualTo(8.0, within(EPS));
        assertThat(scale.md()).isEqualTo(12.0, within(EPS));
        assertThat(scale.lg()).isEqualTo(20.0, within(EPS));
        assertThat(scale.xl()).isEqualTo(32.0, within(EPS));

        assertThat(scale.insetsMd().top()).isEqualTo(12.0, within(EPS));
        assertThat(scale.insetsMd().left()).isEqualTo(12.0, within(EPS));
        assertThat(scale.insetsMd().right()).isEqualTo(12.0, within(EPS));
        assertThat(scale.insetsMd().bottom()).isEqualTo(12.0, within(EPS));
    }

    @Test
    void spacingScaleRejectsNonMonotonicSteps() {
        assertThatThrownBy(() -> new SpacingScale(8.0, 4.0, 12.0, 20.0, 32.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-decreasing");
    }

    @Test
    void spacingScaleRejectsNegativeOrInfiniteStep() {
        assertThatThrownBy(() -> new SpacingScale(-1.0, 4.0, 8.0, 12.0, 16.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
        assertThatThrownBy(() -> new SpacingScale(4.0, 8.0, Double.NaN, 12.0, 16.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void documentPaletteOfFactoryWrapsAwtColors() {
        DocumentPalette palette = DocumentPalette.of(
                Color.BLUE, Color.RED, Color.WHITE, Color.LIGHT_GRAY,
                Color.BLACK, Color.GRAY, Color.DARK_GRAY);
        assertThat(palette.primary().color()).isEqualTo(Color.BLUE);
        assertThat(palette.accent().color()).isEqualTo(Color.RED);
        assertThat(palette.surface().color()).isEqualTo(Color.WHITE);
    }

    @Test
    void documentPaletteRejectsNullToken() {
        assertThatThrownBy(() -> new DocumentPalette(
                null, DocumentColor.BLACK, DocumentColor.WHITE, DocumentColor.WHITE,
                DocumentColor.BLACK, DocumentColor.BLACK, DocumentColor.BLACK))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("primary");
    }

    @Test
    void classicThemeHasAllTokensNonNull() {
        BusinessTheme theme = BusinessTheme.classic();
        assertThat(theme.name()).isEqualTo("classic");
        assertThat(theme.palette()).isNotNull();
        assertThat(theme.spacing()).isNotNull();
        assertThat(theme.text()).isNotNull();
        assertThat(theme.table()).isNotNull();
        assertThat(theme.pageBackground()).isNull();
    }

    @Test
    void modernThemeAppliesCreamPageBackground() {
        BusinessTheme theme = BusinessTheme.modern();
        assertThat(theme.name()).isEqualTo("modern");
        assertThat(theme.pageBackground()).isNotNull();
        assertThat(theme.pageBackground()).isEqualTo(theme.palette().surface());
    }

    @Test
    void executiveThemeUsesTimesRomanForHeadings() {
        BusinessTheme theme = BusinessTheme.executive();
        assertThat(theme.name()).isEqualTo("executive");
        assertThat(theme.text().h1().fontName().name()).isEqualTo("Times-Roman");
        assertThat(theme.text().h1().decoration()).isEqualTo(DocumentTextDecoration.BOLD);
    }

    @Test
    void textScaleResolvesAccentToBoldAccentColor() {
        BusinessTheme theme = BusinessTheme.classic();
        assertThat(theme.text().accent().color()).isEqualTo(theme.palette().accent());
        assertThat(theme.text().accent().decoration()).isEqualTo(DocumentTextDecoration.BOLD);
    }

    @Test
    void textScaleBodyAndCaptionShareTheBodyFont() {
        BusinessTheme theme = BusinessTheme.classic();
        assertThat(theme.text().body().fontName()).isEqualTo(theme.text().caption().fontName());
        assertThat(theme.text().body().decoration()).isEqualTo(DocumentTextDecoration.DEFAULT);
        assertThat(theme.text().caption().decoration()).isEqualTo(DocumentTextDecoration.DEFAULT);
        assertThat(theme.text().body().size()).isGreaterThan(theme.text().caption().size());
    }

    @Test
    void tablePresetCellsShareTheSpacingMdPaddingAndRuleStroke() {
        BusinessTheme theme = BusinessTheme.classic();
        assertThat(theme.table().defaultCellStyle().padding().top())
                .isEqualTo(theme.spacing().sm(), within(EPS));
        assertThat(theme.table().defaultCellStyle().stroke().color())
                .isEqualTo(theme.palette().rule());
        assertThat(theme.table().headerStyle().fillColor())
                .isEqualTo(theme.palette().surfaceMuted());
    }

    @Test
    void withPageBackgroundReturnsForkedThemeWithoutMutatingOriginal() {
        BusinessTheme classic = BusinessTheme.classic();
        DocumentColor cream = DocumentColor.of(new Color(252, 248, 240));
        BusinessTheme withBg = classic.withPageBackground(cream);

        assertThat(withBg.pageBackground()).isEqualTo(cream);
        assertThat(classic.pageBackground()).isNull();
        assertThat(withBg.palette()).isSameAs(classic.palette());
        assertThat(withBg.name()).isEqualTo(classic.name());
    }

    @Test
    void withNameReturnsForkedThemeWithoutMutatingOriginal() {
        BusinessTheme classic = BusinessTheme.classic();
        BusinessTheme renamed = classic.withName("acme-classic");
        assertThat(renamed.name()).isEqualTo("acme-classic");
        assertThat(classic.name()).isEqualTo("classic");
        assertThat(renamed.palette()).isSameAs(classic.palette());
    }

    @Test
    void allThreeBuiltInThemesAreDistinct() {
        BusinessTheme classic = BusinessTheme.classic();
        BusinessTheme modern = BusinessTheme.modern();
        BusinessTheme executive = BusinessTheme.executive();

        assertThat(classic.palette()).isNotEqualTo(modern.palette());
        assertThat(modern.palette()).isNotEqualTo(executive.palette());
        assertThat(classic.text().h1()).isNotEqualTo(modern.text().h1());
    }
}
