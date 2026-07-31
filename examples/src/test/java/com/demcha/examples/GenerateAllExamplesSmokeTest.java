package com.demcha.examples;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * CI-coverage smoke test: runs {@link GenerateAllExamples#main(String[])}
 * end-to-end and then asserts against what it actually wrote.
 *
 * <p>Each individual example invokes the full canonical pipeline
 * ({@code DocumentSession} -> {@code DocumentDsl} -> layout compile ->
 * render) and writes its output to {@code examples/target/generated-pdfs}.
 * If any example breaks at runtime, this test fails — closing the
 * audit's L4 gap that flagged {@code GenerateAllExamples} as never
 * exercised by CI.</p>
 *
 * <p>Completing without throwing is necessary but not sufficient, so the
 * assertions below read the output tree rather than the exit path.</p>
 *
 * <p>Lives under the sibling {@code examples/} Maven module so the
 * smoke runner's JUnit dependency does not pollute the main project's
 * test classpath.</p>
 */
class GenerateAllExamplesSmokeTest {

    private static final Path GENERATED_ROOT = Path.of("target", "generated-pdfs");

    @BeforeAll
    static void generateEveryExample() throws Exception {
        GenerateAllExamples.main(new String[0]);
    }

    @Test
    void generateAllExamplesWritesPdfs() {
        assertThat(GENERATED_ROOT)
                .describedAs("GenerateAllExamples must write its output under %s", GENERATED_ROOT)
                .isDirectoryRecursivelyContaining("glob:**/*.pdf");
    }

    /**
     * Every generated deck needs a PDF of the same name beside it.
     *
     * <p>{@code ShowcaseSync} builds one card per PDF and copies a {@code .pptx} only
     * as the same-named companion of one. A deck generated without its PDF twin is
     * therefore invisible to the published site: it lands in {@code target}, never
     * reaches {@code web/showcase}, and its hand-written {@code ShowcaseMetadata} entry
     * goes silently unused. That is exactly what happened to the Maven banner, whose
     * example exposes both {@code generate()} and {@code generatePdf()} while only the
     * first was wired into the runner.</p>
     */
    @Test
    void everyGeneratedDeckHasAPdfTwin() throws IOException {
        Set<String> orphanDecks = new TreeSet<>();
        try (Stream<Path> walk = Files.walk(GENERATED_ROOT)) {
            List<Path> decks = walk
                    .filter(path -> path.toString().endsWith(".pptx"))
                    .toList();
            for (Path deck : decks) {
                String fileName = deck.getFileName().toString();
                String stem = fileName.substring(0, fileName.length() - ".pptx".length());
                if (!Files.isRegularFile(deck.resolveSibling(stem + ".pdf"))) {
                    orphanDecks.add(GENERATED_ROOT.relativize(deck).toString().replace('\\', '/'));
                }
            }
        }

        assertThat(orphanDecks)
                .describedAs("a .pptx with no same-named .pdf beside it never reaches the "
                        + "published showcase — ShowcaseSync keys every card on a PDF and "
                        + "copies a deck only as that PDF's companion")
                .isEmpty();
    }

    /** Guards against a runner that silently narrows to one corner of the catalogue. */
    @Test
    void generatedCatalogueCoversEveryCategory() throws IOException {
        try (Stream<Path> walk = Files.walk(GENERATED_ROOT)) {
            Set<String> categories = walk
                    .filter(path -> path.toString().endsWith(".pdf"))
                    .map(path -> GENERATED_ROOT.relativize(path).getName(0).toString())
                    .collect(Collectors.toCollection(TreeSet::new));

            assertThat(categories)
                    .describedAs("the runner should cover the whole catalogue, not one corner")
                    .contains("templates", "features", "flagships");
        }
    }
}
