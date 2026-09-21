package com.demcha.documentation;

import com.demcha.compose.qa.RepoPaths;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Holds the showcase's published snippets inside the set of pages this module compiles.
 *
 * <p>The site copies a family's worked example out of its guide and publishes it as code a
 * reader can paste, and the only thing standing behind that is
 * {@link DocumentationSnippetCompileTest}: the block is compiled because it lives on a page
 * that guard scans. {@code ShowcaseSiteGuardTest} already holds each published snippet equal
 * to the block it names — but equality to an <em>uncompiled</em> block proves nothing. Point a
 * snippet at a page outside the scanned set and the site would publish it as compiler-checked
 * while no compiler had ever seen it.</p>
 *
 * <p>This lives in {@code qa} rather than beside that equality check because the scanned set is
 * {@link PublishedDocs}, which lives here. Re-deriving it in another module is exactly the drift
 * that class was written to end — two guards each carrying their own idea of which pages are
 * published, disagreeing quietly.</p>
 */
class ShowcaseSnippetScopeTest {

    private static final Path PROJECT_ROOT = RepoPaths.repoRoot();

    private static final Path MANIFEST = PROJECT_ROOT.resolve("web/examples.json");

    /**
     * The families whose guide carries a compiled block today.
     *
     * <p>Named rather than counted: "at least one" would pass while three snippets quietly
     * became one, and a family losing its worked example is the change worth failing on.</p>
     */
    private static final Set<String> FAMILIES_WITH_A_SNIPPET = Set.of("cv", "invoice", "proposal");

    @Test
    void everyPublishedSnippetComesFromAPageThisModuleCompiles() throws IOException {
        JsonNode snippets = manifest().get("snippets");
        assertThat(snippets)
                .describedAs("web/examples.json carries no snippets object, so the site shows a reader "
                        + "no code for any family")
                .isNotNull();

        Set<String> scanned = new TreeSet<>();
        for (Path page : PublishedDocs.all(PROJECT_ROOT)) {
            scanned.add(PROJECT_ROOT.relativize(page).toString().replace('\\', '/'));
        }
        assertThat(scanned)
                .describedAs("PublishedDocs names no pages, so this guard would accept any source at all")
                .isNotEmpty();

        Set<String> wrong = new TreeSet<>();
        Set<String> families = new TreeSet<>();
        snippets.fieldNames().forEachRemaining(families::add);
        for (String family : families) {
            String source = snippets.path(family).path("source").asText();
            if (!scanned.contains(source)) {
                wrong.add(family + " publishes a snippet from " + source
                        + ", which is not a page DocumentationSnippetCompileTest scans");
            }
        }

        assertThat(wrong)
                .describedAs("a snippet is offered to a reader as code that compiles, and it is compiled "
                        + "only because its page is in this module's scan set. One that drifts out of that "
                        + "set keeps the claim and loses the check")
                .isEmpty();
        assertThat(families)
                .describedAs("the families whose guide carries a worked example are named here so that one "
                        + "losing its snippet fails rather than passing as a smaller catalogue")
                .containsExactlyInAnyOrderElementsOf(FAMILIES_WITH_A_SNIPPET);
    }

    @Test
    void everyPublishedSnippetNamesABlockThatIsActuallyMarkedForCompiling() throws IOException {
        JsonNode snippets = manifest().get("snippets");
        Set<String> wrong = new TreeSet<>();

        for (Iterator<String> it = snippets.fieldNames(); it.hasNext(); ) {
            String family = it.next();
            JsonNode snippet = snippets.path(family);
            Path page = PROJECT_ROOT.resolve(snippet.path("source").asText());
            String exampleId = snippet.path("exampleId").asText();
            if (!Files.isRegularFile(page)) {
                wrong.add(family + " names a page that is not there: " + snippet.path("source").asText());
                continue;
            }
            boolean marked = Files.readAllLines(page).stream()
                    .map(String::trim)
                    .anyMatch(line -> line.startsWith("<!--")
                            && line.contains("doc-example:")
                            && List.of(line.split("\\s+")).contains("id=" + exampleId));
            if (!marked) {
                wrong.add(family + " names no doc-example '" + exampleId + "' on "
                        + snippet.path("source").asText());
            }
        }

        assertThat(wrong)
                .describedAs("the marker is what puts a block in front of the compiler; a snippet naming one "
                        + "that has been renamed or removed is published as checked code that nothing checks")
                .isEmpty();
    }

    private static JsonNode manifest() throws IOException {
        return new ObjectMapper().readTree(Files.readString(MANIFEST));
    }
}
