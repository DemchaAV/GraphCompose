package com.demcha.documentation;

import com.demcha.compose.qa.RepoPaths;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keeps a helper that lives only in the examples module out of the reader's path.
 *
 * <p>{@code BusinessTheme} is declared in {@code com.demcha.examples.support.theme} and
 * ships in none of the published artifacts. Twenty-one examples use it, and quoting it
 * while explaining those examples is honest — a reader who opens the source will find
 * it. Writing {@code BusinessTheme.modern()} in an entry-point table is not: the call
 * form reads as the way to theme a document, and the reader discovers otherwise only
 * after adding the dependency and failing to import it. That is how the cover-letter
 * row presented an examples-local record as the theming API.</p>
 *
 * <p>So the rule is about the form, not the name: no factory call on a type that ships
 * nowhere, in prose, on any README. Inside a {@code java} fence the same call is fine —
 * the fence quotes an example's own source, and stripping the type from it would make
 * the quotation false.</p>
 */
class ExamplesLocalTypeGuardTest {

    private static final Path PROJECT_ROOT = RepoPaths.repoRoot();

    /**
     * Types a reader cannot import from any published artifact. Shared in spirit with
     * {@code CanonicalSurfaceGuardTest.FORBIDDEN_IN_API_GUIDANCE}, which bans the name
     * outright on the pages that teach the API; here only the call form is banned,
     * because these pages also describe the examples that legitimately use it.
     */
    private static final List<String> EXAMPLES_LOCAL_TYPES = List.of("BusinessTheme");

    @Test
    void noReadmeOffersAFactoryCallOnATypeThatShipsNowhere() throws IOException {
        List<Path> readmes = PublishedDocs.readmes(PROJECT_ROOT);

        assertThat(readmes)
                .describedAs("no README was resolved — the scan moved and this guard covers nothing")
                .isNotEmpty();

        Set<String> violations = new TreeSet<>();
        for (Path readme : readmes) {
            String prose = withoutFencedBlocks(Files.readAllLines(readme, StandardCharsets.UTF_8));
            for (String type : EXAMPLES_LOCAL_TYPES) {
                if (prose.contains(type + ".")) {
                    violations.add(PROJECT_ROOT.relativize(readme).toString().replace('\\', '/')
                            + " offers " + type + ".…()");
                }
            }
        }

        assertThat(violations)
                .describedAs("a factory call on a type that ships in no artifact reads as the "
                        + "supported way to do something; the reader finds out it is not only "
                        + "after adding the dependency. Describe what the example demonstrates, "
                        + "or name the shipping equivalent")
                .isEmpty();
    }

    /**
     * Guard-the-guard: the scan must actually see prose. A fence-stripper that swallowed
     * the whole file would leave every assertion above trivially satisfied.
     */
    @Test
    void theScanReadsProseAndNotOnlyFences() throws IOException {
        int prose = 0;
        for (Path readme : PublishedDocs.readmes(PROJECT_ROOT)) {
            prose += withoutFencedBlocks(Files.readAllLines(readme, StandardCharsets.UTF_8)).length();
        }

        assertThat(prose)
                .describedAs("stripping the fenced blocks left almost nothing to scan — the "
                        + "fence detection is swallowing prose and the guard is passing vacuously")
                .isGreaterThan(20_000);
    }

    /** The document with every fenced block removed, so only prose is searched. */
    private static String withoutFencedBlocks(List<String> lines) {
        StringBuilder prose = new StringBuilder();
        boolean inFence = false;
        for (String line : lines) {
            if (line.trim().startsWith("```")) {
                inFence = !inFence;
                continue;
            }
            if (!inFence) {
                prose.append(line).append('\n');
            }
        }
        return prose.toString();
    }
}
