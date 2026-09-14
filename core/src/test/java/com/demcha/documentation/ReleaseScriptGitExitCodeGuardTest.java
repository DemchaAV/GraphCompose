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
 * Guards that {@code cut-release.ps1} stops when a git call that changes the
 * repository fails.
 *
 * <p>PowerShell does not stop on a native command's non-zero exit. A bare
 * {@code git commit} that loses to a stale {@code .git/index.lock} prints
 * {@code fatal:} and the script carries on. On the 2.4.0 cut both the release
 * {@code add} and {@code commit} failed that way, the script reported the commit, and
 * Step 7 tagged the commit before it. The push was skipped, so nothing left the
 * machine, but without {@code -SkipPush} the wrong commit would have been tagged and
 * published.</p>
 *
 * <p>Every {@code add}, {@code commit}, {@code tag} and {@code push} therefore goes
 * through {@code Invoke-Git}, which throws on a non-zero exit. This test holds both
 * halves: no bare repository-changing call is left — the four the script makes, and
 * {@code reset}, {@code checkout}, {@code merge} and {@code rm} should one be added —
 * and the helper still checks the exit code. That the
 * helper actually throws under a held lock is executed by the <em>A failed git
 * mutation stops the cut</em> step in {@code release-script-check.yml}.</p>
 */
class ReleaseScriptGitExitCodeGuardTest {

    private static final Path SCRIPT = RepoRoot.get().resolve("scripts/cut-release.ps1");

    /** A statement that starts with a repository-changing git command. */
    private static final Pattern BARE_MUTATION = Pattern.compile(
            "^\\s*(?:&\\s*)?git\\s+(add|commit|tag|push|reset|checkout|merge|rm)\\b");

    /** The helper's body, from its declaration to the first column-0 closing brace. */
    private static final Pattern HELPER = Pattern.compile(
            "(?ms)^function Invoke-Git \\{\\r?\\n(.*?)^}");

    @Test
    void everyGitMutationGoesThroughTheCheckedHelper() throws IOException {
        List<String> lines = Files.readAllLines(SCRIPT);
        List<String> bare = new ArrayList<>();
        int routed = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (BARE_MUTATION.matcher(line).find()) {
                bare.add("line " + (i + 1) + ": " + line.strip());
            }
            if (line.strip().startsWith("Invoke-Git ")) {
                routed++;
            }
        }

        assertThat(routed)
                .describedAs("sanity: the script must commit, tag and push through Invoke-Git — "
                        + "no routed call means this guard is reading a script that changed shape")
                .isGreaterThanOrEqualTo(4);
        assertThat(bare)
                .describedAs("a bare git mutation does not stop the script when it fails: PowerShell "
                        + "ignores a native non-zero exit, so the cut reports the step and carries on — "
                        + "on 2.4.0 it tagged the commit before the release. Call Invoke-Git instead")
                .isEmpty();
    }

    @Test
    void theHelperThrowsOnANonZeroExit() throws IOException {
        Matcher helper = HELPER.matcher(Files.readString(SCRIPT));

        assertThat(helper.find())
                .describedAs("cut-release.ps1 no longer defines Invoke-Git at column 0")
                .isTrue();
        assertThat(helper.group(1))
                .describedAs("Invoke-Git must run git and throw when $LASTEXITCODE is non-zero — "
                        + "a helper that only forwards the call restores the silent failure")
                .contains("git @args")
                .containsPattern("if \\(\\$LASTEXITCODE -ne 0\\)\\s*\\{\\s*\\r?\\n\\s*throw ");
    }
}
