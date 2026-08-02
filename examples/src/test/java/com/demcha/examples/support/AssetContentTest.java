package com.demcha.examples.support;

import com.demcha.examples.GeneratedCatalogue;
import com.demcha.examples.flagships.MavenBannerPptxExample;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the comparison drops what the machine writes and nothing else.
 *
 * <p>{@link AssetContent} is what the asset gate reads a document through, so each reduction it
 * makes is checked in both directions: the difference it is meant to absorb comes out equal, and
 * the change it must never absorb comes out different. A reduction that quietly stops matching —
 * a reordered attribute, a mistyped part name — would leave a gate that compares nothing and
 * passes everything, so the ones driven by patterns are also run against markup POI really
 * wrote.</p>
 */
class AssetContentTest {

    private static final Path COMMITTED = Path.of("..", "assets", "readme", "examples");
    private static final String WATERMARK_WINDOWS = "watermark-windows.png";
    private static final String WATERMARK_LINUX = "watermark-linux.png";
    private static final String WATERMARK_DECK = "master-showcase.pptx";
    private static final String WATERMARK_PART = "ppt/media/image1.png";

    @BeforeAll
    static void generateEveryExample() throws Exception {
        GeneratedCatalogue.generateOnce();
    }

    /**
     * Rendering the same deck twice produces one document under this comparison.
     *
     * <p>Only the equality is asserted. Whether the two files also differ in bytes depends on the
     * clock — zip entry timestamps have two-second granularity, so two renders in quick
     * succession can land on the same stamp and produce identical files, while two a moment apart
     * do not. That is the reason the gate cannot hash the file, and it is also the reason it
     * cannot be asserted: the property is real but intermittent, and a test that pins it fails on
     * whichever machine happens to be fast.</p>
     */
    @Test
    void theSameDeckRenderedTwiceIsOneDocument() throws Exception {
        Path deck = GeneratedCatalogue.ROOT.resolve("flagships").resolve("maven-banner.pptx");
        assertThat(deck).exists();

        String first = AssetContent.digestOf(deck);
        MavenBannerPptxExample.generate();

        assertThat(AssetContent.digestOf(deck))
                .describedAs("the same document rendered twice must be one document")
                .isEqualTo(first);
    }

    /** A PDF that differs only in the identifier the clock seeds is one document. */
    @Test
    void aPdfCarryingADifferentIdentifierIsTheSameDocument() throws Exception {
        Path pdf = GeneratedCatalogue.ROOT.resolve("flagships").resolve("maven-banner.pdf");
        assertThat(pdf).exists();
        byte[] rendered = Files.readAllBytes(pdf);
        byte[] reseeded = new String(rendered, StandardCharsets.ISO_8859_1)
                .replaceFirst("/ID \\[<[0-9A-Fa-f]+> <[0-9A-Fa-f]+>\\]",
                        "/ID [<" + "A".repeat(64) + "> <" + "A".repeat(64) + ">]")
                .getBytes(StandardCharsets.ISO_8859_1);

        assertThat(reseeded)
                .describedAs("the fixture must actually carry an /ID for this to prove anything")
                .isNotEqualTo(rendered);
        assertThat(AssetContent.withoutPdfId(reseeded))
                .describedAs("a fresh /ID is the clock writing, not the document")
                .isEqualTo(AssetContent.withoutPdfId(rendered));
    }

    /**
     * The same path drawn in a different box is one shape; a path that moves is not.
     *
     * <p>Dropping the box is the load-bearing half of the comparison, so it is checked in both
     * directions: the shapes below are the same freeform normalised the two ways two machines
     * normalised it, and then genuinely moved. Absorbing the third as well would leave the gate
     * reporting success on a deck whose artwork had shifted.</p>
     */
    @Test
    void theBoxIsAbsorbedButAMoveIsNot() {
        String box = freeform("<a:off x=\"1000\" y=\"2000\"/><a:ext cx=\"600\" cy=\"400\"/>",
                "<a:pt x=\"0\" y=\"0\"/>", "<a:pt x=\"600\" y=\"400\"/>");
        String sameLineOtherBox = freeform(
                "<a:off x=\"1050\" y=\"2050\"/><a:ext cx=\"600\" cy=\"400\"/>",
                "<a:pt x=\"-50\" y=\"-50\"/>", "<a:pt x=\"550\" y=\"350\"/>");
        String movedLine = freeform("<a:off x=\"1050\" y=\"2050\"/><a:ext cx=\"600\" cy=\"400\"/>",
                "<a:pt x=\"0\" y=\"0\"/>", "<a:pt x=\"600\" y=\"400\"/>");

        assertThat(AssetContent.freeformsInSlideSpace(sameLineOtherBox))
                .describedAs("the same line from (1000,2000) to (1600,2400), normalised against "
                        + "an origin 50 EMU away — the difference two machines produce")
                .isEqualTo(AssetContent.freeformsInSlideSpace(box));
        assertThat(AssetContent.freeformsInSlideSpace(movedLine))
                .describedAs("a line that actually moved by 50 EMU must not be absorbed")
                .isNotEqualTo(AssetContent.freeformsInSlideSpace(box));
    }

    /**
     * The rewrite reaches the markup POI actually writes, not just the shape of it.
     *
     * <p>{@link #theBoxIsAbsorbedButAMoveIsNot} works on markup written here, so it would still
     * pass if POI reordered an attribute and the rewrite quietly stopped matching — leaving a
     * comparison that absorbs nothing and a red gate nobody can read. A deck the repository
     * commits has to come out changed.</p>
     */
    @Test
    void theRewriteReachesRealMarkup() throws Exception {
        String slide = part(COMMITTED.resolve("twin-output.pptx"), "ppt/slides/slide1.xml");

        assertThat(AssetContent.freeformsInSlideSpace(slide))
                .describedAs("no freeform in a deck full of them was rewritten — the markup no "
                        + "longer looks the way the patterns expect")
                .isNotEqualTo(slide);
    }

    /**
     * An image of the same size is not the same image.
     *
     * <p>Reading a raster part by its dimensions would let a logo be swapped, a screenshot be
     * replaced or a watermark be retyped without the comparison noticing, which is a quiet way
     * for a gate to report that nothing changed. Only the parts named in
     * {@link AssetContent#UNSTABLE_PARTS} are read that way; the fixtures are the same watermark
     * from two machines, so they are the closest two images this repository has, and even they
     * must come out different when compared as images.</p>
     */
    @Test
    void sameDimensionsButDifferentPixelsAreNotEqual() throws Exception {
        BufferedImage windows = decode(WATERMARK_WINDOWS);
        BufferedImage linux = decode(WATERMARK_LINUX);
        assertThat(windows.getWidth()).isEqualTo(linux.getWidth());
        assertThat(windows.getHeight()).isEqualTo(linux.getHeight());

        assertThat(AssetContent.part("business-report.pptx", WATERMARK_PART,
                fixture(WATERMARK_WINDOWS)))
                .describedAs("two images of one size are not one image — and the deck this part "
                        + "sits in is not one the allowlist names")
                .isNotEqualTo(AssetContent.part("business-report.pptx", WATERMARK_PART,
                        fixture(WATERMARK_LINUX)));
    }

    /**
     * The named part absorbs the difference that was measured, and the fixtures carry it.
     *
     * <p>The two files are {@code ppt/media/image1.png} of the showcase deck rendered on Windows
     * and on the runner: same glyphs in the same places, different coverage along their edges.
     * Pinning them here keeps the allowlist honest — if the difference ever stops being
     * antialiasing the entry stops being justified, and this is where that shows.</p>
     */
    @Test
    void theNamedUnstablePartAbsorbsTheMeasuredDifference() throws Exception {
        byte[] windows = fixture(WATERMARK_WINDOWS);
        byte[] linux = fixture(WATERMARK_LINUX);
        assertThat(windows)
                .describedAs("the fixtures have to be two renders, not one file twice")
                .isNotEqualTo(linux);

        assertThat(AssetContent.part(WATERMARK_DECK, WATERMARK_PART, windows))
                .describedAs("the watermark named in the allowlist is the one difference the "
                        + "comparison is allowed to pass over")
                .isEqualTo(AssetContent.part(WATERMARK_DECK, WATERMARK_PART, linux));
    }

    /**
     * The exemption is for two known renders, not for whatever occupies that path.
     *
     * <p>Exempting the part itself would let the watermark be swapped for another image of the
     * same size — a different word, a logo, a blank — and the comparison would report a deck
     * unchanged. Only the two digests written down collapse; a third image keeps its own.</p>
     */
    @Test
    void anUnknownImageOfTheSameSizeIsNotAbsorbed() throws Exception {
        BufferedImage watermark = decode(WATERMARK_WINDOWS);
        byte[] blank = blankPng(watermark.getWidth(), watermark.getHeight());
        assertThat(AssetContent.pixelDigest(blank))
                .describedAs("the substitute must decode, or this proves nothing")
                .isNotNull();

        assertThat(AssetContent.part(WATERMARK_DECK, WATERMARK_PART, blank))
                .describedAs("an image the allowlist has never seen is compared by its pixels, "
                        + "however well it matches the size of the one that is exempt")
                .isNotEqualTo(AssetContent.part(WATERMARK_DECK, WATERMARK_PART,
                        fixture(WATERMARK_WINDOWS)));
    }

    /**
     * The part the allowlist exempts is still the watermark that was measured.
     *
     * <p>Naming a path and pinning two digests protects nothing if the deck stopped carrying
     * either of them: the entry would silently become an exemption for an image nobody has
     * looked at. Both the committed deck and the one the catalogue renders have to hold a
     * version the fixtures account for.</p>
     */
    @Test
    void theAllowlistedPartInBothDecksIsAKnownRender() throws Exception {
        Set<String> known = AssetContent.UNSTABLE_PARTS.get(WATERMARK_DECK + "!" + WATERMARK_PART);
        assertThat(known).describedAs("the allowlist entry has no digests").isNotEmpty();

        for (Path deck : List.of(COMMITTED.resolve(WATERMARK_DECK), generated(WATERMARK_DECK))) {
            assertThat(AssetContent.pixelDigest(bytesOfPart(deck, WATERMARK_PART)))
                    .describedAs("%s carries a watermark neither fixture accounts for — the "
                            + "exemption no longer describes what it exempts", deck)
                    .isIn(known);
        }
    }

    /** A date somebody wrote is not a date the machine wrote. */
    @Test
    void aCustomDatePropertyIsNotAbsorbed() throws Exception {
        String properties = "<Properties><property name=\"reviewed\"><vt:filetime>%s"
                            + "</vt:filetime></property></Properties>";
        byte[] earlier = properties.formatted("2026-01-01T00:00:00Z")
                .getBytes(StandardCharsets.UTF_8);
        byte[] later = properties.formatted("2026-08-02T00:00:00Z")
                .getBytes(StandardCharsets.UTF_8);

        assertThat(AssetContent.part("word-export-companion.docx", "docProps/custom.xml", earlier))
                .describedAs("only the element that records when the package was written is "
                        + "dropped; a date the document states is the document's")
                .isNotEqualTo(AssetContent.part("word-export-companion.docx",
                        "docProps/custom.xml", later));
    }

    /**
     * A part written with either line ending is one part.
     *
     * <p>This is the reduction that failed six decks on the runner, and the one whose absence
     * shows up nowhere else: every test here runs on one machine, where both sides carry the same
     * line ending and the difference hides.</p>
     */
    @Test
    void theSamePartWithEitherLineEndingIsOnePart() throws Exception {
        String declaration = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
        byte[] unix = (declaration + "\n<p:sld/>").getBytes(StandardCharsets.UTF_8);
        byte[] windows = (declaration + "\r\n<p:sld/>").getBytes(StandardCharsets.UTF_8);

        assertThat(AssetContent.part(WATERMARK_DECK, "ppt/slides/slide1.xml", windows))
                .describedAs("POI ends the XML declaration with the platform's line separator; "
                        + "that is the machine writing, not the document")
                .isEqualTo(AssetContent.part(WATERMARK_DECK, "ppt/slides/slide1.xml", unix));
    }

    /** The stamp a package records for when it was written is not part of the document. */
    @Test
    void theCreationStampIsNotPartOfTheDocument() throws Exception {
        String properties = "<cp:coreProperties><dcterms:created>%s</dcterms:created>"
                            + "</cp:coreProperties>";
        byte[] earlier = properties.formatted("2026-08-02T16:15:46Z")
                .getBytes(StandardCharsets.UTF_8);
        byte[] later = properties.formatted("2026-08-02T16:46:46Z")
                .getBytes(StandardCharsets.UTF_8);

        assertThat(AssetContent.part("word-export-companion.docx", "docProps/core.xml", earlier))
                .describedAs("two renders of one document minutes apart are one document")
                .isEqualTo(AssetContent.part("word-export-companion.docx", "docProps/core.xml",
                        later));
    }

    /**
     * Every allowlisted part names a part that is really there.
     *
     * <p>A misspelled document or part would match nothing, and an entry that matches nothing
     * exempts nothing — the comparison would quietly go back to reading the watermark by its
     * pixels and fail on a runner for the reason the entry was added to explain.</p>
     */
    @Test
    void everyAllowlistedPartExists() throws Exception {
        for (String entry : new TreeSet<>(AssetContent.UNSTABLE_PARTS.keySet())) {
            String[] split = entry.split("!", 2);
            assertThat(split).describedAs("%s is not document!part", entry).hasSize(2);
            for (Path document : List.of(COMMITTED.resolve(split[0]), generated(split[0]))) {
                assertThat(document)
                        .describedAs("allowlisted document %s is missing", split[0]).exists();
                assertThat(partNames(document))
                        .describedAs("allowlisted part %s is not in %s", split[1], document)
                        .contains(split[1]);
            }
        }
    }

    private static Path generated(String name) throws IOException {
        try (var files = Files.walk(GeneratedCatalogue.ROOT)) {
            return files.filter(path -> path.getFileName().toString().equals(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "the catalogue no longer renders " + name));
        }
    }

    private static Set<String> partNames(Path archive) throws IOException {
        Set<String> names = new TreeSet<>();
        try (ZipInputStream zip =
                     new ZipInputStream(new ByteArrayInputStream(Files.readAllBytes(archive)))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                names.add(entry.getName());
            }
        }
        return names;
    }

    private static String part(Path archive, String name) throws IOException {
        return new String(bytesOfPart(archive, name), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static byte[] bytesOfPart(Path archive, String name) throws IOException {
        try (ZipInputStream zip =
                     new ZipInputStream(new ByteArrayInputStream(Files.readAllBytes(archive)))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (entry.getName().equals(name)) {
                    return zip.readAllBytes();
                }
            }
        }
        throw new IllegalStateException(archive + " has no part " + name);
    }

    /** A blank image of a given size — a stand-in for whatever else could occupy that path. */
    private static byte[] blankPng(int width, int height) throws IOException {
        BufferedImage blank = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var bytes = new java.io.ByteArrayOutputStream();
        ImageIO.write(blank, "png", bytes);
        return bytes.toByteArray();
    }

    private static byte[] fixture(String name) throws IOException {
        try (var in = AssetContentTest.class.getResourceAsStream("/pptx-media/" + name)) {
            assertThat(in).describedAs("missing test fixture %s", name).isNotNull();
            return in.readAllBytes();
        }
    }

    private static BufferedImage decode(String name) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(fixture(name)));
        assertThat(image).describedAs("fixture %s does not decode as an image", name).isNotNull();
        return image;
    }

    private static String freeform(String xfrm, String from, String to) {
        return "<p:sp><p:nvSpPr><p:cNvPr name=\"Freeform 1\" id=\"1\"/></p:nvSpPr><p:spPr>"
               + "<a:xfrm>" + xfrm + "</a:xfrm><a:custGeom><a:pathLst>"
               + "<a:path h=\"400\" w=\"600\">"
               + "<a:moveTo>" + from + "</a:moveTo><a:lnTo>" + to + "</a:lnTo>"
               + "</a:path></a:pathLst></a:custGeom></p:spPr></p:sp>";
    }
}
