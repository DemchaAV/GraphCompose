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
 * <p>This holds four properties: the tooling check runs before either mode of
 * {@code cut-release.ps1} mutates, it terminates rather than warns, what it
 * gates and what the tag will re-check are the same tools, and the release
 * runbook describes that contract rather than the one it replaced. They are
 * read structurally — by where things sit relative to each other — rather
 * than by line number, so ordinary edits to the script do not redden it.</p>
 *
 * <p>What a text reading cannot say is whether the {@code throw} is reachable:
 * invert the condition to {@code if (Get-Command node)} and every assertion
 * here still passes. That half is executed instead, by the <em>Missing Node
 * aborts both release paths</em> step in {@code release-script-check.yml},
 * which lifts this same function and runs it with {@code node} reported
 * missing.</p>
 */
class ReleaseKnowledgeGateGuardTest {

    private static final Path PROJECT_ROOT = RepoRoot.get();
    private static final Path SCRIPT = PROJECT_ROOT.resolve("scripts/cut-release.ps1");
    private static final Path RELEASE_WORKFLOW =
            PROJECT_ROOT.resolve(".github/workflows/release.yml");
    private static final Path RUNBOOK =
            PROJECT_ROOT.resolve("docs/contributing/release-process.md");

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
    void theLocalAndTagTimeGatesRunTheSameKnowledgeTools() throws IOException {
        List<String> local = knowledgeCommands(Files.readString(SCRIPT));
        List<String> onTag = knowledgeCommands(Files.readString(RELEASE_WORKFLOW));

        assertThat(local)
                .describedAs("the release script runs no knowledge commands at all")
                .isNotEmpty();
        assertThat(onTag)
                .describedAs("release.yml runs no knowledge commands at all")
                .isNotEmpty();

        // --check variants are a tag-time refinement of the same tool; compare
        // the tools themselves, which is what can silently diverge. The contract
        // is which tools each side runs, not the order it runs them in.
        List<String> localTools = local.stream().map(ReleaseKnowledgeGateGuardTest::tool).distinct().toList();
        List<String> tagTools = onTag.stream().map(ReleaseKnowledgeGateGuardTest::tool).distinct().toList();

        assertThat(tagTools)
                .describedAs("the tag re-runs a knowledge tool the local gate never ran, so "
                        + "a cut can push a tag that its own machine could not have predicted "
                        + "would fail")
                .isSubsetOf(localTools);
        assertThat(localTools)
                .describedAs("the local gate runs a knowledge tool the tag does not re-run, so "
                        + "the independent re-verification after push does not cover it — a "
                        + "tag pushed without the script ships whatever that tool would catch")
                .isSubsetOf(tagTools);
    }

    @Test
    void theBundleIsVerifiedBeforeTheTagAndNotOnlyAfterIt() throws IOException {
        assertThat(knowledgeCommands(Files.readString(SCRIPT)))
                .describedAs("build-bundle --verify used to run only in release.yml, which "
                        + "is after the tag exists and therefore too late to stop a bad one")
                .anySatisfy(command -> assertThat(command).contains("build-bundle.mjs"));
    }

    // --- D. the runbook describes the contract that ships -------------------

    @Test
    void theRunbookDescribesTheGateTheScriptActuallyRuns() throws IOException {
        String runbook = Files.readString(RUNBOOK);
        List<String> localTools = knowledgeCommands(Files.readString(SCRIPT)).stream()
                .map(ReleaseKnowledgeGateGuardTest::tool)
                .distinct()
                .toList();

        assertThat(localTools)
                .describedAs("the release runbook must name every knowledge tool the cut "
                        + "runs, so a tool added to the script cannot stay undocumented — "
                        + "build-bundle joined the local gate and nothing held the runbook "
                        + "to following it")
                .allSatisfy(expected -> assertThat(runbook).contains(expected));

        assertThat(runbook)
                .describedAs("a runbook that does not name the preflight cannot tell a "
                        + "maintainer when a cut will refuse to start")
                .contains(PREFLIGHT);

        assertThat(runbook)
                .describedAs("the preflight throws. A runbook that still says the step is "
                        + "skipped, and tells the maintainer to regenerate and amend "
                        + "afterwards, documents the failure mode this gate removed")
                .doesNotContain("the step is skipped with a loud warning");
    }

    // --- helpers ----------------------------------------------------------

    /**
     * Every {@code node knowledge/tools/...} command a file invokes, one per match.
     *
     * <p>A command's flags end with its line. {@code release.yml} runs its commands on
     * consecutive lines of one {@code run:} block, and a flag class that also matched a
     * line break ran on into the next command and swallowed its prefix, hiding that
     * command from the parity check.</p>
     */
    private static List<String> knowledgeCommands(String content) {
        Matcher matcher = Pattern.compile("node knowledge/tools/[\\w./-]+\\.mjs[\\w -]*")
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
