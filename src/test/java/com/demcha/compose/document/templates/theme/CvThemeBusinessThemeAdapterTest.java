package com.demcha.compose.document.templates.theme;

import com.demcha.compose.document.theme.BusinessTheme;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ADR 0002 — verifies the {@link CvTheme#fromBusinessTheme(BusinessTheme)}
 * bridge derives every CV-specific token from the supplied business
 * theme so a project that already picked a {@link BusinessTheme} for
 * its invoices and proposals can produce a visually-matching CV theme
 * without re-declaring the visual tokens.
 *
 * @author Artem Demchyshyn
 */
class CvThemeBusinessThemeAdapterTest {

    @Test
    void coloursAreDerivedFromBusinessThemePalette() {
        BusinessTheme theme = BusinessTheme.modern();
        CvTheme adapted = CvTheme.fromBusinessTheme(theme);

        assertThat(adapted.primaryColor())
                .as("primary colour follows the business primary slot")
                .isEqualTo(theme.palette().primary().color());
        assertThat(adapted.bodyColor())
                .as("body colour follows the business textPrimary slot")
                .isEqualTo(theme.palette().textPrimary().color());
        assertThat(adapted.accentColor())
                .as("accent colour follows the business accent slot")
                .isEqualTo(theme.palette().accent().color());
        assertThat(adapted.secondaryColor())
                .as("section-heading colour follows the business accent slot")
                .isEqualTo(theme.palette().accent().color());
    }

    @Test
    void fontChoicesFollowTheBusinessTextScale() {
        BusinessTheme theme = BusinessTheme.modern();
        CvTheme adapted = CvTheme.fromBusinessTheme(theme);

        assertThat(adapted.headerFont())
                .as("header font follows the business h1 font")
                .isEqualTo(theme.text().h1().fontName());
        assertThat(adapted.bodyFont())
                .as("body font follows the business body font")
                .isEqualTo(theme.text().body().fontName());
    }

    @Test
    void fontSizesFollowTheBusinessTextScale() {
        BusinessTheme theme = BusinessTheme.modern();
        CvTheme adapted = CvTheme.fromBusinessTheme(theme);

        assertThat(adapted.nameFontSize())
                .as("display-name size follows business h1 size")
                .isEqualTo(theme.text().h1().size());
        assertThat(adapted.headerFontSize())
                .as("section-header size follows business h2 size")
                .isEqualTo(theme.text().h2().size());
        assertThat(adapted.bodyFontSize())
                .as("body size follows business body size")
                .isEqualTo(theme.text().body().size());
    }

    @Test
    void differentBusinessThemesProduceDifferentCvThemes() {
        CvTheme classic = CvTheme.fromBusinessTheme(BusinessTheme.classic());
        CvTheme modern = CvTheme.fromBusinessTheme(BusinessTheme.modern());
        CvTheme executive = CvTheme.fromBusinessTheme(BusinessTheme.executive());

        // Each business theme picks distinct primary palette colours,
        // so the adapter MUST surface the difference rather than
        // collapsing to a single CV theme.
        assertThat(classic.primaryColor()).isNotEqualTo(modern.primaryColor());
        assertThat(modern.primaryColor()).isNotEqualTo(executive.primaryColor());
        assertThat(classic.primaryColor()).isNotEqualTo(executive.primaryColor());
    }

    @Test
    void cvSpecificDefaultsArePreserved() {
        // Spacing, module margin, and the spacingModuleName token are
        // CV-specific layout tokens that the BusinessTheme has no
        // equivalent for — the bridge keeps the CV defaults so legacy
        // composers see the same numbers they do under
        // CvTheme.defaultTheme().
        CvTheme reference = CvTheme.defaultTheme();
        CvTheme adapted = CvTheme.fromBusinessTheme(BusinessTheme.modern());

        assertThat(adapted.spacing()).isEqualTo(reference.spacing());
        assertThat(adapted.moduleMargin()).isEqualTo(reference.moduleMargin());
        assertThat(adapted.spacingModuleName()).isEqualTo(reference.spacingModuleName());
    }
}
