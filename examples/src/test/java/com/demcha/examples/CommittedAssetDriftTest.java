package com.demcha.examples;

import com.demcha.examples.support.AssetContent;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
 * recorded in {@code examples/pom.xml}. Without that the coordinate pill alone would differ and
 * this would fail on every preview that carries one.</p>
 *
 * <p>Nothing moves that property yet, and this guard cannot notice: it compares both sides at
 * whatever the property says, so a release that re-renders the showcase site at a new version
 * while leaving the previews behind passes here. Closing that is the release step's job, and it
 * is the reason to land it alongside rather than long after.</p>
 */
class CommittedAssetDriftTest {

    private static final Path ASSETS = Path.of("..", "assets", "readme");
    private static final Path PREVIEWS = ASSETS.resolve("examples");

    /** What counts as a document: the catalogue also writes a preview image and a data file. */
    private static final Set<String> DOCUMENTS = Set.of(".pdf", ".pptx", ".docx");

    /**
     * The documents the catalogue renders and the repository deliberately does not commit.
     *
     * <p>Publishing a preview is a decision, and this is the half of it worth writing down: the
     * other half is the folder itself, and the two together have to account for the catalogue
     * exactly. Without that, comparing only the files that happen to be there means deleting one
     * removes it from its own guard — README would lose a figure and every test would stay
     * green.</p>
     *
     * <p>Listing what is <em>not</em> published rather than what is keeps the shorter list, and
     * puts the decision where it is actually made: a new example ships unpublished unless somebody
     * says otherwise, and saying so is adding a file to the folder and a name off this list.</p>
     */
    private static final Set<String> UNPUBLISHED_PREVIEWS = Set.of(
            "cover-letter-blue-banner-v2.pdf",
            "cover-letter-boxed-sections-v2.pdf",
            "cover-letter-centered-headline-v2.pdf",
            "cover-letter-classic-serif-v2.pdf",
            "cover-letter-compact-mono-v2.pdf",
            "cover-letter-editorial-blue-v2.pdf",
            "cover-letter-engineering-resume-v2.pdf",
            "cover-letter-executive-v2.pdf",
            "cover-letter-mint-editorial-v2.pdf",
            "cover-letter-modern-professional-v2.pdf",
            "cover-letter-monogram-sidebar-v2.pdf",
            "cover-letter-nordic-clean-v2.pdf",
            "cover-letter-panel-v2.pdf",
            "cover-letter-sidebar-portrait-v2.pdf",
            "cover-letter-timeline-minimal-v2.pdf",
            "cv-blue-banner-v2.pdf",
            "cv-boxed-sections-v2.pdf",
            "cv-centered-headline-v2.pdf",
            "cv-editorial-blue-v2.pdf",
            "cv-executive-v2.pdf",
            "cv-minimal-underlined-v2.pdf",
            "cv-mint-editorial-v2-custom.pdf",
            "cv-mint-editorial-v2.pdf",
            "cv-monogram-sidebar-v2.pdf",
            "cv-sidebar-portrait-v2.pdf",
            "emoji-clip-path.pdf",
            "emoji-gallery.pdf",
            "emoji-svg-vs-png.pdf",
            "engine-deck.pptx",
            "invoice-modern-v2.pdf",
            "linkedin-carousel.pdf",
            "linkedin-carousel.pptx",
            "photo-clip.pdf",
            "poetry-title.pdf",
            "proposal-modern-v2.pdf");

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
     * The figures beside the preview folder are rasters, and this cannot compare a raster.
     *
     * <p>Everything under {@link #PREVIEWS} is a document, and two machines write one document the
     * same way once the reduction is applied. These are not: each is a page rasterised to a PNG for
     * README, and rasterising text is where two machines disagree — the same measurement that put
     * the showcase watermark in {@link AssetContent#UNSTABLE_PARTS}, over a whole page rather than
     * one band of it. Comparing them would mean a pixel budget, and a budget is a number nobody can
     * defend a year later.</p>
     *
     * <p>So they are written down instead. The list is what this guard can say about them: a
     * figure arriving here that is not on it is a file nothing checks, and it should arrive as a
     * decision rather than as a commit nobody read. The same holds for a folder — {@code v1.5} is
     * the figures of a released line, where re-rendering would itself be the bug, and a second
     * folder wants that said out loud rather than assumed.</p>
     */
    private static final Set<String> RASTER_FIGURES = Set.of(
            "barcode-showcase.png",
            "chart-showcase.png",
            "feature-catalog.png",
            "repository_showcase_render.png",
            "social-card.png",
            "twin-output-editing.png",
            "twin-output-pdf.png",
            "twin-output-pptx.png");

    /** The folders under {@code assets/readme}: the previews this compares, and 1.5's figures. */
    private static final Set<String> ASSET_FOLDERS = Set.of("examples", "v1.5");

    @Test
    void theOnlyAssetsThisCannotCompareAreTheOnesWrittenDown() throws Exception {
        Set<String> beside = new TreeSet<>();
        Set<String> folders = new TreeSet<>();
        try (var files = Files.list(ASSETS)) {
            files.forEach(path -> (Files.isDirectory(path) ? folders : beside)
                    .add(path.getFileName().toString()));
        }

        assertThat(beside)
                .describedAs("an asset beside %s is compared by nothing: either move it in with the "
                        + "previews so it is, or add it here with the reason it cannot be", PREVIEWS)
                .isEqualTo(new TreeSet<>(RASTER_FIGURES));
        assertThat(folders)
                .describedAs("a folder of assets beside %s is compared by nothing either, and a "
                        + "whole folder is easier to add without noticing than a file", PREVIEWS)
                .isEqualTo(new TreeSet<>(ASSET_FOLDERS));
    }

    /**
     * The catalogue is exactly the published previews plus the deliberately unpublished ones.
     *
     * <p>This is what makes a deletion visible. The comparison above only reads the files that
     * are there, so removing one takes it out of its own guard: README loses a figure and nothing
     * goes red. Pinning the whole catalogue against the folder plus the list catches that, and
     * catches its opposites too — a new example nobody decided to publish, a preview added
     * without a source, a rename that lands as one of each.</p>
     */
    @Test
    void theCatalogueIsExactlyThePublishedPreviewsPlusTheUnpublishedOnes() throws Exception {
        Set<String> committed = new TreeSet<>(committedPreviews());
        assertThat(committed)
                .describedAs("a preview cannot be published and deliberately unpublished at once")
                .doesNotContainAnyElementsOf(UNPUBLISHED_PREVIEWS);

        Set<String> accountedFor = new TreeSet<>(committed);
        accountedFor.addAll(UNPUBLISHED_PREVIEWS);

        Set<String> rendered = new TreeSet<>(generatedByName().keySet());
        rendered.removeIf(name -> DOCUMENTS.stream().noneMatch(name::endsWith));

        assertThat(rendered)
                .describedAs("the catalogue, the committed folder and the unpublished list have to "
                        + "cover each other exactly. A document rendered but neither committed nor "
                        + "listed is one nobody decided about; a preview committed but no longer "
                        + "rendered is a file nothing can refresh; and a published preview deleted "
                        + "is a figure README loses — the one this comparison cannot see on its "
                        + "own, since a file that is gone is a file it never looks at")
                .isEqualTo(accountedFor);
    }

    /**
     * The preview folder's files, and it has to be flat to have only files.
     *
     * <p>Listing it and keeping the regular files would step over a folder in silence, and a
     * preview one level down would be in none of the sets these tests compare: not held to the
     * catalogue, not published, not unpublished, not named as something that cannot be compared.
     * The site reads this folder flat, so a folder inside it is a mistake worth naming rather
     * than passing over.</p>
     */
    private static Set<String> committedPreviews() throws IOException {
        Set<String> names = new TreeSet<>();
        Set<String> folders = new TreeSet<>();
        try (var files = Files.list(PREVIEWS)) {
            files.forEach(path -> (Files.isDirectory(path) ? folders : names)
                    .add(path.getFileName().toString()));
        }
        assertThat(folders)
                .describedAs("%s is read flat, so a folder inside it holds previews nothing "
                        + "compares: move them up beside the others", PREVIEWS)
                .isEmpty();
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
