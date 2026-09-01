package com.demcha.documentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                .contains("function Update-AssetVersion");
    }

    /**
     * The version the previews record moves on a final cut and on nothing else.
     *
     * <p>{@code ExampleVersion} accepts a released {@code X.Y.Z} and rejects everything else, so
     * the property cannot ride along with the generic pom bump: the post-release step carries the
     * train to {@code X.Y.(Z+1)-SNAPSHOT}, which would throw before a single preview was compared,
     * and a pre-release cut carries {@code X.Y.Z-rc.N}, whose qualifier-stripped form names a
     * release that does not exist yet — the previews would advertise it.</p>
     *
     * <p>The three modes are exercised for real in {@code release-script-check.yml}. What this
     * pins is the wiring that makes those outcomes structural rather than incidental: the generic
     * bump does not touch the property, and the step that does sits inside the final-release
     * branch.</p>
     */
    @Test
    void onlyAFinalCutMovesTheVersionThePreviewsRecord() throws IOException {
        String script = Files.readString(SCRIPT);

        int genericBump = script.indexOf("function Update-PomVersion");
        int assetBump = script.indexOf("function Update-AssetVersion");
        assertThat(genericBump).describedAs("Update-PomVersion is gone").isNotNegative();
        assertThat(assetBump).describedAs("Update-AssetVersion is gone").isNotNegative();
        assertThat(script.substring(genericBump, assetBump))
                .describedAs("the generic pom bump touches the asset version again — it runs for "
                        + "the post-release SNAPSHOT and for a pre-release, and both values are "
                        + "ones the examples module refuses")
                .doesNotContain("assetVersion");

        int finalBranch = script.indexOf("if ($isFinalRelease) {", script.indexOf("Step 4 "));
        int call = script.indexOf("Update-AssetVersion (Join-Path");
        assertThat(finalBranch).describedAs("the final-release branch around Step 4 is gone")
                .isNotNegative();
        assertThat(call)
                .describedAs("the asset version is moved outside the final-release branch, so a "
                        + "pre-release cut would move it too")
                .isGreaterThan(finalBranch);
    }

    /**
     * The hero step names a class that exists, and writes the file the cut stages.
     *
     * <p>Both halves are strings the compiler never reads. The class is passed to
     * {@code exec:java} as text, so renaming or moving it leaves the script naming something
     * gone — and the first thing to say so is a cut that has already bumped the version. The
     * path is written by one part of the script and staged by another, so a change to either
     * alone commits whichever hero happened to be in the working tree.</p>
     *
     * <p>Read out of the script rather than compared against a constant here: a copy of the
     * name in this file would be a second thing to keep true, and it would agree with itself
     * while the script named a class nobody has.</p>
     */
    @Test
    void theHeroStepNamesAClassThatExistsAndTheFileTheCutStages() throws IOException {
        String script = Files.readString(SCRIPT);

        int step = script.indexOf("function Render-ReadmeBanner");
        assertThat(step)
                .describedAs("the step that re-renders the README hero at the tagged version is "
                        + "gone: the release would ship the previous version's image")
                .isNotNegative();

        String hero = "'assets/readme/repository_showcase_render.png'";
        String renders = script.substring(step, endOfStep(script, step));
        String stages = script.substring(0, step) + script.substring(endOfStep(script, step));

        assertThat(mainClassesIn(renders))
                .describedAs("the hero step no longer runs a main class")
                .hasSize(1);
        assertThat(renders)
                .describedAs("the hero step writes somewhere other than the file README reads")
                .contains(hero);
        assertThat(stages)
                .describedAs("the hero is re-rendered and staged nowhere else, so the cut would "
                        + "leave the previous release's image in the release commit")
                .contains(hero);

        for (String mainClass : mainClassesIn(script)) {
            assertThat(RepoRoot.get().resolve("examples/src/main/java")
                    .resolve(mainClass.replace('.', '/') + ".java"))
                    .describedAs("cut-release.ps1 runs %s, which no source file declares: the "
                            + "cut fails on it after the version bump", mainClass)
                    .exists();
        }
    }

    /**
     * Where the function starting at {@code step} ends: its own closing brace.
     *
     * <p>Every block inside a function in this script is indented, so a {@code }} in the first
     * column is the function's and nothing else's. Stopping at the next {@code function} instead
     * would not work when the scanned function sits last — the region would swallow the rest of
     * the file, including the staging list this test reads separately.</p>
     */
    private static int endOfStep(String script, int step) {
        int close = script.indexOf("\n}", step);
        return close < 0 ? script.length() : close + 2;
    }

    /** Every class the script hands to {@code exec:java}, in the order it names them. */
    private static List<String> mainClassesIn(String script) {
        Matcher named = Pattern.compile("-Dexec\\.mainClass=([\\w.]+)\"").matcher(script);
        List<String> classes = new ArrayList<>();
        while (named.find()) {
            classes.add(named.group(1));
        }
        return classes;
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
