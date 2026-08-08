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
 * ambiguity the check exists to report is ever reached there, and an entry shape the
 * parser stops recognising does not turn the check red either: it leaves it with nothing
 * to compare, which reads exactly like success. That is the failure this class exists to
 * make impossible, so each entry shape and each rejection gets a case of its own.</p>
 *
 * <p>The shapes here are not invented. {@code — in progress}, {@code — Unreleased} and
 * {@code - unreleased} have all opened a line in this repository's history; the 2.1.0
 * line was opened as {@code — in progress} while the poms named 2.0.1. Pre-release
 * headings are equally real — twenty-nine {@code ## v1.5.0-beta.N} entries were written
 * during the 1.5.0 cycle.</p>
 */
class ChangelogVersionParsingTest {

    private static final String POM = "2.1.2-SNAPSHOT";
    private static final String SHIPPED = "\n\n## v2.1.1 — 2026-08-05\n";

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

    // ── Pre-release entries keep their qualifier ────────────────────

    @Test
    void aDatedPreReleaseEntryIsShippedRatherThanOpen() {
        // Truncating the version at the hyphen would read this as version 2.2.0 with a
        // line starting "rc.1", hence undated, hence a second open entry holding the
        // build red against a changelog that is perfectly consistent.
        assertThat(open("## v2.2.0-rc.1 — 2026-09-01\n" + SHIPPED)).isEmpty();
        assertThat(open("## v1.5.0-beta.3 — 2026-02-01\n")).isEmpty();
    }

    @Test
    void anOpenPreReleaseEntryKeepsItsQualifier() {
        assertThat(open("## v2.2.0-rc.1 — Planned\n")).containsExactly("2.2.0-rc.1");
    }

    @Test
    void aPreReleaseEntryAgreesWithThePomCuttingIt() {
        assertThat(problem("## v2.2.0-rc.1 — Planned\n" + SHIPPED, "2.2.0-rc.1")).isNull();
    }

    @Test
    void aDateFurtherAlongTheLineDoesNotMakeAnEntryShipped() {
        assertThat(open("## v2.1.2 — Planned, superseding 2026-01-01\n")).containsExactly("2.1.2");
    }

    // ── What the check accepts ──────────────────────────────────────

    @Test
    void anOpenEntryAgreeingWithThePomIsAccepted() {
        assertThat(problem("## v2.1.2 — Planned" + SHIPPED, POM)).isNull();
    }

    @Test
    void noOpenEntryIsAcceptedBecauseThePostReleaseBumpWritesNone() {
        assertThat(problem("## v2.1.1 — 2026-08-05\n\n## v2.1.0 — 2026-07-26\n", POM)).isNull();
    }

    @Test
    void aReleaseCandidateAgreesWithTheLineItTargets() {
        assertThat(problem("## v2.1.2 — Planned" + SHIPPED, "2.1.2-rc.1")).isNull();
    }

    @Test
    void aDecoratedPlannedMarkerIsAcceptedBecauseTheCutStillDatesIt() {
        // Step 2 replaces the matched "## v2.1.2 — Planned" and leaves the tail in place,
        // so this entry does get dated — rejecting it would be a false alarm.
        assertThat(problem("## v2.1.2 — Planned (target)" + SHIPPED, POM)).isNull();
    }

    // ── What the check reports ──────────────────────────────────────

    @Test
    void anOpenEntryNamingAnotherReleaseThanThePomIsReported() {
        assertThat(problem("## v2.2.0 — Planned" + SHIPPED, POM))
                .contains("2.2.0")
                .contains(POM);
    }

    @Test
    void aSecondOpenEntryIsReported() {
        assertThat(problem("## v2.1.2 — Planned\n\n## v2.3.0 — Planned" + SHIPPED, POM))
                .contains("ambiguous");
    }

    @Test
    void anAsciiHyphenIsNotTheSeparatorTheCutMatches() {
        // The likeliest thing to type, and the one the release script cannot see: it
        // replaces the literal em-dash form.
        assertThat(problem("## v2.1.2 - Planned" + SHIPPED, POM))
                .contains("Planned");
    }

    @Test
    void anOpenEntryTheCutCannotDateIsReported() {
        assertThat(problem("## v2.1.2 — in progress" + SHIPPED, POM))
                .contains("in progress");
    }

    @Test
    void theDriftIsReportedAheadOfTheWording() {
        // Both are wrong here. The release the two sources disagree about is the finding;
        // being told only about the marker would bury it.
        assertThat(problem("## v2.2.0 - in progress" + SHIPPED, POM))
                .contains("2.2.0")
                .contains(POM);
    }

    @Test
    void anUndatedEntryBelowAShippedReleaseIsReported() {
        assertThat(problem("## v2.1.1 — 2026-08-05\n\n## v1.9.0 — Planned\n", POM))
                .contains("leftover");
    }

    // ── Headings the check cannot read are reported, not skipped ────

    @Test
    void aTopmostHeadingNamingNoReleaseIsReported() {
        assertThat(problem("## Unreleased" + SHIPPED, POM)).contains("names no release");
    }

    @Test
    void aTwoComponentVersionIsReported() {
        assertThat(problem("## v2.2 — Planned" + SHIPPED, POM)).contains("names no release");
    }

    // ── Release lines ───────────────────────────────────────────────

    @Test
    void aSnapshotAndAReleaseCandidateShareTheirReleaseLine() {
        assertThat(VersionConsistencyGuardTest.releaseLineOf("2.2.0-SNAPSHOT")).isEqualTo("2.2.0");
        assertThat(VersionConsistencyGuardTest.releaseLineOf("2.2.0-rc.1")).isEqualTo("2.2.0");
        assertThat(VersionConsistencyGuardTest.releaseLineOf("2.2.0")).isEqualTo("2.2.0");
    }

    private static String problem(String changelog, String pomVersion) {
        return VersionConsistencyGuardTest.versionDriftProblem(changelog, pomVersion);
    }

    private static List<String> open(String changelog) {
        return VersionConsistencyGuardTest.changelogEntriesIn(changelog).stream()
                .filter(entry -> !entry.isDated())
                .map(VersionConsistencyGuardTest.ChangelogEntry::version)
                .toList();
    }
}
