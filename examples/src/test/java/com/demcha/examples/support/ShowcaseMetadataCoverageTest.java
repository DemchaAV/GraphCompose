package com.demcha.examples.support;

import com.demcha.examples.GeneratedCatalogue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the hand-kept showcase register against what the runner actually produces.
 *
 * <p>{@code ShowcaseMetadata.lookup} falls back to a filename-derived card, so the
 * register is never consulted for a document that does not exist and an entry pointing
 * at nothing is inert: no warning, no failure, no card. The Maven banner sat that way —
 * a title, a description, tags and a source link for a PDF the runner never wrote.</p>
 *
 * <p>The reverse direction is deliberately not asserted. A generated document without an
 * entry still reaches the site through the fallback, which is the point of having one.</p>
 */
class ShowcaseMetadataCoverageTest {

    /** The module directory is the working directory; the repository is its parent. */
    private static final Path REPO_ROOT = Path.of("..").toAbsolutePath().normalize();

    /** The segment that marks where a source link stops being a URL and starts being a path. */
    private static final String SOURCE_PATH_MARKER = "/examples/src/main/java/";

    @BeforeAll
    static void generateEveryExample() throws Exception {
        GeneratedCatalogue.generateOnce();
    }

    @Test
    void everyRegisteredEntryDescribesAGeneratedDocument() throws IOException {
        Set<String> generated = generatedBasenames();
        Map<String, ShowcaseMetadata.Entry> entries = ShowcaseMetadata.registeredEntries();

        assertThat(entries)
                .describedAs("the register is empty — the guard would have nothing to check")
                .isNotEmpty();
        assertThat(generated)
                .describedAs("no generated PDF was found under %s — every entry would look "
                        + "orphaned and the failure would say nothing about the register",
                        GeneratedCatalogue.ROOT)
                .isNotEmpty();

        Set<String> orphaned = new TreeSet<>(entries.keySet());
        orphaned.removeAll(generated);

        assertThat(orphaned)
                .describedAs("an entry keyed on a basename the runner never writes is dead "
                        + "weight: the card it describes is never built, and the drift between "
                        + "the register and the catalogue is invisible at runtime")
                .isEmpty();
    }

    /**
     * The source link on every entry points at a file that exists.
     *
     * <p>A renamed or moved example leaves the card intact and its "view source" link
     * pointing at a 404 on the published site — visible to a reader, invisible here,
     * because nothing in the build follows it. Checked against the working tree rather
     * than over the network, so it stays deterministic and offline.</p>
     */
    @Test
    void everySourceLinkResolvesToAFileInTheRepository() {
        Set<String> broken = new TreeSet<>();
        ShowcaseMetadata.registeredEntries().forEach((basename, entry) -> {
            String url = entry.codeUrl();
            int at = url.indexOf(SOURCE_PATH_MARKER);
            if (at < 0) {
                broken.add(basename + " — source link does not point into the examples module: " + url);
                return;
            }
            String relative = url.substring(at + 1);
            if (!Files.isRegularFile(REPO_ROOT.resolve(relative))) {
                broken.add(basename + " — " + relative);
            }
        });

        assertThat(broken)
                .describedAs("a showcase card links to the source that produced it; a link to a "
                        + "file that no longer exists is a 404 on the published site")
                .isEmpty();
    }

    private static Set<String> generatedBasenames() throws IOException {
        Set<String> basenames = new TreeSet<>();
        try (Stream<Path> walk = Files.walk(GeneratedCatalogue.ROOT)) {
            walk.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".pdf"))
                    .map(name -> name.substring(0, name.length() - ".pdf".length()))
                    .forEach(basenames::add);
        }
        return basenames;
    }
}
