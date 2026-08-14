package com.demcha.documentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Holds {@code AGENTS.md} to being a set of pointers rather than a second copy of them.
 *
 * <p>The file's whole job is to send a reader to the documents that own a subject and
 * to add only what none of them says. Both halves fail quietly. A link that has moved
 * leaves "read this first" pointing at nothing, and it is read by automation that
 * follows a dead path confidently. And a paragraph copied out of a document it links —
 * the package roots, the module inventory — goes stale the day that document changes,
 * with nothing to notice: existence checks pass either way, which is how prose
 * describing a removed engine stayed green here for a full release line.</p>
 *
 * <p>Neither case is a judgement about the advice. What is checked is that the file
 * still points somewhere real, that the gate it prescribes is a command, and that it has
 * not started restating package coordinates again — the one restatement recognisable on
 * sight, and the one the first draft of this file actually made.</p>
 */
class AgentsGuideGuardTest {

    private static final Path PROJECT_ROOT = RepoRoot.get();

    private static String agentsGuide() throws IOException {
        return Files.readString(PROJECT_ROOT.resolve("AGENTS.md"));
    }

    @Test
    void everyPathItSendsAReaderToExists() throws IOException {
        Matcher links = Pattern.compile("\\[[^\\]]+\\]\\(([^)]+)\\)").matcher(agentsGuide());

        Set<String> missing = new TreeSet<>();
        Set<String> checked = new TreeSet<>();
        while (links.find()) {
            String target = links.group(1);
            if (target.startsWith("http") || target.startsWith("#")) {
                continue;
            }
            String path = target.replaceAll("[#?].*$", "").replaceAll("/$", "");
            checked.add(path);
            if (!Files.exists(PROJECT_ROOT.resolve(path))) {
                missing.add(path);
            }
        }

        assertThat(checked)
                .describedAs("AGENTS.md is a reading list before it is anything else — "
                        + "finding no links suggests the list was dropped, or its formatting "
                        + "changed out from under this guard")
                .isNotEmpty();
        assertThat(missing)
                .describedAs("AGENTS.md links documents that are not there, so it tells an "
                        + "agent to read something it cannot open")
                .isEmpty();
    }

    @Test
    void theBuildCommandsItPrescribesCanBeRun() throws IOException {
        // The file tells an agent what to run before reporting work complete. A wrapper
        // that has moved turns that instruction into a failure the agent will read as a
        // broken repository rather than as a stale document.
        assertThat(agentsGuide())
                .describedAs("AGENTS.md must prescribe the reactor gate, or 'verify before "
                        + "finishing' has no command behind it")
                .contains("./mvnw -B -ntp clean verify");
        assertThat(PROJECT_ROOT.resolve("mvnw"))
                .describedAs("AGENTS.md prescribes ./mvnw, which is not in the repository root")
                .exists();
    }

    @Test
    void itDoesNotRestateThePackageCoordinatesItLinksTo() throws IOException {
        // The failure this guards is the one that made the first draft of this file a
        // second copy of the documentation: restating the package roots and the module
        // inventory, both owned by docs/architecture/package-map.md. Existence checks
        // cannot see that going stale — the packages still exist, they have simply
        // stopped being the ones the guide names. Linking is the only version of this
        // that stays true on its own.
        String guide = agentsGuide();

        Matcher coordinates = Pattern.compile("com\\.demcha\\.compose\\.[a-z0-9.]+").matcher(guide);
        Set<String> restated = new TreeSet<>();
        while (coordinates.find()) {
            restated.add(coordinates.group());
        }

        assertThat(restated)
                .describedAs("AGENTS.md names package coordinates, which docs/architecture/"
                        + "package-map.md owns. A copy here goes stale the day that document "
                        + "moves a package, and nothing fails: link to it instead")
                .isEmpty();
    }
}
