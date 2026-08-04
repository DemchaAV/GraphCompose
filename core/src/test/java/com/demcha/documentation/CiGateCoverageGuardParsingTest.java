package com.demcha.documentation;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives {@link CiGateCoverageGuardTest}'s job parser with shapes the repository's
 * own workflow does not currently contain.
 *
 * <p>{@link CiGateCoverageGuardTest} reads {@code .github/workflows/ci.yml}, so it
 * can only ever exercise the job spellings that file happens to use — today, lower
 * case with hyphens and nothing after the colon. Every other spelling GitHub
 * accepts is untested there, and the failure mode is not a red build: a job the
 * parser does not match is a job the guard cannot report missing from
 * {@code ci-gate.needs}. It stays absent, the test stays green, and the aggregate
 * check branch protection requires is blind to it.</p>
 *
 * <p>So each spelling gets a workflow of its own here, and each asserts the parser
 * saw the job at all. These are the cases that would otherwise be discovered by a
 * new job silently escaping the gate.</p>
 */
class CiGateCoverageGuardParsingTest {

    @Test
    void aJobNameWithAnUnderscoreOrCapitalsIsSeen() {
        Map<String, String> jobs = CiGateCoverageGuardTest.jobBlocks("""
                on: [push]

                jobs:
                  build_and_test:
                    runs-on: ubuntu-latest
                  CodeQL:
                    runs-on: ubuntu-latest
                """);

        assertThat(jobs).containsKeys("build_and_test", "CodeQL");
    }

    @Test
    void aTrailingCommentDoesNotHideTheJob() {
        Map<String, String> jobs = CiGateCoverageGuardTest.jobBlocks("""
                on: [push]

                jobs:
                  security_scan: # runs the scanners
                    runs-on: ubuntu-latest
                  perf-smoke:
                    runs-on: ubuntu-latest
                """);

        assertThat(jobs)
                .describedAs("a job whose declaration carries an inline comment must still be "
                        + "parsed — otherwise it can sit outside the gate's needs list with "
                        + "nothing reporting it")
                .containsKeys("security_scan", "perf-smoke");
    }

    @Test
    void trailingWhitespaceDoesNotHideTheJob() {
        Map<String, String> jobs = CiGateCoverageGuardTest.jobBlocks(
                "on: [push]\n\njobs:\n  build-and-test:   \n    runs-on: ubuntu-latest\n");

        assertThat(jobs).containsKey("build-and-test");
    }

    @Test
    void keysNestedInsideAJobAreNotMistakenForJobs() {
        Map<String, String> jobs = CiGateCoverageGuardTest.jobBlocks("""
                on: [push]

                jobs:
                  build-and-test:
                    runs-on: ubuntu-latest
                    steps:
                      - name: Compile
                        run: ./mvnw -B verify
                """);

        assertThat(jobs)
                .describedAs("only two-space keys are jobs; deeper keys belong to the job above")
                .containsOnlyKeys("build-and-test");
    }

    @Test
    void theBlockOfAJobStopsAtTheNextJob() {
        Map<String, String> jobs = CiGateCoverageGuardTest.jobBlocks("""
                on: [push]

                jobs:
                  first:
                    runs-on: ubuntu-latest
                    if: github.event_name == 'schedule'
                  second:
                    runs-on: ubuntu-latest
                """);

        assertThat(jobs.get("second"))
                .describedAs("a job must not inherit the previous job's condition, or the guard "
                        + "excuses it from the gate for a reason that belongs to its neighbour")
                .doesNotContain("schedule");
    }
}
