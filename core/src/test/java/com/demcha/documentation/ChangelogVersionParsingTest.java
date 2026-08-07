package com.demcha.documentation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives {@link VersionConsistencyGuardTest}'s CHANGELOG check with entries and pom
 * versions the repository's own files do not currently hold.
 *
 * <p>{@link VersionConsistencyGuardTest#theOpenChangelogEntryNamesTheVersionUnderDevelopment}
 * reads the real {@code CHANGELOG.md} and the real pom, which agree — so on their own
 * they exercise the passing branch and nothing else. Neither the mismatch nor the
 * ambiguity the check exists to report is ever reached there, and an entry spelling the
 * parser stops recognising does not turn the check red either: it leaves it with nothing
 * to compare, which reads exactly like success. That is the failure this class exists to
 * make impossible, so each entry shape and each rejection gets a case of its own.</p>
 *
 * <p>The spellings here are not invented. {@code — in progress}, {@code — Unreleased} and
 * {@code - unreleased} have all been used to open a line in this repository's history,
 * and the 2.1.0 line was opened as {@code — in progress} while the poms named 2.0.1 —
 * the drift the guard is for, in the wording that would have hidden it.</p>
 */
class ChangelogVersionParsingTest {

    private static final String POM = "2.1.2-SNAPSHOT";

    // ── Which entries are open ──────────────────────────────────────

    @Test
    void anEntryWithoutADateIsOpen() {
        assertThat(open("""
                # Changelog

                ## v2.1.2 — Planned

                ### Build
                """)).containsExactly("2.1.2");
    }

    @Test
    void aDatedEntryIsNotOpen() {
        assertThat(open("## v2.1.1 — 2026-08-05\n\n### Build\n")).isEmpty();
    }

    @Test
    void anEntrySpelledInProgressIsStillOpen() {
        // The 2.1.0 line was opened this way and ran 31 commits against poms that named
        // 2.0.1. Matching only "Planned" would have seen no open entry and stayed green.
        assertThat(open("## v2.1.0 — in progress\n")).containsExactly("2.1.0");
    }

    @Test
    void anEntrySpelledUnreleasedIsStillOpen() {
        assertThat(open("## v2.1.0 — Unreleased\n")).containsExactly("2.1.0");
        assertThat(open("## v2.1.0 - unreleased\n")).containsExactly("2.1.0");
    }

    @Test
    void anEntryWithNoMarkerAtAllIsOpen() {
        assertThat(open("## v2.1.0\n")).containsExactly("2.1.0");
    }

    @Test
    void aDecoratedMarkerDoesNotHideTheEntry() {
        assertThat(open("## v2.2.0 — Planned (target)\n")).containsExactly("2.2.0");
    }

    @Test
    void carriageReturnsDoNotHideTheEntry() {
        assertThat(open("# Changelog\r\n\r\n## v2.2.0 — Planned\r\n")).containsExactly("2.2.0");
    }

    @Test
    void aHeadingThatNamesNoVersionIsNotAnEntry() {
        assertThat(open("## Unreleased — Planned\n\n### Build\n")).isEmpty();
    }

    // ── What the check accepts ──────────────────────────────────────

    @Test
    void anOpenEntryAgreeingWithThePomIsAccepted() {
        assertThat(VersionConsistencyGuardTest.versionDriftProblem(
                "## v2.1.2 — Planned\n\n## v2.1.1 — 2026-08-05\n", POM))
                .isNull();
    }

    @Test
    void noOpenEntryIsAcceptedBecauseThePostReleaseBumpWritesNone() {
        assertThat(VersionConsistencyGuardTest.versionDriftProblem(
                "## v2.1.1 — 2026-08-05\n\n## v2.1.0 — 2026-07-26\n", POM))
                .isNull();
    }

    @Test
    void aReleaseCandidateAgreesWithTheLineItTargets() {
        assertThat(VersionConsistencyGuardTest.versionDriftProblem(
                "## v2.1.2 — Planned\n", "2.1.2-rc.1"))
                .isNull();
    }

    // ── What the check reports ──────────────────────────────────────

    @Test
    void anOpenEntryNamingAnotherReleaseThanThePomIsReported() {
        assertThat(VersionConsistencyGuardTest.versionDriftProblem(
                "## v2.2.0 — Planned\n\n## v2.1.1 — 2026-08-05\n", POM))
                .contains("2.2.0")
                .contains(POM);
    }

    @Test
    void aSecondOpenEntryIsReported() {
        assertThat(VersionConsistencyGuardTest.versionDriftProblem(
                "## v2.1.2 — Planned\n\n## v2.3.0 — Planned\n\n## v2.1.1 — 2026-08-05\n", POM))
                .contains("ambiguous");
    }

    @Test
    void anOpenEntryTheReleaseScriptCannotDateIsReported() {
        // cut-release.ps1 replaces the literal "## vX.Y.Z — Planned" and notes-and-skips
        // anything else, so a differently worded entry ships undated.
        assertThat(VersionConsistencyGuardTest.versionDriftProblem(
                "## v2.1.2 — in progress\n\n## v2.1.1 — 2026-08-05\n", POM))
                .contains("in progress");
    }

    @Test
    void anUndatedEntryBelowAShippedReleaseIsReported() {
        assertThat(VersionConsistencyGuardTest.versionDriftProblem(
                "## v2.1.1 — 2026-08-05\n\n## v1.9.0 — Planned\n", POM))
                .contains("leftover");
    }

    // ── Release lines ───────────────────────────────────────────────

    @Test
    void aSnapshotAndAReleaseCandidateShareTheirReleaseLine() {
        assertThat(VersionConsistencyGuardTest.releaseLineOf("2.2.0-SNAPSHOT")).isEqualTo("2.2.0");
        assertThat(VersionConsistencyGuardTest.releaseLineOf("2.2.0-rc.1")).isEqualTo("2.2.0");
        assertThat(VersionConsistencyGuardTest.releaseLineOf("2.2.0")).isEqualTo("2.2.0");
    }

    private static List<String> open(String changelog) {
        return VersionConsistencyGuardTest.changelogEntriesIn(changelog).stream()
                .filter(entry -> !entry.isDated())
                .map(VersionConsistencyGuardTest.ChangelogEntry::version)
                .toList();
    }
}
