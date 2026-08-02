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
import java.util.TreeMap;
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

    @Test
    void everyCommittedDeckMatchesAFreshRenderPartForPart() throws Exception {
        List<String> compared = new ArrayList<>();
        List<String> differing = new ArrayList<>();

        try (var decks = Files.walk(GeneratedCatalogue.ROOT)) {
            for (Path fresh : decks.filter(path -> path.toString().endsWith(".pptx")).sorted().toList()) {
                Path committed = COMMITTED.resolve(fresh.getFileName().toString());
                if (!Files.isRegularFile(committed)) {
                    continue;
                }
                compared.add(committed.getFileName().toString());
                if (!canonicalDigest(committed).equals(canonicalDigest(fresh))) {
                    differing.add(committed.getFileName().toString());
                }
            }
        }

        assertThat(compared)
                .describedAs("no committed deck was found beside a generated one — the asset "
                        + "folder or the catalogue moved, and this guard compared nothing")
                .isNotEmpty();
        assertThat(differing)
                .describedAs("a committed deck no longer matches what the example produces. The "
                        + "comparison ignores zip timestamps, so this is a content change: "
                        + "re-render the asset, or revert whatever changed the example")
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
                    parts.put(entry.getName(), zip.readAllBytes());
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
}
