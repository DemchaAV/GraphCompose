package com.demcha.compose.document.templates.support.common;

import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateLayoutPolicyTest {

    @Test
    void shouldUseThemeRhythmForStandardCv() {
        CvTheme theme = CvTheme.defaultTheme();

        TemplateLayoutPolicy policy = TemplateLayoutPolicy.standardCv(theme);

        assertThat(policy.rootSpacing()).isEqualTo(theme.spacingModuleName());
        assertThat(policy.sectionMargin()).isEqualTo(Margin.of(5));
        assertThat(policy.bodyLineSpacing()).isEqualTo(theme.spacing());
        assertThat(policy.bodyItemSpacing()).isEqualTo(theme.spacing());
        assertThat(policy.markerlessContinuationIndent()).isEqualTo("  ");
        assertThat(policy.bodyPadding()).isEqualTo(new Padding(0, 5, 0, 20));
    }

    @Test
    void shouldCentralizeBusinessDocumentRhythm() {
        TemplateLayoutPolicy policy = TemplateLayoutPolicy.businessDocument();

        assertThat(policy.rootSpacing()).isEqualTo(10.0);
        assertThat(policy.sectionMargin()).isEqualTo(Margin.top(6));
        assertThat(policy.subsectionMargin()).isEqualTo(Margin.top(4));
        assertThat(policy.blockMargin()).isEqualTo(Margin.top(3));
        assertThat(policy.bodyLineSpacing()).isEqualTo(2.0);
        assertThat(policy.tableLineSpacing()).isEqualTo(1.2);
        assertThat(policy.compactCellPadding()).isEqualTo(new Padding(2, 0, 2, 0));
        assertThat(policy.contentCellPadding()).isEqualTo(new Padding(7, 8, 7, 8));
    }

    @Test
    void shouldNormalizeNullableMarginAndPaddingTokens() {
        TemplateLayoutPolicy policy = new TemplateLayoutPolicy(
                0,
                null,
                null,
                null,
                0,
                0,
                0,
                null,
                null,
                null,
                null);

        assertThat(policy.sectionMargin()).isEqualTo(Margin.zero());
        assertThat(policy.subsectionMargin()).isEqualTo(Margin.zero());
        assertThat(policy.blockMargin()).isEqualTo(Margin.zero());
        assertThat(policy.markerlessContinuationIndent()).isEmpty();
        assertThat(policy.bodyPadding()).isEqualTo(Padding.zero());
        assertThat(policy.compactCellPadding()).isEqualTo(Padding.zero());
        assertThat(policy.contentCellPadding()).isEqualTo(Padding.zero());
    }

    @Test
    void shouldRejectInvalidSpacingValues() {
        assertThatThrownBy(() -> new TemplateLayoutPolicy(
                Double.NaN,
                Margin.zero(),
                Margin.zero(),
                Margin.zero(),
                0,
                0,
                0,
                "",
                Padding.zero(),
                Padding.zero(),
                Padding.zero()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rootSpacing");

        assertThatThrownBy(() -> TemplateLayoutPolicy.businessDocument().top(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("value");
    }
}
