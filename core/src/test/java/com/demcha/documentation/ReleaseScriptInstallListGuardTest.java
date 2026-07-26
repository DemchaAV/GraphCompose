package com.demcha.documentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the hand-maintained module list that {@code cut-release.ps1} installs
 * before it runs {@code ShowcaseSync}.
 *
 * <p>Step 4 of the release script regenerates {@code web/examples.json} by running
 * {@code exec:java} in the examples module. By that point every train pom carries
 * the just-bumped release version, which exists in no repository yet — so each
 * sibling the examples module depends on at that version must have been installed
 * into the local cache first. The script does that from a literal list.</p>
 *
 * <p>A module added to {@code examples/pom.xml} but not to that list fails the cut
 * <em>mid-flight</em>, after the version bump has already rewritten every pom,
 * README and the changelog date, leaving a dirty tree to unwind by hand. That is
 * how {@code graph-compose-render-pptx} — which the examples gained for the PPTX
 * twin examples — aborted a 2.1.0 cut. This test fails in CI instead.</p>
 *
 * <p>Only dependencies pinned to {@code ${graphcompose.version}} are checked. The
 * independently-versioned companions ({@code graph-compose-fonts},
 * {@code graph-compose-emoji}) keep their own release lines and resolve from
 * Central, so the cut never has to build them.</p>
 *
 * <p>The reverse direction is deliberately not asserted: the list legitimately
 * carries modules the examples reach only transitively — {@code render-pdf} comes
 * in through the {@code graph-compose} wrapper, and it too is bumped, so it too
 * must be installed.</p>
 */
class ReleaseScriptInstallListGuardTest {

    private static final Path PROJECT_ROOT = RepoRoot.get();

    /** A {@code graph-compose-*} dependency pinned to the train version. */
    private static final Pattern TRAIN_DEPENDENCY = Pattern.compile(
            "<artifactId>(graph-compose[a-z-]*)</artifactId>\\s*"
                    + "<version>\\$\\{graphcompose\\.version}</version>");

    /** The literal PowerShell array the script installs from. */
    private static final Pattern INSTALL_LIST = Pattern.compile(
            "\\$exampleSnapshotSiblings\\s*=\\s*@\\(([^)]*)\\)", Pattern.DOTALL);

    @Test
    void releaseScriptInstallsEveryTrainSiblingTheExamplesDependOn() throws IOException {
        Set<String> required = examplesTrainSiblings();

        assertThat(required)
                .describedAs("sanity: the examples module must depend on at least one train sibling — "
                        + "an empty set would make this guard pass against anything")
                .isNotEmpty();
        assertThat(scriptInstallList())
                .describedAs("cut-release.ps1 must keep $exampleSnapshotSiblings in lockstep with "
                        + "examples/pom.xml — a train sibling missing here aborts Step 4 of the cut "
                        + "after the version bump has already rewritten the tree")
                .containsAll(required);
    }

    /**
     * Artifact ids of the train-versioned {@code graph-compose-*} siblings the
     * examples depend on, ignoring the {@code <parent>} coordinate.
     */
    private static Set<String> examplesTrainSiblings() throws IOException {
        String pom = Files.readString(PROJECT_ROOT.resolve("examples/pom.xml"))
                .replaceAll("(?s)<parent>.*?</parent>", "");
        Set<String> ids = new LinkedHashSet<>();
        Matcher matcher = TRAIN_DEPENDENCY.matcher(pom);
        while (matcher.find()) {
            ids.add(matcher.group(1));
        }
        return ids;
    }

    /**
     * Artifact ids the script installs, derived from the pom paths in its list
     * ({@code render-pptx/pom.xml} → {@code graph-compose-render-pptx}, and the
     * directory {@code wrapper/} → the bare {@code graph-compose} coordinate).
     */
    private static Set<String> scriptInstallList() throws IOException {
        String script = Files.readString(PROJECT_ROOT.resolve("scripts/cut-release.ps1"));
        Matcher list = INSTALL_LIST.matcher(script);
        assertThat(list.find())
                .describedAs("cut-release.ps1 no longer declares $exampleSnapshotSiblings — this guard "
                        + "is reading a list that moved, so it is no longer guarding anything")
                .isTrue();

        Set<String> ids = new LinkedHashSet<>();
        Matcher path = Pattern.compile("'([^']+)/pom\\.xml'").matcher(list.group(1));
        while (path.find()) {
            String module = path.group(1);
            ids.add(module.equals("wrapper") ? "graph-compose" : "graph-compose-" + module);
        }
        return ids;
    }
}
