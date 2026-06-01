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

    // --- Contemporary themes added in v1.6.8 -------------------------------

    @Test
    void nordicThemeHasAllTokensNonNullAndUsesGenerousSpacing() {
        BusinessTheme theme = BusinessTheme.nordic();
        assertThat(theme.name()).isEqualTo("nordic");
        assertThat(theme.palette()).isNotNull();
        assertThat(theme.spacing()).isNotNull();
        assertThat(theme.text()).isNotNull();
        assertThat(theme.table()).isNotNull();
        assertThat(theme.pageBackground()).isNull();
        // Nordic is tuned for whitespace — every step is at least as wide
        // as the default scale, and md is strictly wider.
        SpacingScale standard = SpacingScale.defaultScale();
        assertThat(theme.spacing().md()).isGreaterThan(standard.md());
        assertThat(theme.spacing().xl()).isGreaterThan(standard.xl());
    }

    @Test
    void editorialThemeUsesTimesRomanBodyAndCreamPageBackground() {
        BusinessTheme theme = BusinessTheme.editorial();
        assertThat(theme.name()).isEqualTo("editorial");
        assertThat(theme.text().body().fontName().name()).isEqualTo("Times-Roman");
        assertThat(theme.text().h1().fontName().name()).isEqualTo("Times-Roman");
        // Cream page background distinguishes editorial from the
        // strictly-white classic theme.
        assertThat(theme.pageBackground()).isNotNull();
        assertThat(theme.pageBackground()).isEqualTo(theme.palette().surface());
    }

    @Test
    void cinematicThemeInvertsPaletteToLightTextOnDarkSurface() {
        BusinessTheme theme = BusinessTheme.cinematic();
        assertThat(theme.name()).isEqualTo("cinematic");
        // The defining trait: surface is dark, primary/text is light.
        Color surface = theme.palette().surface().color();
        Color textPrimary = theme.palette().textPrimary().color();
        int surfaceLuminance = (surface.getRed() + surface.getGreen() + surface.getBlue()) / 3;
        int textLuminance = (textPrimary.getRed() + textPrimary.getGreen() + textPrimary.getBlue()) / 3;
        assertThat(surfaceLuminance).isLessThan(64);   // genuinely dark surface
        assertThat(textLuminance).isGreaterThan(192);  // genuinely light text
        // And the surface doubles as the page background so the moody
        // look fills the page edges too.
        assertThat(theme.pageBackground()).isEqualTo(theme.palette().surface());
    }

    @Test
    void monochromeThemeIsPureBlackOnWhiteWithBoldYellowAccent() {
        BusinessTheme theme = BusinessTheme.monochrome();
        assertThat(theme.name()).isEqualTo("monochrome");
        assertThat(theme.palette().primary().color()).isEqualTo(Color.BLACK);
        assertThat(theme.palette().surface().color()).isEqualTo(Color.WHITE);
        // The single accent is the entire identity of the theme — assert
        // it leans yellow (R and G high, B low) rather than pinning the
        // exact RGB, so a future shade tweak does not break the test.
        Color accent = theme.palette().accent().color();
        assertThat(accent.getRed()).isGreaterThan(200);
        assertThat(accent.getGreen()).isGreaterThan(150);
        assertThat(accent.getBlue()).isLessThan(80);
    }

    @Test
    void allSevenBuiltInThemesArePairwiseDistinctByPalette() {
        BusinessTheme[] all = {
                BusinessTheme.classic(),
                BusinessTheme.modern(),
                BusinessTheme.executive(),
                BusinessTheme.nordic(),
                BusinessTheme.editorial(),
                BusinessTheme.cinematic(),
                BusinessTheme.monochrome()
        };
        for (int i = 0; i < all.length; i++) {
            for (int j = i + 1; j < all.length; j++) {
                assertThat(all[i].palette())
                        .as("Themes '%s' and '%s' must have distinct palettes",
                                all[i].name(), all[j].name())
                        .isNotEqualTo(all[j].palette());
                assertThat(all[i].name()).isNotEqualTo(all[j].name());
            }
        }
    }
}
