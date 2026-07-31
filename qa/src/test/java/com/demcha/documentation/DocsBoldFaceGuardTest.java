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
 * <p>So published snippets name the <em>family</em>, and the alias is refused outright
 * rather than accepted when paired with a decoration. Requiring the pair would still
 * admit {@code HELVETICA_BOLD} with {@code decoration(ITALIC)} — not bold, whatever the
 * constant says — and would keep publishing a form that reads as the weight set twice.
 * A rule with no exceptions is also a rule with nothing to get subtly wrong.</p>
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
    void publishedSnippetsNameAFontFamilyRatherThanAFaceAlias() throws IOException {
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
                violations.add(relative + " selects FontName." + alias.group(1)
                        + " — name the family and set the decoration");
            }
        }

        assertThat(scannedSites)
                .describedAs("found no fontName(FontName.*) site at all: the scan roots moved and "
                        + "this guard is passing vacuously. Zero *alias* sites is the goal — zero "
                        + "font selections of any kind means the scan is not reading the docs.")
                .isPositive();
        assertThat(violations)
                .describedAs("a *_BOLD / *_ITALIC / *_OBLIQUE constant is an alias of its base "
                        + "family, not a face: FontLibrary resolves it back and the face comes "
                        + "from decoration(...). Pairing the alias with a decoration works but "
                        + "reads as the weight set twice, and pairing it with a different one "
                        + "silently contradicts the name. Published snippets name the family.")
                .isEmpty();
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
