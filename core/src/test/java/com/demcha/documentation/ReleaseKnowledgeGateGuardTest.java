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
 * Keeps the release script's local knowledge gate fail-closed and aligned with the
 * tag-time workflow.
 *
 * <p>The version bump invalidates the tracked API surfaces. A release that skips their
 * regeneration can still create and push a tag, but {@code release.yml} then rejects that
 * exact tag. The release script must therefore prove its Node tooling is available before
 * either release mode writes a file, and must run every independent bundle check locally.</p>
 */
class ReleaseKnowledgeGateGuardTest {

    private static final Path PROJECT_ROOT = RepoRoot.get();
    private static final Path SCRIPT = PROJECT_ROOT.resolve("scripts/cut-release.ps1");
    private static final Path RELEASE_WORKFLOW = PROJECT_ROOT.resolve(".github/workflows/release.yml");

    @Test
    void bothReleaseModesRequireKnowledgeToolingBeforeTheirFirstMutation() throws IOException {
        String script = Files.readString(SCRIPT);

        int postMode = script.indexOf("# Mode: -PostReleaseOnly");
        int postGate = script.indexOf("Assert-KnowledgeToolingAvailable", postMode);
        int postMutation = script.indexOf("$showcaseChanged = Update-ShowcaseGhBase", postMode);
        assertOrdered("PostReleaseOnly", postMode, postGate, postMutation);

        int fullMode = script.indexOf("# Mode: full release cut");
        int fullGate = script.indexOf("Assert-KnowledgeToolingAvailable", fullMode);
        int fullMutation = script.indexOf("Update-PomVersion (Join-Path", fullMode);
        assertOrdered("full release", fullMode, fullGate, fullMutation);
    }

    @Test
    void missingNodeIsATerminatingPrecondition() throws IOException {
        String preflight = functionBody(Files.readString(SCRIPT),
                "Assert-KnowledgeToolingAvailable");

        assertThat(preflight)
                .describedAs("a version bump must abort when knowledge/ exists but Node.js is "
                        + "unavailable; warning and return lets commit/tag/push continue")
                .contains("if (-not (Get-Command node -ErrorAction SilentlyContinue))")
                .contains("throw \"Node.js is required to regenerate and verify the knowledge pack");
    }

    @Test
    void localKnowledgeGateCoversEveryTagTimeBundleCheck() throws IOException {
        String script = Files.readString(SCRIPT);
        String workflow = Files.readString(RELEASE_WORKFLOW);
        String update = functionBody(script, "Update-KnowledgeSurfaces");

        Matcher commands = Pattern.compile("Run \"(node knowledge/[^\"]+)\"").matcher(update);
        List<String> localCommands = new ArrayList<>();
        while (commands.find()) {
            localCommands.add(commands.group(1));
        }

        assertThat(localCommands)
                .describedAs("the pre-tag gate must regenerate the API surfaces and verify claims, "
                        + "routes, and the self-sufficient release bundle")
                .containsExactly(
                        "node knowledge/tools/api-surface/extract-api.mjs --from-reactor",
                        "node knowledge/tools/claims/check-claims.mjs --check",
                        "node knowledge/tools/routing/check-routes.mjs",
                        "node knowledge/tools/bundle/build-bundle.mjs --verify");
        assertThat(localCommands)
                .allSatisfy(command -> assertThat(workflow)
                        .describedAs("release.yml must re-run the local knowledge gate command: %s",
                                command)
                        .contains(command));
    }

    private static void assertOrdered(String mode, int start, int gate, int mutation) {
        assertThat(start).describedAs("%s mode marker is gone", mode).isNotNegative();
        assertThat(gate)
                .describedAs("%s does not require Node.js before changing the tree", mode)
                .isGreaterThan(start)
                .isLessThan(mutation);
        assertThat(mutation).describedAs("%s first mutation marker is gone", mode).isNotNegative();
    }

    private static String functionBody(String script, String name) {
        int start = script.indexOf("function " + name);
        assertThat(start).describedAs("cut-release.ps1 no longer defines %s", name).isNotNegative();
        int end = script.indexOf("\n}", start);
        assertThat(end).describedAs("could not find the end of %s", name).isNotNegative();
        return script.substring(start, end + 2);
    }
}
