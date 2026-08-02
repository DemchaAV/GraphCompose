package com.demcha.documentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keeps the release script's asset step joined to the commit it has to land in.
 *
 * <p>A cut re-renders the previews under {@code assets/readme} at the version it is tagging, and
 * commits them with everything else it bumped. The two halves sit in different parts of the
 * script and neither fails without the other: a refresh that is not staged leaves the release
 * carrying previews from the version before, and a staged path nothing refreshes commits
 * whatever happened to be in the working tree. Both come back as the same symptom — a release
 * publishing figures that do not match the code — which is what the drift gate was written to
 * end, and what this keeps it from being reintroduced beneath.</p>
 *
 * <p>The order matters as much as the presence. The verify step runs the drift gate, so a
 * refresh scheduled after it fails the cut on the previews it was about to fix.</p>
 */
class ReleaseAssetStepGuardTest {

    private static final Path SCRIPT = RepoRoot.get().resolve("scripts/cut-release.ps1");

    @Test
    void theReleaseScriptRefreshesTheCommittedPreviewsAndCommitsThem() throws IOException {
        String script = Files.readString(SCRIPT);

        assertThat(script)
                .describedAs("cut-release.ps1 no longer refreshes the committed previews: a cut "
                        + "would tag a release whose figures are the previous one's")
                .contains("Refresh-CommittedPreviews");
        assertThat(script)
                .describedAs("cut-release.ps1 no longer stages assets/readme/examples, so a refresh "
                        + "would happen and never reach the release commit")
                .contains("'assets/readme/examples'");
        assertThat(script)
                .describedAs("cut-release.ps1 no longer moves the version the previews record; the "
                        + "drift gate would then compare a release's previews at the version before "
                        + "it, and pass")
                .contains("graphcompose.examples.assetVersion");
    }

    @Test
    void thePreviewsAreRefreshedBeforeTheStepThatChecksThem() throws IOException {
        String script = Files.readString(SCRIPT);

        int refresh = script.indexOf("    Refresh-CommittedPreviews");
        int verify = script.indexOf("Run mvnw clean verify");
        assertThat(refresh).describedAs("the refresh call is gone").isNotNegative();
        assertThat(verify).describedAs("the verify step is gone").isNotNegative();

        assertThat(refresh)
                .describedAs("the previews are refreshed after the verify step that compares them, "
                        + "so a cut fails on exactly the files it was about to bring up to date")
                .isLessThan(verify);
    }
}
