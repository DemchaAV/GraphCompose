package com.demcha.documentation;

import com.demcha.compose.qa.RepoPaths;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keeps a helper that lives only in the examples module out of the reader's path.
 *
 * <p>{@code BusinessTheme} and its neighbours are declared in
 * {@code com.demcha.examples.support.theme} and ship in none of the published artifacts.
 * Eighteen examples use them, and quoting one while explaining those examples is honest —
 * a reader who opens the source will find it. Writing {@code BusinessTheme.modern()} in
 * an entry-point table is not: the call form reads as the way to theme a document, and
 * the reader discovers otherwise only after adding the dependency and failing to import
 * it. That is how the cover-letter row presented an examples-local record as the theming
 * API.</p>
 *
 * <p>Two forms are rejected, for different reasons. A <em>call</em> anywhere in prose
 * offers the type as something to invoke. The bare <em>name</em> in a table row offers it
 * as what that row's document is made of — the same claim in fewer words, and exactly how
 * the defect was written. Everywhere else in prose the bare name is allowed, because
 * these pages have to be able to describe the examples they document.</p>
 *
 * <p>Inside a {@code java} fence both forms are fine: a fence quotes an example's own
 * source, and editing the type out of a quotation would make the quotation false.</p>
 */
class ExamplesLocalTypeGuardTest {

    private static final Path PROJECT_ROOT = RepoPaths.repoRoot();

    /**
     * The helper package whose types a reader cannot import. Read from the source tree
     * rather than listed: it holds five public types today, two of them with static
     * factories, and a list would have covered whichever one was noticed first.
     */
    private static final Path EXAMPLES_LOCAL_PACKAGE =
            PROJECT_ROOT.resolve("examples/src/main/java/com/demcha/examples/support/theme");

    @Test
    void noReadmeOffersATypeThatShipsNowhereAsTheWayToDoSomething() throws IOException {
        List<String> types = examplesLocalTypes();
        Set<String> violations = new TreeSet<>();

        for (Path readme : PublishedDocs.readmes(PROJECT_ROOT)) {
            List<String> prose = prose(Files.readAllLines(readme, StandardCharsets.UTF_8));
            String rel = relative(readme);
            for (String type : types) {
                Pattern call = Pattern.compile("\\b" + Pattern.quote(type) + "\\s*\\.\\s*\\w+\\s*\\(");
                Pattern bareName = Pattern.compile("\\b" + Pattern.quote(type) + "\\b");
                for (String line : prose) {
                    if (call.matcher(line).find()) {
                        violations.add(rel + " calls " + type + " in prose");
                    } else if (line.stripLeading().startsWith("|") && bareName.matcher(line).find()) {
                        violations.add(rel + " names " + type + " in a table row");
                    }
                }
            }
        }

        assertThat(violations)
                .describedAs("a type that ships in no artifact — offered as something to call, or "
                        + "as what a row's document is made of — reads as the supported way to do "
                        + "it, and the reader finds out otherwise only after adding the dependency. "
                        + "Describe what the example demonstrates, or name the shipping equivalent")
                .isEmpty();
    }

    /**
     * Guard-the-guard: prose is what gets scanned, and it is the larger half.
     *
     * <p>Comparing the two halves rather than checking a threshold is deliberate. A fixed
     * floor is satisfied by the fenced content on its own — some 23k characters of it
     * across these READMEs — so a stripper that kept the fences and dropped the prose
     * would clear any floor low enough to be safe, and the rule would then be policing
     * the one place it exists to leave alone.</p>
     */
    @Test
    void theScanReadsProseAndNotFences() throws IOException {
        int prose = 0;
        int fenced = 0;
        for (Path readme : PublishedDocs.readmes(PROJECT_ROOT)) {
            List<String> lines = Files.readAllLines(readme, StandardCharsets.UTF_8);
            prose += length(prose(lines));
            fenced += length(fenced(lines));
        }

        assertThat(fenced)
                .describedAs("no fenced content found across the READMEs — fence detection is not "
                        + "detecting fences, nothing is being exempted, and the comparison below "
                        + "proves nothing")
                .isPositive();
        assertThat(prose)
                .describedAs("the scanned half must be the prose, which outweighs the code on "
                        + "these pages by roughly five to one. A smaller number here means the "
                        + "split is inverted: the rule would be reading the quotations it exists "
                        + "to permit and skipping the sentences it exists to police")
                .isGreaterThan(fenced);
    }

    @Test
    void theHelperPackageIsWhereTheGuardThinksItIs() throws IOException {
        assertThat(examplesLocalTypes())
                .describedAs("no types found in %s — the examples-local helpers moved and this "
                        + "guard is checking the READMEs against an empty list",
                        relative(EXAMPLES_LOCAL_PACKAGE))
                .isNotEmpty()
                .contains("BusinessTheme");
    }

    /** The types declared in the examples module's theme helper package. */
    private static List<String> examplesLocalTypes() throws IOException {
        if (!Files.isDirectory(EXAMPLES_LOCAL_PACKAGE)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(EXAMPLES_LOCAL_PACKAGE)) {
            return files.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".java"))
                    .map(name -> name.substring(0, name.length() - ".java".length()))
                    .filter(name -> !name.equals("package-info"))
                    .sorted()
                    .toList();
        }
    }

    private static List<String> prose(List<String> lines) {
        return partition(lines, false);
    }

    private static List<String> fenced(List<String> lines) {
        return partition(lines, true);
    }

    /** The lines inside fenced blocks, or the lines outside them. Fence markers are neither. */
    private static List<String> partition(List<String> lines, boolean wantFenced) {
        List<String> kept = new ArrayList<>();
        boolean inFence = false;
        for (String line : lines) {
            if (line.stripLeading().startsWith("```")) {
                inFence = !inFence;
                continue;
            }
            if (inFence == wantFenced) {
                kept.add(line);
            }
        }
        return kept;
    }

    private static int length(List<String> lines) {
        return lines.stream().mapToInt(line -> line.length() + 1).sum();
    }

    private static String relative(Path path) {
        return PROJECT_ROOT.relativize(path).toString().replace('\\', '/');
    }
}
