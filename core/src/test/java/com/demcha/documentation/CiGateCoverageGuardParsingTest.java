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
    void aJobWrittenAsAnInlineMappingIsSeen() {
        Map<String, String> jobs = CiGateCoverageGuardTest.jobBlocks("""
                on: [push]

                jobs:
                  deploy: {runs-on: ubuntu-latest}
                  build-and-test:
                    runs-on: ubuntu-latest
                """);

        assertThat(jobs)
                .describedAs("GitHub documents the value of jobs.<job_id> as a map, and an inline "
                        + "mapping is one — a job spelled this way must not be invisible to the "
                        + "guard, or it can sit outside the gate's needs with the build green")
                .containsKeys("deploy", "build-and-test");
    }

    @Test
    void quotesAroundAKeyAreNotPartOfTheJobId() {
        Map<String, String> jobs = CiGateCoverageGuardTest.jobBlocks("""
                on: [push]

                jobs:
                  "build-and-test":
                    runs-on: ubuntu-latest
                  'perf_smoke':
                    runs-on: ubuntu-latest
                """);

        assertThat(jobs)
                .describedAs("the quotes are YAML's encoding of the key, not the name — kept, the "
                        + "id could never match the bare name in the gate's needs list and a legal "
                        + "workflow would be reported as having an unwatched job")
                .containsOnlyKeys("build-and-test", "perf_smoke");
    }

    /**
     * A key GitHub would reject is still parsed, so the guard can report it.
     *
     * <p>{@code "security scan"} is not a legal job id — GitHub requires a leading
     * letter or {@code _} and then only letters, digits, {@code -} or {@code _}. The
     * parser does not filter on that grammar, because filtering is how a key becomes
     * invisible; it hands the key over and
     * {@code CiGateCoverageGuardTest#everyJobIdIsOneGitHubWouldAccept} fails on it.</p>
     */
    @Test
    void aKeyThatIsNotALegalJobIdIsStillParsedRatherThanSkipped() {
        Map<String, String> jobs = CiGateCoverageGuardTest.jobBlocks("""
                on: [push]

                jobs:
                  "security scan":
                    runs-on: ubuntu-latest
                """);

        assertThat(jobs)
                .describedAs("an unparseable job-level key must reach the assertions rather than "
                        + "vanish — a guard that filters its input reports on a subset and calls "
                        + "it coverage")
                .containsOnlyKeys("security scan");
    }

    @Test
    void unquotingLeavesAnUnmatchedOrEmbeddedQuoteAlone() {
        assertThat(CiGateCoverageGuardTest.unquoted("\"build")).isEqualTo("\"build");
        assertThat(CiGateCoverageGuardTest.unquoted("bui\"ld")).isEqualTo("bui\"ld");
        assertThat(CiGateCoverageGuardTest.unquoted("\"")).isEqualTo("\"");
        assertThat(CiGateCoverageGuardTest.unquoted("'perf'")).isEqualTo("perf");
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
