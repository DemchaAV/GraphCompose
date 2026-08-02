package com.demcha.examples;

import com.demcha.examples.flagships.MavenBannerPptxExample;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what a PPTX can be compared by, and proves the committed decks match a fresh
 * render under that comparison.
 *
 * <p>A deck cannot be compared byte for byte. Every entry in the package carries the zip
 * timestamp of the run that wrote it, so two renders of an unchanged document differ. The
 * engine can pin those — both fixed backends take a {@code deterministic(...)} instant —
 * but {@code buildPptx(Path)} routes through the convenience path, which resolves the
 * backend from the provider and gives a caller no way to configure it. Until that seam
 * exists, a gate comparing bytes would either fail forever or drop the decks.</p>
 *
 * <p>So the comparison is defined here: the sorted package parts and their contents, with
 * the zip metadata dropped. That is the whole document — slides, shapes, relationships,
 * embedded media — and nothing about when it was written. This is the comparator the asset
 * gate will use, exercised now on the decks the repository commits, so the gate arrives
 * with its comparison already proven instead of discovering the problem when it first
 * runs red.</p>
 */
class PptxCanonicalContentTest {

    private static final Path COMMITTED = Path.of("..", "assets", "readme", "examples");

    @BeforeAll
    static void generateEveryExample() throws Exception {
        GeneratedCatalogue.generateOnce();
    }

    /**
     * The committed decks are a curated subset, and which decks are in it is a decision.
     *
     * <p>Pairing generated files with committed ones and skipping the unpaired hides the
     * two failures worth catching: a deck added to the catalogue and never committed, and
     * a committed deck whose example is gone. Both leave every surviving pair matching, so
     * a content comparison alone reports success. The subset is therefore written down —
     * an unpaired file on either side is a decision somebody has to make, not something
     * for a guard to pass over.</p>
     */
    private static final Set<String> CURATED_DECKS = Set.of(
            "business-report.pptx",
            "financial-report.pptx",
            "master-showcase.pptx",
            "maven-banner.pptx",
            "social-card.pptx",
            "twin-output.pptx");

    @Test
    void theCommittedDecksAreExactlyTheCuratedSubset() throws Exception {
        Set<String> committed = new TreeSet<>();
        try (var files = Files.list(COMMITTED)) {
            files.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".pptx"))
                    .forEach(committed::add);
        }
        Set<String> generated = new TreeSet<>();
        try (var decks = Files.walk(GeneratedCatalogue.ROOT)) {
            decks.filter(path -> path.toString().endsWith(".pptx"))
                    .map(path -> path.getFileName().toString())
                    .forEach(generated::add);
        }

        assertThat(committed)
                .describedAs("the committed decks and the curated list have to agree: a deck "
                        + "added to the folder without a decision, or removed from it without "
                        + "one, is exactly what this list exists to surface")
                .isEqualTo(new TreeSet<>(CURATED_DECKS));
        assertThat(generated)
                .describedAs("every curated deck must still be produced by an example — one that "
                        + "is not is a committed file nothing can refresh")
                .containsAll(CURATED_DECKS);
    }

    @Test
    void everyCommittedDeckMatchesAFreshRenderPartForPart() throws Exception {
        Map<String, Path> generated = new TreeMap<>();
        try (var decks = Files.walk(GeneratedCatalogue.ROOT)) {
            decks.filter(path -> path.toString().endsWith(".pptx"))
                    .forEach(path -> {
                        Path clash = generated.put(path.getFileName().toString(), path);
                        if (clash != null) {
                            throw new IllegalStateException(
                                    "two generated decks share the name " + path.getFileName()
                                    + " (" + clash + " and " + path + "); the committed gallery is "
                                    + "flat, so one would silently stand in for the other");
                        }
                    });
        }

        List<String> differing = new ArrayList<>();
        for (String deck : new TreeSet<>(CURATED_DECKS)) {
            Path committed = COMMITTED.resolve(deck);
            Path fresh = generated.get(deck);
            assertThat(committed).describedAs("curated deck %s is not committed", deck).exists();
            assertThat(fresh).describedAs("curated deck %s is not generated", deck).isNotNull();
            if (!canonicalDigest(committed).equals(canonicalDigest(fresh))) {
                differing.add(deck);
            }
        }

        assertThat(differing)
                .describedAs("a committed deck no longer matches what the example produces. The "
                        + "comparison ignores zip timestamps and the platform's line endings, so "
                        + "this is a content change: re-render the asset, or revert whatever "
                        + "changed the example")
                .isEmpty();
    }

    /**
     * Rendering the same deck twice produces one document under this digest.
     *
     * <p>Only the equality is asserted. Whether the two files also differ in bytes depends
     * on the clock — zip entry timestamps have two-second granularity, so two renders in
     * quick succession can land on the same stamp and produce identical files, while two
     * a moment apart do not. That is the reason the gate cannot hash the file, and it is
     * also the reason it cannot be asserted: the property is real but intermittent, and a
     * test that pins it fails on whichever machine happens to be fast.</p>
     */
    @Test
    void theSameDeckRenderedTwiceIsOneDocument() throws Exception {
        Path deck = GeneratedCatalogue.ROOT.resolve("flagships").resolve("maven-banner.pptx");
        assertThat(deck).exists();

        String firstDigest = canonicalDigest(deck);
        MavenBannerPptxExample.generate();

        assertThat(canonicalDigest(deck))
                .describedAs("the same document rendered twice must be one document")
                .isEqualTo(firstDigest);
    }

    /** The package's parts and their contents, with everything the zip adds left out. */
    private static String canonicalDigest(Path pptx) throws Exception {
        Map<String, byte[]> parts = new TreeMap<>();
        try (ZipInputStream zip =
                     new ZipInputStream(new ByteArrayInputStream(Files.readAllBytes(pptx)))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (!entry.isDirectory()) {
                    parts.put(entry.getName(), canonicalise(entry.getName(), zip.readAllBytes()));
                }
            }
        }

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        parts.forEach((name, content) -> {
            digest.update(name.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(content);
            digest.update((byte) 0);
        });
        return HexFormat.of().formatHex(digest.digest());
    }

    /**
     * Strips the one difference that is about the machine rather than the document.
     *
     * <p>POI ends the XML declaration with the platform's line separator, so every XML
     * part of a deck written on Windows differs from the same deck written on Linux by a
     * single byte. Sixteen parts of an untouched deck differed for that reason alone —
     * enough to fail a comparison that is otherwise exact, and the sort of difference a
     * gate must not report as a content change. Binary parts are left alone: a stray CR
     * inside an embedded font or image is content.</p>
     */
    private static byte[] canonicalise(String name, byte[] content) {
        if (!name.endsWith(".xml") && !name.endsWith(".rels")) {
            return content;
        }
        return new String(content, StandardCharsets.UTF_8)
                .replace("\r\n", "\n")
                .getBytes(StandardCharsets.UTF_8);
    }
}
