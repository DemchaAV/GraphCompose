package com.demcha.examples;

import com.demcha.examples.support.AssetContent;
import com.demcha.examples.support.ReadmeBannerV2Renderer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Holds the committed previews to the code that produces them.
 *
 * <p>The repository commits a preview of part of the example catalogue, and README and the
 * showcase site read those files rather than rendering anything. Nothing regenerated them, so
 * they drifted quietly: a change to an example, a theme or the engine moved the render while the
 * committed file stayed where it was, and the first time anybody noticed was when a release
 * published it. At the point this guard was written, 23 of the 67 committed previews were behind
 * the code — and the way that surfaced was a deck losing its bold weights for two releases
 * without a single test going red.</p>
 *
 * <p>The comparison is {@link AssetContent}: exact, with the handful of differences a machine
 * rather than an author writes named and dropped. So a failure here means the render moved, and
 * the fix is to re-render the named files or to revert what moved them — never to widen the
 * comparison.</p>
 *
 * <p>A committed preview is rendered at the <em>released</em> version while a working tree is
 * already on the next one, so this compares like with like only because the examples module runs
 * its tests with {@code graphcompose.examples.displayVersion} pinned to
 * {@code graphcompose.examples.assetVersion} — the version the committed files were rendered at,
 * recorded in {@code examples/pom.xml} and moved by the release script. Without that the pill in
 * every hero would differ and this would fail on every asset that carries one.</p>
 */
class CommittedAssetDriftTest {

    private static final Path ASSETS = Path.of("..", "assets", "readme");
    private static final Path PREVIEWS = ASSETS.resolve("examples");
    private static final String HERO = "repository_showcase_render.png";
    private static final int HERO_DPI = 200;

    /** The decks whose preview the repository commits — see {@link #UNPUBLISHED_DECKS}. */
    private static final Set<String> CURATED_DECKS = Set.of(
            "business-report.pptx",
            "financial-report.pptx",
            "master-showcase.pptx",
            "maven-banner.pptx",
            "social-card.pptx",
            "twin-output.pptx");

    /**
     * Decks the catalogue renders but the repository deliberately does not commit.
     *
     * <p>Which decks are published is a decision, and both lists exist so that no deck can arrive
     * without somebody making it. A subset alone is not enough — asserting only that the curated
     * decks are among the generated ones lets a new example ship a deck nobody decided about.</p>
     */
    private static final Set<String> UNPUBLISHED_DECKS = Set.of(
            "engine-deck.pptx",
            "linkedin-carousel.pptx");

    @BeforeAll
    static void generateEveryExample() throws Exception {
        GeneratedCatalogue.generateOnce();
    }

    @Test
    void everyCommittedPreviewMatchesWhatTheCatalogueRenders() throws Exception {
        Map<String, Path> generated = generatedByName();
        List<String> missing = new ArrayList<>();
        List<String> drifted = new ArrayList<>();

        for (String name : committedPreviews()) {
            Path fresh = generated.get(name);
            if (fresh == null) {
                missing.add(name);
            } else if (!AssetContent.digestOf(PREVIEWS.resolve(name))
                    .equals(AssetContent.digestOf(fresh))) {
                drifted.add(name);
            }
        }

        assertThat(missing)
                .describedAs("a committed preview that no example renders is a file nothing can "
                        + "refresh: delete it, or restore the example that produced it")
                .isEmpty();
        assertThat(drifted)
                .describedAs("a committed preview no longer matches what its example renders. The "
                        + "comparison drops what the machine writes, so this is a change to the "
                        + "document: re-render these files, or revert what moved them")
                .isEmpty();
    }

    /**
     * The README hero is committed too, and nothing else regenerates it.
     *
     * <p>It is not part of the catalogue — the release script renders it on its own — which is
     * exactly why it drifts unnoticed between releases. Rendering it here costs one page and
     * closes the one committed asset the catalogue comparison cannot see.</p>
     */
    @Test
    void theCommittedHeroMatchesWhatTheBannerRendererRenders(@TempDir Path directory)
            throws Exception {
        Path fresh = ReadmeBannerV2Renderer.render(directory.resolve(HERO), HERO_DPI);

        assertThat(AssetContent.digestOf(fresh))
                .describedAs("the committed README hero no longer matches what the banner "
                        + "renderer produces at %d DPI: re-render it, or revert what moved it",
                        HERO_DPI)
                .isEqualTo(AssetContent.digestOf(ASSETS.resolve(HERO)));
    }

    @Test
    void theTwoDeckListsDoNotOverlap() {
        assertThat(CURATED_DECKS)
                .describedAs("a deck cannot be both published and deliberately unpublished")
                .doesNotContainAnyElementsOf(UNPUBLISHED_DECKS);
    }

    @Test
    void everyRenderedDeckIsEitherPublishedOrDeliberatelyNot() throws Exception {
        Set<String> committedDecks = new TreeSet<>(committedPreviews());
        committedDecks.removeIf(name -> !name.endsWith(".pptx"));
        Set<String> renderedDecks = new TreeSet<>(generatedByName().keySet());
        renderedDecks.removeIf(name -> !name.endsWith(".pptx"));

        assertThat(committedDecks)
                .describedAs("the committed decks and the curated list have to agree: a deck added "
                        + "to the folder without a decision, or removed from it without one, is "
                        + "exactly what this list exists to surface")
                .isEqualTo(new TreeSet<>(CURATED_DECKS));

        Set<String> accountedFor = new TreeSet<>(CURATED_DECKS);
        accountedFor.addAll(UNPUBLISHED_DECKS);
        assertThat(renderedDecks)
                .describedAs("the catalogue and the two lists have to cover each other exactly: a "
                        + "deck listed but not rendered is a committed file nothing can refresh, "
                        + "and a deck rendered but on neither list is one nobody has decided to "
                        + "publish or to leave out")
                .isEqualTo(accountedFor);
    }

    private static Set<String> committedPreviews() throws IOException {
        Set<String> names = new TreeSet<>();
        try (var files = Files.list(PREVIEWS)) {
            files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .forEach(names::add);
        }
        return names;
    }

    /**
     * The catalogue keyed by file name, which is how the flat preview folder addresses it.
     *
     * <p>Two rendered documents sharing a name would leave one standing in for the other, and
     * whichever the walk reached first would decide what a committed file is compared against.</p>
     */
    private static Map<String, Path> generatedByName() throws IOException {
        Map<String, Path> byName = new TreeMap<>();
        try (var files = Files.walk(GeneratedCatalogue.ROOT)) {
            files.filter(Files::isRegularFile).forEach(path -> {
                Path clash = byName.put(path.getFileName().toString(), path);
                if (clash != null) {
                    throw new IllegalStateException(
                            "two rendered documents share the name " + path.getFileName()
                            + " (" + clash + " and " + path + "); the committed folder is flat, so "
                            + "one would silently stand in for the other");
                }
            });
        }
        return byName;
    }
}
