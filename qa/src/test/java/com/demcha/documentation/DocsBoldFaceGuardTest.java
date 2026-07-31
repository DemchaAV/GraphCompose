package com.demcha.documentation;

import com.demcha.compose.qa.RepoPaths;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards published snippets against the font-face trap.
 *
 * <p>{@code FontName.HELVETICA_BOLD} does not select a bold face. {@code FontLibrary}
 * resolves it — and every other {@code *_BOLD} / {@code *_ITALIC} / {@code *_OBLIQUE}
 * constant — back to its base family, and the face within that family comes from
 * {@code DocumentTextStyle.decoration(...)}. A style that names the alias and omits the
 * decoration therefore renders regular, which is what six documented headings did.</p>
 *
 * <p>The failure is silent: the snippet compiles, runs, produces a PDF, and the text is
 * simply the wrong weight. Nothing else in the suite looks at it, which is why the
 * defect survived in the getting-started guide and four recipes.</p>
 *
 * <p>Scans published markdown only. Example sources are excluded deliberately: their
 * rendered output is pinned by layout snapshots and committed previews, so a weight
 * change there is caught by a different gate, and sweeping them would churn every
 * committed render.</p>
 */
class DocsBoldFaceGuardTest {

    private static final Path PROJECT_ROOT = RepoPaths.repoRoot();

    /** Documents a reader copies from. */
    private static final List<String> SCANNED = List.of(
            "README.md",
            "docs",
            "core/README.md",
            "render-pdf/README.md",
            "render-docx/README.md",
            "render-pptx/README.md",
            "templates/README.md",
            "testing/README.md",
            "fonts/README.md",
            "emoji/README.md");

    /**
     * A face alias: any {@code FontName} constant naming a weight or slant rather than
     * a family. Derived from the shape of the name so a new alias is covered on sight.
     */
    private static final Pattern FACE_ALIAS =
            Pattern.compile("fontName\\(\\s*FontName\\.([A-Z_]*(?:BOLD|ITALIC|OBLIQUE)[A-Z_]*)\\s*\\)");

    /** Any font selection at all — proves the scan reached live documents. */
    private static final Pattern ANY_FONT_NAME =
            Pattern.compile("fontName\\(\\s*FontName\\.[A-Z_]+\\s*\\)");

    /** Historical records name the old form on purpose. */
    private static final List<String> EXEMPT_PREFIXES = List.of(
            "docs/adr/", "docs/archive/", "docs/migration/", "docs/private/",
            "docs/roadmaps/", "docs/templates/v1-classic/");

    @Test
    void publishedSnippetsPairAFaceAliasWithADecoration() throws IOException {
        Set<String> violations = new TreeSet<>();
        int scannedSites = 0;

        for (Path doc : scannedDocuments()) {
            String relative = relative(doc);
            if (EXEMPT_PREFIXES.stream().anyMatch(relative::startsWith)) {
                continue;
            }
            String source = Files.readString(doc);
            Matcher anyFont = ANY_FONT_NAME.matcher(source);
            while (anyFont.find()) {
                scannedSites++;
            }
            Matcher alias = FACE_ALIAS.matcher(source);
            while (alias.find()) {
                if (!hasDecorationInSameChain(source, alias.end())) {
                    violations.add(relative + " uses FontName." + alias.group(1)
                            + " with no decoration(...) — renders regular");
                }
            }
        }

        assertThat(scannedSites)
                .describedAs("found no fontName(FontName.*) site at all: the scan roots moved and "
                        + "this guard is passing vacuously. Zero *alias* sites is the goal — zero "
                        + "font selections of any kind means the scan is not reading the docs.")
                .isPositive();
        assertThat(violations)
                .describedAs("the font name selects the family and the decoration selects the face "
                        + "within it, so a *_BOLD constant without decoration(...) renders regular. "
                        + "Name the family and set the decoration.")
                .isEmpty();
    }

    /**
     * Whether a {@code decoration(...)} call belongs to the same builder chain as the
     * {@code fontName(...)} at {@code from}. The chain ends at its {@code build()}; a
     * decoration set after that belongs to a different style.
     */
    private static boolean hasDecorationInSameChain(String source, int from) {
        int chainEnd = source.indexOf(".build()", from);
        int windowEnd = chainEnd < 0 ? source.length() : chainEnd;
        String chainTail = source.substring(from, windowEnd);
        if (chainTail.contains("decoration(")) {
            return true;
        }
        // The decoration may precede the font name in the same chain, so look back to
        // where this builder started.
        int chainStart = source.lastIndexOf("DocumentTextStyle.builder()", from);
        return chainStart >= 0 && source.substring(chainStart, from).contains("decoration(");
    }

    private static List<Path> scannedDocuments() throws IOException {
        List<Path> documents = new ArrayList<>();
        for (String entry : SCANNED) {
            Path root = PROJECT_ROOT.resolve(entry);
            if (Files.isRegularFile(root)) {
                documents.add(root);
            } else if (Files.isDirectory(root)) {
                try (Stream<Path> walk = Files.walk(root)) {
                    walk.filter(Files::isRegularFile)
                            .filter(path -> path.toString().endsWith(".md"))
                            .sorted()
                            .forEach(documents::add);
                }
            }
        }
        return documents;
    }

    private static String relative(Path path) {
        return PROJECT_ROOT.relativize(path).toString().replace('\\', '/');
    }
}
