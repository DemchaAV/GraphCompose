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
 * The release refuses to start when it cannot verify what it is about to ship.
 *
 * <p>A cut rewrites thirteen poms, the CHANGELOG, the ROADMAP, the README and
 * {@code ShowcaseMetadata}, then commits, tags and pushes. The knowledge checks
 * that would have caught a stale pack used to run after all of that, and
 * {@code release.yml} runs them later still — on the tag, which by then is
 * already on origin. A tag Maven Central has validated cannot be moved, so the
 * only useful place to discover that Node is missing is before the first file
 * changes.</p>
 *
 * <p>This holds three properties of {@code cut-release.ps1}: the tooling check
 * runs before either mode mutates, it terminates rather than warns, and what it
 * gates covers what the tag will re-check. They are read structurally — by
 * where things sit relative to each other — rather than by line number, so
 * ordinary edits to the script do not redden it.</p>
 */
class ReleaseKnowledgeGateGuardTest {

    private static final Path PROJECT_ROOT = RepoRoot.get();
    private static final Path SCRIPT = PROJECT_ROOT.resolve("scripts/cut-release.ps1");
    private static final Path RELEASE_WORKFLOW =
            PROJECT_ROOT.resolve(".github/workflows/release.yml");

    private static final String PREFLIGHT = "Assert-KnowledgeToolingAvailable";

    // --- A. order ---------------------------------------------------------

    @Test
    void theFullCutChecksItsToolingBeforeItRewritesAnything() throws IOException {
        String script = Files.readString(SCRIPT);
        int mode = indexOf(script, "# Mode: full release cut", "the full-cut mode marker");
        int preflight = indexOf(script, PREFLIGHT, "the tooling preflight", mode);
        int firstMutation = indexOf(script, "Update-PomVersion (Join-Path $repoRoot 'core",
                "the first pom rewrite", mode);

        assertThat(preflight)
                .describedAs("a full cut reaches its first pom rewrite before it has "
                        + "checked it can run the knowledge pipeline, so a missing tool "
                        + "is discovered with thirteen poms already moved")
                .isLessThan(firstMutation);
    }

    @Test
    void thePostReleaseBumpChecksItsToolingBeforeItRewritesAnything() throws IOException {
        String script = Files.readString(SCRIPT);
        int mode = indexOf(script, "# Mode: -PostReleaseOnly", "the post-release mode marker");
        int preflight = indexOf(script, PREFLIGHT, "the tooling preflight", mode);
        int firstMutation = indexOf(script, "Update-ShowcaseGhBase", "the first showcase rewrite", mode);

        assertThat(preflight)
                .describedAs("-PostReleaseOnly commits and pushes too, so it needs the "
                        + "same refusal before its first write")
                .isLessThan(firstMutation);
    }

    @Test
    void everyModeThatMutatesAlsoVerifiesBeforeItCommits() throws IOException {
        String script = Files.readString(SCRIPT);
        for (String mode : List.of("# Mode: full release cut", "# Mode: -PostReleaseOnly")) {
            int start = indexOf(script, mode, "a mode marker");
            int verify = indexOf(script, "Update-KnowledgeSurfaces", "the knowledge regen", start);
            int commit = indexOf(script, "git commit -m", "the release commit", start);

            assertThat(verify)
                    .describedAs("%s commits before regenerating the knowledge pack, so the "
                            + "commit ships surfaces naming the previous version", mode)
                    .isLessThan(commit);
        }
    }

    // --- B. fail closed ---------------------------------------------------

    @Test
    void missingToolingTerminatesTheReleaseRatherThanWarningAboutIt() throws IOException {
        String preflight = functionBody(Files.readString(SCRIPT), PREFLIGHT);

        assertThat(preflight)
                .describedAs("the preflight must decide on Node itself")
                .contains("Get-Command node");
        assertThat(preflight)
                .describedAs("a release whose tooling is missing must stop. Warning and "
                        + "returning is what let a cut mutate, commit, tag and push before "
                        + "anything noticed")
                .contains("throw");
        assertThat(preflight.replaceAll("(?m)^\\s*#.*$", ""))
                .describedAs("the preflight must not report the problem by printing it")
                .doesNotContain("Write-Host");
    }

    @Test
    void theKnowledgeStepDoesNotQuietlySucceedWithoutItsTools() throws IOException {
        String update = functionBody(Files.readString(SCRIPT), "Update-KnowledgeSurfaces");

        assertThat(update)
                .describedAs("the step that runs the knowledge tools must assert they are "
                        + "there, so calling it directly cannot fail open either")
                .contains(PREFLIGHT);
    }

    // --- C. parity with what the tag will re-check -------------------------

    @Test
    void theLocalGateCoversEveryKnowledgeCheckTheTagWillRun() throws IOException {
        List<String> local = knowledgeCommands(Files.readString(SCRIPT));
        List<String> onTag = knowledgeCommands(Files.readString(RELEASE_WORKFLOW));

        assertThat(local)
                .describedAs("the release script runs no knowledge commands at all")
                .isNotEmpty();
        assertThat(onTag)
                .describedAs("release.yml runs no knowledge commands at all")
                .isNotEmpty();

        // --check variants are a tag-time refinement of the same tool; compare
        // the tools themselves, which is what can silently diverge.
        List<String> localTools = local.stream().map(ReleaseKnowledgeGateGuardTest::tool).distinct().toList();
        assertThat(onTag.stream().map(ReleaseKnowledgeGateGuardTest::tool).distinct().toList())
                .describedAs("the tag re-runs a knowledge tool the local gate never ran, so "
                        + "a cut can push a tag that its own machine could not have predicted "
                        + "would fail")
                .allSatisfy(onTagTool -> assertThat(localTools).contains(onTagTool));
    }

    @Test
    void theBundleIsVerifiedBeforeTheTagAndNotOnlyAfterIt() throws IOException {
        assertThat(knowledgeCommands(Files.readString(SCRIPT)))
                .describedAs("build-bundle --verify used to run only in release.yml, which "
                        + "is after the tag exists and therefore too late to stop a bad one")
                .anySatisfy(command -> assertThat(command).contains("build-bundle.mjs"));
    }

    // --- helpers ----------------------------------------------------------

    /** Every {@code node knowledge/tools/...} command a file invokes. */
    private static List<String> knowledgeCommands(String content) {
        Matcher matcher = Pattern.compile("node knowledge/tools/[\\w./-]+\\.mjs[\\w\\s-]*")
                .matcher(content);
        List<String> commands = new ArrayList<>();
        while (matcher.find()) {
            commands.add(matcher.group().trim());
        }
        return commands;
    }

    /** The script a knowledge command runs, without its flags. */
    private static String tool(String command) {
        int mjs = command.indexOf(".mjs");
        return command.substring(0, mjs + ".mjs".length());
    }

    private static int indexOf(String content, String needle, String what) {
        return indexOf(content, needle, what, 0);
    }

    private static int indexOf(String content, String needle, String what, int from) {
        int at = content.indexOf(needle, from);
        assertThat(at)
                .describedAs("cut-release.ps1 no longer contains %s (%s) — this guard "
                        + "describes a contract, so rename it here too rather than "
                        + "dropping the check", what, needle)
                .isNotNegative();
        return at;
    }

    private static String functionBody(String script, String name) {
        int start = script.indexOf("function " + name);
        assertThat(start)
                .describedAs("cut-release.ps1 no longer defines %s", name)
                .isNotNegative();
        int end = script.indexOf("\n}", start);
        assertThat(end).describedAs("could not find the end of %s", name).isNotNegative();
        return script.substring(start, end + 2);
    }
}
