package com.demcha.examples;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Holds the examples to what they can still claim.
 *
 * <p>{@link CommittedAssetDriftTest} catches a preview falling behind its example. It cannot catch
 * an example that renders faithfully and reads as a document about a release that shipped, or one
 * that links to a page somebody has since deleted — both render perfectly. Those went unnoticed
 * for six releases and were found by reading, so they are checked here rather than read for again.
 * </p>
 */
class ExampleContentGuardTest {

    private static final Path REPO_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final Path PREVIEWS = REPO_ROOT.resolve("assets/readme/examples");
    private static final Path SOURCES = Path.of("src", "main", "java");

    /**
     * A release the project has left behind, named in a rendered page.
     *
     * <p>Matches the {@code v}-prefixed form, which is how prose names a release — "v1.6 Phase A",
     * "Composed with GraphCompose v1.5", "tag v1.9.0". A bare {@code 1.9.0} is deliberately not
     * matched: a Maven coordinate is the subject of the inline-code demo, not a stamp on it.</p>
     */
    private static final Pattern PAST_RELEASE = Pattern.compile("\\bv[01]\\.\\d+(\\.\\d+)?\\b");

    @Test
    void noCommittedPreviewNamesAReleaseTheProjectHasLeftBehind() throws IOException {
        List<String> dated = new ArrayList<>();
        try (var files = Files.list(PREVIEWS)) {
            for (Path pdf : files.filter(p -> p.toString().endsWith(".pdf")).sorted().toList()) {
                Set<String> found = new TreeSet<>();
                try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
                    Matcher match = PAST_RELEASE.matcher(new PDFTextStripper().getText(document));
                    while (match.find()) {
                        found.add(match.group());
                    }
                }
                if (!found.isEmpty()) {
                    dated.add(pdf.getFileName() + " " + found);
                }
            }
        }

        assertThat(dated)
                .describedAs("a committed preview reads as a document about a release the project "
                        + "has moved past. Describe what the example demonstrates instead of the "
                        + "release it shipped in — a version in prose dates the page, and nothing "
                        + "re-reads these once they are committed")
                .isEmpty();
    }

    private static final Pattern REPOSITORY_LINK = Pattern.compile(
            "https://github\\.com/DemchaAV/GraphCompose/blob/[^/\"]+/([^\"\\s)]+)");

    /**
     * Every repository page an example links to is a page that exists.
     *
     * <p>The rich-text example rendered two links into a public preview and both had been deleted:
     * the example that demonstrates hyperlinks shipped broken ones. They point into
     * {@code blob/develop}, which is a branch that moves, so a file renamed a year from now breaks
     * them again — silently, since a PDF is not a page anybody crawls. Resolving the path against
     * this checkout costs nothing and catches the rename in the commit that makes it.</p>
     */
    @Test
    void everyRepositoryLinkAnExampleRendersResolvesToAFileInTheTree() throws IOException {
        List<String> broken = new ArrayList<>();
        try (var sources = Files.walk(SOURCES)) {
            for (Path source : sources.filter(p -> p.toString().endsWith(".java")).sorted().toList()) {
                Matcher link = REPOSITORY_LINK.matcher(Files.readString(source));
                while (link.find()) {
                    if (!Files.exists(REPO_ROOT.resolve(link.group(1)))) {
                        broken.add(source.getFileName() + " -> " + link.group(1));
                    }
                }
            }
        }

        assertThat(broken)
                .describedAs("an example renders a link to a repository path that is not in the "
                        + "tree. A reader clicking it in the committed preview gets a 404, and the "
                        + "example demonstrating links is the worst place to ship one")
                .isEmpty();
    }
}
