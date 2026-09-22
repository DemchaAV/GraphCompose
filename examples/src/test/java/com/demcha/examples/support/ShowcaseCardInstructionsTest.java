package com.demcha.examples.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Holds what a card tells a reader to install and run against the example that card links to.
 *
 * <p>Both claims fail the same way if they are wrong: the reader's project compiles, and then
 * something throws. A document that reaches a second backend needs that backend's artifact —
 * the DOCX one is named in an import, the PPTX one is discovered by format, so the call is the
 * only sign the source gives — and the render fails with {@code MissingBackendException} when
 * it is missing. An example class without a {@code main} is driven by
 * {@code GenerateAllExamples}: an {@code exec:java} command for it fails with "doesn't contain
 * a main method".</p>
 *
 * <p>Read from the example's own source, which is the thing the site links to. The honest claim
 * is narrow, though: the detection here is the same rule {@code ShowcaseSync} applies, so what
 * this catches is a manifest that no longer matches the sources — a sync never re-run, a card
 * edited by hand, an example that grew a backend after its last publish — and not a flaw in the
 * rule itself. A source that reaches a backend in a shape neither copy reads (a static import,
 * a fully-qualified name, a sibling class doing the export) would satisfy both. The consumer
 * projects under {@code scripts/release-smoke} are what test the conclusion, by building from
 * the coordinates the site publishes.</p>
 */
class ShowcaseCardInstructionsTest {

    /** The module directory is the working directory; the repository is its parent. */
    private static final Path REPO_ROOT = Path.of("..").toAbsolutePath().normalize();

    private static final Path MANIFEST = REPO_ROOT.resolve("web/examples.json");

    private static final Pattern MAIN_METHOD = Pattern.compile("static\\s+void\\s+main\\s*\\(");

    private static final String DOCX_IMPORT = "import com.demcha.compose.document.backend.semantic.docx.";

    private static final List<String> PPTX_CALLS = List.of(".toPptxBytes(", ".buildPptx(", ".writePptx(");

    @Test
    void everyCardAsksForTheBackendsItsExampleReaches() throws IOException {
        Set<String> wrong = new TreeSet<>();
        int docx = 0;
        int pptx = 0;

        for (JsonNode card : cards()) {
            String id = card.path("id").asText();
            String source = card.path("sourcePath").asText();
            Path file = REPO_ROOT.resolve(source);
            if (!Files.isRegularFile(file)) {
                wrong.add(id + " — its source is not a file: " + source);
                continue;
            }
            String text = Files.readString(file);
            Set<String> asked = artifacts(card);

            if (text.contains(DOCX_IMPORT)) {
                docx++;
                if (!asked.contains("graph-compose-render-docx")) {
                    wrong.add(id + " — names the DOCX backend and does not ask a reader for it");
                }
            }
            // A card can publish a deck its own source never mentions — the flagship twins are
            // rendered by a sibling class — so the deck the card offers decides the coordinate
            // just as much as the call does. Keying on the call alone left four cards offering
            // a download a reader could not produce.
            boolean publishesDeck = card.hasNonNull("pptx");
            boolean callsPptx = PPTX_CALLS.stream().anyMatch(text::contains);
            if (publishesDeck || callsPptx) {
                pptx++;
                if (!asked.contains("graph-compose-render-pptx")) {
                    wrong.add(id + " — offers a deck and does not ask a reader for the PPTX backend");
                }
            }
            if (!text.contains(DOCX_IMPORT) && asked.contains("graph-compose-render-docx")) {
                wrong.add(id + " — asks for the DOCX backend its example never names");
            }
            if (!publishesDeck && !callsPptx && asked.contains("graph-compose-render-pptx")) {
                wrong.add(id + " — asks for the PPTX backend it neither renders to nor publishes");
            }
        }

        assertThat(docx)
                .describedAs("no example names the DOCX backend, so this guard could not tell a card that "
                        + "needs it from one that does not")
                .isGreaterThan(0);
        assertThat(pptx)
                .describedAs("no example renders a deck, so the PPTX half of this guard checks nothing")
                .isGreaterThan(0);
        assertThat(wrong)
                .describedAs("the artifacts on a card are the dependency block a reader pastes into their "
                        + "build. One short and the render throws where they cannot see why; one long and "
                        + "they take a backend their document never uses")
                .isEmpty();
    }

    @Test
    void onlyAnExampleWithAMainIsOfferedAsOneToRun() throws IOException {
        Set<String> wrong = new TreeSet<>();
        int runnable = 0;
        int driven = 0;

        for (JsonNode card : cards()) {
            String id = card.path("id").asText();
            Path file = REPO_ROOT.resolve(card.path("sourcePath").asText());
            JsonNode claim = card.get("runnable");
            if (claim == null || !claim.isBoolean()) {
                wrong.add(id + " — says nothing about whether its example can be run");
                continue;
            }
            if (!Files.isRegularFile(file)) {
                wrong.add(id + " — its source is not a file: " + card.path("sourcePath").asText());
                continue;
            }
            boolean hasMain = MAIN_METHOD.matcher(Files.readString(file)).find();
            if (hasMain != claim.asBoolean()) {
                wrong.add(id + " — says runnable=" + claim.asBoolean() + ", its class "
                        + (hasMain ? "has a main" : "has no main"));
            }
            if (hasMain) {
                runnable++;
            } else {
                driven++;
            }
        }

        assertThat(runnable)
                .describedAs("no example has a main, so the site could offer no run command at all")
                .isGreaterThan(0);
        assertThat(driven)
                .describedAs("every example has a main, so a blanket 'runnable' would pass here while "
                        + "hiding the case this guard exists for")
                .isGreaterThan(0);
        assertThat(wrong)
                .describedAs("the site prints an exec:java command from this flag, and a class without a "
                        + "main answers it with 'doesn't contain a main method'")
                .isEmpty();
    }

    private static Set<String> artifacts(JsonNode card) {
        Set<String> asked = new TreeSet<>();
        card.path("requiredArtifacts").forEach(artifact -> asked.add(artifact.asText()));
        return asked;
    }

    private static List<JsonNode> cards() throws IOException {
        JsonNode manifest = new ObjectMapper().readTree(Files.readString(MANIFEST));
        List<JsonNode> cards = new ArrayList<>();
        for (JsonNode category : manifest.path("categories")) {
            for (JsonNode group : category.path("groups")) {
                for (JsonNode card : group.path("examples")) {
                    cards.add(card);
                }
            }
        }
        assertThat(cards)
                .describedAs("web/examples.json lists no cards — these guards would check nothing")
                .isNotEmpty();
        return cards;
    }
}
