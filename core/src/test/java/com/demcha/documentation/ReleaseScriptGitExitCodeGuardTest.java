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
 * <p>Every {@code fetch}, {@code add}, {@code commit}, {@code tag}, {@code push} and
 * {@code checkout} therefore goes through {@code Invoke-Git}, which throws on a non-zero
 * exit. This test holds both halves: no bare repository-changing call is left — the six
 * the script makes, and {@code reset}, {@code merge} and {@code rm} should one be added —
 * and the helper still checks the exit code. That the
 * helper actually throws under a held lock is executed by the <em>A failed git
 * mutation stops the cut</em> step in {@code release-script-check.yml}.</p>
 *
 * <p>The helper also runs git with automatic gc and maintenance switched off. A commit
 * or a fetch may start {@code git maintenance run --auto}; on Windows a repack that
 * cannot delete an object directory held open by another worktree's git process asks
 * y/n for each one, and on the 2.5.0 cut that prompt stopped the script after the
 * release commit. That the settings keep a commit from starting automatic maintenance
 * is executed by the same workflow step, read from {@code GIT_TRACE} against a bare
 * commit that starts it.</p>
 *
 * <p>A pathspec separator passed to the helper is written {@code '--'}. PowerShell reads a
 * bare {@code --} in a call to one of its own functions as the end of the function's
 * parameters and drops it, so {@code Invoke-Git checkout -- web/showcase} ran
 * {@code git checkout web/showcase}, which switches to a branch or tag of that name
 * instead of restoring the paths. That the quoted form restores the paths beside such a
 * branch is executed by the <em>A pathspec separator reaches git</em> step of the same
 * workflow.</p>
 */
class ReleaseScriptGitExitCodeGuardTest {

    private static final Path SCRIPT = RepoRoot.get().resolve("scripts/cut-release.ps1");

    /** A statement that starts with a repository-changing git command. */
    private static final Pattern BARE_MUTATION = Pattern.compile(
            "^\\s*(?:&\\s*)?git\\s+(fetch|add|commit|tag|push|reset|checkout|merge|rm)\\b");

    /** A call to the helper anywhere on a line, including inside a one-line {@code if} block. */
    private static final Pattern HELPER_CALL = Pattern.compile("(?<![\\w-])Invoke-Git\\s");

    /** A single- or double-quoted string. */
    private static final Pattern QUOTED = Pattern.compile("'[^']*'|\"[^\"]*\"");

    /** A bare {@code --} token, not part of a longer option such as {@code --quiet}. */
    private static final Pattern BARE_SEPARATOR = Pattern.compile("(?<![\\w-])--(?![\\w-])");

    /** The helper's body, from its declaration to the first column-0 closing brace. */
    private static final Pattern HELPER = Pattern.compile(
            "(?ms)^function Invoke-Git \\{\\r?\\n(.*?)^}");

    /** The helper's git call: its options, then the caller's arguments, ending the line. */
    private static final Pattern GIT_CALL = Pattern.compile("(?m)^\\s*git\\s(.*)@args\\s*$");

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
                .describedAs("sanity: the script must fetch, commit, tag and push through Invoke-Git — "
                        + "no routed call means this guard is reading a script that changed shape")
                .isGreaterThanOrEqualTo(5);
        assertThat(bare)
                .describedAs("a bare git mutation does not stop the script when it fails: PowerShell "
                        + "ignores a native non-zero exit, so the cut reports the step and carries on — "
                        + "on 2.4.0 it tagged the commit before the release. Call Invoke-Git instead")
                .isEmpty();
    }

    @Test
    void aPathspecSeparatorPassedToTheHelperIsQuoted() throws IOException {
        List<String> lines = Files.readAllLines(SCRIPT);
        List<String> bare = new ArrayList<>();
        int quoted = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).strip();
            Matcher call = HELPER_CALL.matcher(line);
            if (line.startsWith("#") || !call.find()) {
                continue;
            }
            String arguments = line.substring(call.end());
            if (arguments.contains("'--'")) {
                quoted++;
            }
            // A -- inside a quoted string is text, not a token PowerShell binds.
            String unquoted = QUOTED.matcher(arguments).replaceAll("''");
            if (BARE_SEPARATOR.matcher(unquoted).find()) {
                bare.add("line " + (i + 1) + ": " + line);
            }
        }

        assertThat(quoted)
                .describedAs("sanity: the post-release showcase restore passes '--' to Invoke-Git — "
                        + "none found means this guard is reading a script that changed shape")
                .isGreaterThanOrEqualTo(1);
        assertThat(bare)
                .describedAs("PowerShell drops a bare -- passed to one of its own functions, so it never "
                        + "reaches git: `Invoke-Git checkout -- web/showcase` runs `git checkout web/showcase`, "
                        + "which switches to a branch or tag of that name instead of restoring the paths. "
                        + "Quote it as '--'")
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
                .containsPattern(GIT_CALL)
                .containsPattern("if \\(\\$LASTEXITCODE -ne 0\\)\\s*\\{\\s*\\r?\\n\\s*throw ");
    }

    @Test
    void theHelperRunsGitWithAutoGcOff() throws IOException {
        Matcher helper = HELPER.matcher(Files.readString(SCRIPT));

        assertThat(helper.find())
                .describedAs("cut-release.ps1 no longer defines Invoke-Git at column 0")
                .isTrue();
        Matcher call = GIT_CALL.matcher(helper.group(1));
        assertThat(call.find())
                .describedAs("Invoke-Git no longer runs git with the caller's arguments")
                .isTrue();
        assertThat(call.group(1))
                .describedAs("Invoke-Git must pass -c gc.auto=0 -c maintenance.auto=false ahead of the "
                        + "caller's arguments: a commit or fetch may otherwise start auto-maintenance, and "
                        + "on Windows its repack asks y/n for every object directory another worktree's "
                        + "git holds open — on the 2.5.0 cut that prompt stopped the script after the commit")
                .contains("-c gc.auto=0")
                .contains("-c maintenance.auto=false");
    }
}
