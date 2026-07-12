package com.demcha.compose.engine.components.content.header_footer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Engine semantics of page numbering on {@link HeaderFooterConfig}: the instance
 * {@code resolveTokens} offset/restart/style math, the {@code appliesTo}
 * predicate, and that the static {@code resolvePlaceholders} shim stays
 * byte-identical for the legacy decimal path.
 */
class HeaderFooterConfigNumberingTest {

    @Test
    void defaultNumberingMatchesTheLegacyStaticOutput() {
        HeaderFooterConfig config = HeaderFooterConfig.builder().build();
        // counted == physical, counted total == physical total → identical to the static shim.
        assertThat(config.resolveTokens("Page {page} of {pages}", 2, 5))
                .isEqualTo(HeaderFooterConfig.resolvePlaceholders("Page {page} of {pages}", 2, 5))
                .isEqualTo("Page 2 of 5");
    }

    @Test
    void styleAndOffsetApplyToCountedValue() {
        HeaderFooterConfig config = HeaderFooterConfig.builder()
                .numberStyle(PageNumberStyle.UPPER_ROMAN)
                .build();
        assertThat(config.resolveTokens("{page}", 3, 6)).isEqualTo("III");
    }

    @Test
    void countFromRestartsAndPagesReportsCountedTotal() {
        // Front matter: first two physical pages are uncounted; body starts at 1.
        HeaderFooterConfig config = HeaderFooterConfig.builder()
                .numberCountFrom(3)
                .numberStartAt(1)
                .build();
        // physical page 3 -> counted 1; physical page 4 -> counted 2.
        assertThat(config.resolveTokens("{page}", 3, 6)).isEqualTo("1");
        assertThat(config.resolveTokens("{page}", 4, 6)).isEqualTo("2");
        // {pages} on a 6-page doc = counted total = 1 + (6 - 3) = 4 (not physical 6).
        assertThat(config.resolveTokens("{page} of {pages}", 4, 6)).isEqualTo("2 of 4");
    }

    @Test
    void appliesToHonoursCountFromAndShowOnFirstPage() {
        HeaderFooterConfig suppressed = HeaderFooterConfig.builder()
                .numberShowOnFirstPage(false)
                .build();
        assertThat(suppressed.appliesTo(1)).isFalse();
        assertThat(suppressed.appliesTo(2)).isTrue();

        HeaderFooterConfig offset = HeaderFooterConfig.builder()
                .numberCountFrom(3)
                .build();
        assertThat(offset.appliesTo(2)).isFalse();
        assertThat(offset.appliesTo(3)).isTrue();

        // Default config renders on every page.
        assertThat(HeaderFooterConfig.builder().build().appliesTo(1)).isTrue();
    }

    @Test
    void staticShimIsUnchanged() {
        assertThat(HeaderFooterConfig.resolvePlaceholders("p{page}/{pages}", 4, 9)).isEqualTo("p4/9");
        assertThat(HeaderFooterConfig.resolvePlaceholders(null, 1, 1)).isNull();
        assertThat(HeaderFooterConfig.resolvePlaceholders("", 1, 1)).isEmpty();
    }
}
