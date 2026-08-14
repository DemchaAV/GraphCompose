package com.demcha.documentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the coupling between {@code cut-release.ps1} and the roadmap's live-release
 * claim.
 *
 * <p>{@link VersionConsistencyGuardTest#roadmapCurrentStableSectionNamesAPublishedVersion}
 * holds {@code ROADMAP.md}'s {@code ## Current stable} section to a published version,
 * and it reads the pom to decide what that is. The release script bumps every pom to the
 * final version in Step 1 and only then runs {@code mvnw clean verify} in Step 5 — so a
 * cut that rewrote the poms without rewriting that section would fail its own gate,
 * mid-flight, with the tree already dirty. A guard that breaks the release it exists to
 * protect is worse than no guard.</p>
 *
 * <p>The README's release-status block has the same shape and the same solution
 * ({@code Update-ReadmeReleaseStatus}); this test asserts the roadmap got it too, rather
 * than leaving the pairing to whoever reads both files next. It checks the wiring — the
 * function exists, Step 1 calls it, the pre-flight verifies the result, and the file
 * reaches the release commit — because the behaviour itself is PowerShell that no JVM
 * test can execute.</p>
 *
 * <p>Deliberately not asserted: that the call sits inside the {@code $isFinalRelease}
 * branch. Matching that structure from outside would pin the script's block layout
 * rather than its behaviour, and the branch is what the function's own comment and the
 * README precedent already document. What a test can catch is the call going missing
 * entirely, which is the failure that costs a cut.</p>
 */
class ReleaseScriptRoadmapGuardTest {

    private static final Path SCRIPT = RepoRoot.get().resolve("scripts/cut-release.ps1");

    @Test
    void theCutRewritesTheRoadmapCurrentStableSection() throws IOException {
        String script = Files.readString(SCRIPT);

        assertThat(script)
                .describedAs("cut-release.ps1 must define Update-RoadmapCurrentStable — "
                        + "without it a final cut leaves ROADMAP naming the previous "
                        + "release and Step 5's verify gate fails on its own bump")
                .contains("function Update-RoadmapCurrentStable");
        assertThat(script)
                .describedAs("Step 1 must call Update-RoadmapCurrentStable on ROADMAP.md, "
                        + "beside the README release-status rewrite it mirrors")
                .contains("Update-RoadmapCurrentStable (Join-Path $repoRoot 'ROADMAP.md') $Version");
    }

    @Test
    void thePreflightVerifiesTheRewriteLanded() throws IOException {
        String script = Files.readString(SCRIPT);

        assertThat(script)
                .describedAs("a Test-RoadmapCurrentStable check must verify the section "
                        + "after Step 1 rewrote it, as Test-ReadmeLatestStable does for "
                        + "the README — a silent no-op rewrite otherwise surfaces only "
                        + "later, as a verify-gate failure with the poms already bumped")
                .contains("function Test-RoadmapCurrentStable")
                .contains("Test-RoadmapCurrentStable $version");
    }

    @Test
    void theReleaseCommitCarriesTheRoadmap() throws IOException {
        String script = Files.readString(SCRIPT);

        int commitFiles = script.indexOf("$commitFiles = @(");
        assertThat(commitFiles)
                .describedAs("cut-release.ps1 must build a $commitFiles list")
                .isNotNegative();
        String list = script.substring(commitFiles, script.indexOf(')', commitFiles));

        assertThat(list)
                .describedAs("ROADMAP.md must be in the release commit: the cut rewrites "
                        + "it, so leaving it out strands the change in a dirty tree and "
                        + "ships a tag whose roadmap names the previous release")
                .contains("'ROADMAP.md'");
    }
}
