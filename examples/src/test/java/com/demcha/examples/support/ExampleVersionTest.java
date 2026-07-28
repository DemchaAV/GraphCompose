package com.demcha.examples.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the version string the published example documents render.
 *
 * <p>Example PDFs are regenerated whenever their content changes, not only at a
 * release, so between cuts the reactor version carries {@code -SNAPSHOT}. That
 * suffix reached the committed decks once already, which meant the README-linked
 * asset advertised a coordinate nobody could resolve. The strip lived privately
 * in one deck and the other never inherited it; it is shared now, and this is
 * what keeps a future caller from reaching for {@code current()} by habit.</p>
 */
class ExampleVersionTest {

    @Test
    void theRenderedVersionCarriesNoPreReleaseQualifier() {
        assertThat(ExampleVersion.withoutQualifier())
                .describedAs("published examples must not render a -SNAPSHOT or -rc coordinate")
                .doesNotContain("-");
    }

    @Test
    void itKeepsTheReleaseDigitsIntact() {
        assertThat(ExampleVersion.withoutQualifier())
                .describedAs("stripping the qualifier must not eat the version itself")
                .isEqualTo(ExampleVersion.current().replaceFirst("-.*$", ""));
    }

    @Test
    void theLineIsTheLeadingMajorMinor() {
        assertThat(ExampleVersion.majorMinor("2.1.1-SNAPSHOT")).isEqualTo("2.1");
        assertThat(ExampleVersion.majorMinor("dev")).isEqualTo("dev");
    }
}
