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

    /**
     * The override decides what a committed document says, so what it accepts is worth
     * pinning. Driven through {@code resolve} rather than the system property: the answer
     * is cached in a static field, so a test that sets the property proves only that some
     * earlier test had not touched the class yet.
     */
    @Test
    void aDisplayVersionOverridesTheReactorsAndIsNormalised() {
        assertThat(ExampleVersion.resolve("2.1.0", "2.1.1-SNAPSHOT")).isEqualTo("2.1.0");
        assertThat(ExampleVersion.resolve("  2.1.0  ", "2.1.1-SNAPSHOT")).isEqualTo("2.1.0");
        assertThat(ExampleVersion.resolve("v2.1.0", "2.1.1-SNAPSHOT"))
                .describedAs("a leading v is how a version is written in prose, and one render "
                        + "site prepends its own — accepting both spellings here is what keeps "
                        + "vv2.1.0 off the page")
                .isEqualTo("2.1.0");
    }

    @Test
    void withoutAnOverrideTheReactorAnswers() {
        assertThat(ExampleVersion.resolve(null, "2.1.1-SNAPSHOT")).isEqualTo("2.1.1-SNAPSHOT");
        assertThat(ExampleVersion.resolve("   ", "2.1.1-SNAPSHOT"))
                .describedAs("a blank override is an unset one, not an instruction to render "
                        + "nothing")
                .isEqualTo("2.1.1-SNAPSHOT");
        assertThat(ExampleVersion.resolve(null, "@project.version@"))
                .describedAs("an unfiltered resource means the module was run straight from "
                        + "sources")
                .isEqualTo("dev");
        assertThat(ExampleVersion.resolve(null, null)).isEqualTo("dev");
    }
}
