package com.demcha.examples.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keeps the honest fallback from quietly becoming the normal case.
 *
 * <p>{@link PerfBaseline} degrades to "not measured" when its resource is
 * absent, which is the right behaviour at render time and the wrong thing to
 * discover in a published PDF. The baseline reaches the classpath through a
 * resource directory in {@code examples/pom.xml} that points outside the
 * module; rename the file, move the directory, or drop that pom entry and every
 * figure in the master showcase silently turns into a placeholder while the
 * build stays green. This is the test that would go red instead.</p>
 */
class PerfBaselineTest {

    @Test
    void theCommittedBaselineIsOnTheClasspath() {
        assertThat(PerfBaseline.get().capturedOn())
                .describedAs("baseline resource missing from the examples classpath - "
                             + "check the baselines resource entry in examples/pom.xml")
                .matches("\\d{4}-\\d{2}-\\d{2}");
    }

    @Test
    void itCarriesTheScenariosTheShowcaseQuotes() {
        for (String scenario : new String[] {"invoice-template", "feature-rich"}) {
            assertThat(PerfBaseline.get().scenario(scenario))
                    .describedAs("MasterShowcaseExample renders '%s', so the baseline must carry it",
                            scenario)
                    .hasValueSatisfying(measured -> {
                        assertThat(measured.avgMillis()).isPositive();
                        assertThat(measured.docsPerSecond()).isPositive();
                    });
        }
    }

    @Test
    void anUnknownScenarioIsAbsentRatherThanZero() {
        assertThat(PerfBaseline.get().scenario("no-such-scenario"))
                .describedAs("an unknown scenario must be empty so the caller can say "
                             + "'not measured' instead of rendering 0.0 ms")
                .isEmpty();
    }
}
