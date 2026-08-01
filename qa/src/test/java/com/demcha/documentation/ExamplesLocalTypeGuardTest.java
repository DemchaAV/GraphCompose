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
 * Keeps helpers that live only in the examples module out of the reader's path.
 *
 * <p>{@code BusinessTheme} and its neighbours are declared in
 * {@code com.demcha.examples.support.theme} and ship in none of the published artifacts.
 * Explaining an example that uses one is honest — a reader who opens the source will find
 * it. Writing {@code BusinessTheme.modern()} in an entry-point table is not: it reads as
 * the way to theme a document, and the reader discovers otherwise only after adding the
 * dependency and failing to import it. That is how the cover-letter row presented an
 * examples-local record as the theming API.</p>
 *
 * <p>What is rejected is <em>using</em> the type: a call, a constructor, a method
 * reference, a member access. The bare name in prose stays legal, because these pages
 * have to be able to name the thing they are describing — except in a table row, where a
 * bare name is a claim about what that row's document is made of, which is the same
 * offer in fewer words and is how the defect was written.</p>
 *
 * <p>Code blocks are scanned like everything else. A fenced snippet is the most
 * copy-pasted thing on the page, so exempting fences wholesale would leave the rule
 * policing only the form nobody copies. The single exemption is a fence introduced by a
 * {@code doc-example-ignore} marker, which already has to carry a written reason and is
 * how a page quotes an example's own source verbatim.</p>
 */
class ExamplesLocalTypeGuardTest {

    private static final Path PROJECT_ROOT = RepoPaths.repoRoot();

    /**
     * The helper package whose types a reader cannot import. Read from the source tree
     * rather than listed: it holds five public types today, and a list would have covered
     * whichever one was noticed first.
     */
    private static final Path EXAMPLES_LOCAL_PACKAGE =
            PROJECT_ROOT.resolve("examples/src/main/java/com/demcha/examples/support/theme");

    /** Pages whose subject is what a past release contained. Mirrors the canonical guard. */
    private static final List<String> HISTORICAL_RECORD_PREFIXES = List.of(
            "CHANGELOG.md", "docs/adr/", "docs/archive/", "docs/migration/",
            "docs/private/", "docs/roadmaps/", "docs/templates/v1-classic/");

    /**
     * The decision guide between the removed template surfaces and the layered ones. It
     * names {@code BusinessTheme.X()} beside its replacement {@code BrandTheme.X()} on
     * purpose — and the type it means is the canonical record 2.0 removed, which happens
     * to share a name with the examples-local helper. Naming the old surface is the
     * document's whole job; the canonical guard allows it here for the same reason.
     */
    private static final Set<String> MIGRATION_INVENTORY =
            Set.of("docs/templates/which-template-system.md");

    /** A marker that exempts the fence below it, with a reason, on a published page. */
    private static final Pattern QUOTED_SOURCE_MARKER =
            Pattern.compile("^<!--\\s*doc-example-ignore:\\s*\\S.*-->\\s*$");

    @Test
    void noPublishedPageOffersATypeThatShipsNowhere() throws IOException {
        List<String> types = examplesLocalTypes();
        Set<String> violations = new TreeSet<>();

        for (Path page : scannedPages()) {
            Scan scan = scan(Files.readAllLines(page, StandardCharsets.UTF_8));
            String rel = relative(page);
            for (String type : types) {
                Pattern used = usage(type);
                Pattern bareName = Pattern.compile("\\b" + Pattern.quote(type) + "\\b");
                for (String line : scan.scanned()) {
                    if (used.matcher(line).find()) {
                        violations.add(rel + " uses " + type);
                    } else if (line.stripLeading().startsWith("|") && bareName.matcher(line).find()) {
                        violations.add(rel + " names " + type + " in a table row");
                    }
                }
            }
        }

        assertThat(violations)
                .describedAs("a type that ships in no artifact — called, constructed, referenced, "
                        + "or offered as what a row's document is made of — reads as the supported "
                        + "way to do it, and the reader finds out otherwise only after adding the "
                        + "dependency. Describe what the example demonstrates, or name the shipping "
                        + "equivalent. To quote an example's own source verbatim, introduce the "
                        + "fence with a doc-example-ignore marker saying so")
                .isEmpty();
    }

    /**
     * Every use of the type: a call, a constructor, a method reference, a member access.
     *
     * <p>Restricting the rule to {@code Type.method(} would leave
     * {@code new BusinessTheme(…)}, {@code BusinessTheme::modern} and
     * {@code BusinessTheme.DEFAULT} as three unguarded ways to make the same offer. A
     * member named {@code java} is the one exception — that is a link to the type's own
     * source file, which is a reader following the trail, not an API being suggested.</p>
     */
    private static Pattern usage(String type) {
        String name = Pattern.quote(type);
        return Pattern.compile("\\b" + name + "\\s*\\.\\s*(?!java\\b)\\w+"
                + "|new\\s+" + name + "\\s*[(<]"
                + "|\\b" + name + "\\s*::");
    }

    /**
     * A fence left open swallows the rest of its page.
     *
     * <p>Checked per file, because the totals cannot see it: one unbalanced marker turns
     * everything below it into an exempt block, and the repository's remaining pages keep
     * every aggregate comfortably in range.</p>
     */
    @Test
    void noPageLeavesAFenceOpen() throws IOException {
        Set<String> unbalanced = new TreeSet<>();
        int scanned = 0;
        for (Path page : scannedPages()) {
            Scan scan = scan(Files.readAllLines(page, StandardCharsets.UTF_8));
            scanned += scan.scanned().size();
            if (scan.fenceLeftOpen()) {
                unbalanced.add(relative(page));
            }
        }

        assertThat(unbalanced)
                .describedAs("an unclosed code fence exempts every line after it, so the page "
                        + "stops being checked from that point on and nothing says so")
                .isEmpty();
        assertThat(scanned)
                .describedAs("almost nothing was scanned — the page set or the fence handling "
                        + "moved and this guard is reading an empty corpus")
                .isGreaterThan(1_000);
    }

    /**
     * The exemption path is exercised. If no page quotes an example's own source any
     * more, the fence handling is dead code and the next reader will not know it was ever
     * needed — and a guard whose only escape hatch is untested tends to acquire a wider
     * one the first time it fires inconveniently.
     */
    @Test
    void quotingAnExamplesOwnSourceStaysPossible() throws IOException {
        int exempt = 0;
        for (Path page : scannedPages()) {
            exempt += scan(Files.readAllLines(page, StandardCharsets.UTF_8)).exempted().size();
        }

        assertThat(exempt)
                .describedAs("no fence is exempted anywhere — either the marker mechanism changed "
                        + "or nothing quotes example source, and the exemption below is untested")
                .isPositive();
    }

    @Test
    void theHelperPackageIsWhereTheGuardThinksItIs() throws IOException {
        assertThat(examplesLocalTypes())
                .describedAs("no types found in %s — the examples-local helpers moved and this "
                        + "guard is checking the documentation against an empty list",
                        relative(EXAMPLES_LOCAL_PACKAGE))
                .isNotEmpty()
                .contains("BusinessTheme");
    }

    /** The published pages this rule applies to. */
    private static List<Path> scannedPages() throws IOException {
        List<Path> pages = new ArrayList<>();
        for (Path page : PublishedDocs.all(PROJECT_ROOT)) {
            String rel = relative(page);
            boolean historical = HISTORICAL_RECORD_PREFIXES.stream().anyMatch(rel::startsWith);
            if (!historical && !MIGRATION_INVENTORY.contains(rel)) {
                pages.add(page);
            }
        }
        return pages;
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

    /** What a page contributes: the lines the rule reads, the lines a marker excused, and balance. */
    private record Scan(List<String> scanned, List<String> exempted, boolean fenceLeftOpen) {
    }

    private static Scan scan(List<String> lines) {
        List<String> scanned = new ArrayList<>();
        List<String> exempted = new ArrayList<>();
        boolean inFence = false;
        boolean fenceIsExempt = false;
        String previous = "";
        for (String line : lines) {
            String trimmed = line.stripLeading();
            if (trimmed.startsWith("```")) {
                if (inFence) {
                    inFence = false;
                    fenceIsExempt = false;
                } else {
                    inFence = true;
                    fenceIsExempt = QUOTED_SOURCE_MARKER.matcher(previous.strip()).matches();
                }
                continue;
            }
            (inFence && fenceIsExempt ? exempted : scanned).add(line);
            if (!trimmed.isEmpty()) {
                previous = line;
            }
        }
        return new Scan(scanned, exempted, inFence);
    }

    private static String relative(Path path) {
        return PROJECT_ROOT.relativize(path).toString().replace('\\', '/');
    }
}
