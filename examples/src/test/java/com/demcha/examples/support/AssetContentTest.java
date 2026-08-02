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
    private static final String TWIN_SLIDE = "twin-output.pptx!ppt/slides/slide1.xml";

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
     * Two shapes drawn around different centres are two shapes.
     *
     * <p>An earlier comparison read every freeform's path in slide coordinates and dropped the box
     * those coordinates were measured against. The paths below land in the same place and turn by
     * the same angle, and they are still different pictures: {@code rot} turns a shape around the
     * centre of its box, so a 100-wide box and a 200-wide one spin the same line about different
     * points. Nothing may collapse them.</p>
     */
    @Test
    void rotatedFreeformsWithDifferentPivotsAreNotEqual() {
        String narrow = rotatedFreeform("<a:off x=\"0\" y=\"0\"/><a:ext cx=\"100\" cy=\"100\"/>");
        String wide = rotatedFreeform("<a:off x=\"0\" y=\"0\"/><a:ext cx=\"200\" cy=\"100\"/>");

        assertThat(AssetContent.knownShapesCollapsed(TWIN_SLIDE, wide))
                .describedAs("the same line turned 90 degrees about a different centre is not the "
                        + "same picture, however the points are written")
                .isNotEqualTo(AssetContent.knownShapesCollapsed(TWIN_SLIDE, narrow));
    }

    /**
     * The two shapes the allowlist names collapse; a shape it does not name is untouched.
     *
     * <p>What was measured is two freeforms in one deck, so two freeforms in one deck are what is
     * exempted. A shape reaching this with any other name — or either of these two after somebody
     * changed it — comes out of the comparison exactly as it went in.</p>
     */
    @Test
    void onlyTheNamedShapesCollapse() throws Exception {
        String slide = part(COMMITTED.resolve("twin-output.pptx"), "ppt/slides/slide1.xml");
        String collapsed = AssetContent.knownShapesCollapsed(TWIN_SLIDE, slide);

        assertThat(collapsed)
                .describedAs("the two measured shapes have to be recognised in the deck they were "
                        + "measured in, or the exemption describes nothing")
                .contains("Freeform 41/>", "Freeform 51/>");
        assertThat(collapsed)
                .describedAs("a shape the allowlist does not name is compared as it was written")
                .contains("<p:cNvPr name=\"Freeform 42\"");
        assertThat(AssetContent.knownShapesCollapsed("other.pptx!ppt/slides/slide1.xml", slide))
                .describedAs("the exemption is keyed on the deck as well as the shape")
                .isEqualTo(slide);
    }

    /**
     * The shapes the allowlist exempts are the ones both decks still carry.
     *
     * <p>The digests are of what two machines wrote at the time they were measured. If the example
     * moves either shape, both sides move together and the pair silently stops being exempt — the
     * comparison would go back to reading them and fail on a runner for a reason the entry was
     * added to explain.</p>
     */
    @Test
    void theNamedShapesAreStillWhatBothDecksCarry() throws Exception {
        for (Path deck : List.of(COMMITTED.resolve("twin-output.pptx"),
                generated("twin-output.pptx"))) {
            String slide = part(deck, "ppt/slides/slide1.xml");
            assertThat(AssetContent.knownShapesCollapsed(TWIN_SLIDE, slide))
                    .describedAs("%s no longer carries the shapes the allowlist was measured on",
                            deck)
                    .contains("Freeform 41/>", "Freeform 51/>");
        }
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

    /** One line, turned a quarter turn, inside whatever box the caller gives it. */
    private static String rotatedFreeform(String xfrm) {
        return "<p:sp><p:nvSpPr><p:cNvPr name=\"Freeform 1\" id=\"1\"/></p:nvSpPr><p:spPr>"
               + "<a:xfrm rot=\"5400000\">" + xfrm + "</a:xfrm><a:custGeom><a:pathLst>"
               + "<a:path h=\"100\" w=\"100\">"
               + "<a:moveTo><a:pt x=\"0\" y=\"0\"/></a:moveTo>"
               + "<a:lnTo><a:pt x=\"100\" y=\"100\"/></a:lnTo>"
               + "</a:path></a:pathLst></a:custGeom></p:spPr></p:sp>";
    }
}
