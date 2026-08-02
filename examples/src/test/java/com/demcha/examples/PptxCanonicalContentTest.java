package com.demcha.examples;

import com.demcha.examples.flagships.MavenBannerPptxExample;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the canonical PPTX comparison the asset gate will be built on.
 *
 * <p>A deck cannot be compared byte for byte. Every entry in the package carries the zip
 * timestamp of the run that wrote it, so two renders of an unchanged document differ. The
 * engine can pin those — both fixed backends take a {@code deterministic(...)} instant —
 * but {@code buildPptx(Path)} routes through the convenience path, which resolves the
 * backend from the provider and gives a caller no way to configure it. Until that seam
 * exists, a gate comparing bytes would either fail forever or drop the decks.</p>
 *
 * <p>Nor can it be compared part for part: three things about a package are decided by the
 * machine rather than by the document, and all three were measured on these decks rather
 * than assumed. See {@link #canonicalise(String, String, byte[])}. So the comparison is
 * defined here as the sorted package parts reduced to what the document decides — every
 * shape, relationship and run of text, with each freeform's path read where it lands on the
 * slide and every image read pixel for pixel bar the one part {@link #UNSTABLE_RASTERS}
 * names.</p>
 *
 * <p>What this class does not do is hold a committed asset against a fresh render. A
 * committed asset is rendered at the released version while a working tree renders the
 * next one, so the two are different documents by construction; and a committed asset is
 * not otherwise guaranteed to be level with the code that produces it. Comparing them
 * needs the display version as an input and a way to refresh whatever has drifted — one
 * job, belonging to the asset gate rather than to a unit test. The comparator is proven
 * here so that the gate arrives with it already working.</p>
 */
class PptxCanonicalContentTest {

    private static final Path COMMITTED = Path.of("..", "assets", "readme", "examples");

    @BeforeAll
    static void generateEveryExample() throws Exception {
        GeneratedCatalogue.generateOnce();
    }

    /**
     * The decks whose preview the repository commits.
     *
     * <p>Which decks those are is a decision, and this list plus {@link #UNPUBLISHED_DECKS}
     * exist so that no deck can arrive without somebody making it. A subset alone is not
     * enough: asserting that the curated decks are among the generated ones lets a new
     * example ship a deck nobody has decided about, which is the same silence the pairing
     * this replaced used to keep. Together the two lists cover the catalogue exactly, so a
     * deck added, removed or renamed lands here first.</p>
     */
    private static final Set<String> CURATED_DECKS = Set.of(
            "business-report.pptx",
            "financial-report.pptx",
            "master-showcase.pptx",
            "maven-banner.pptx",
            "social-card.pptx",
            "twin-output.pptx");

    /** Decks the catalogue renders but the repository deliberately does not commit. */
    private static final Set<String> UNPUBLISHED_DECKS = Set.of(
            "engine-deck.pptx",
            "linkedin-carousel.pptx");

    @Test
    void theTwoDeckListsDoNotOverlap() {
        assertThat(CURATED_DECKS)
                .describedAs("a deck cannot be both published and deliberately unpublished")
                .doesNotContainAnyElementsOf(UNPUBLISHED_DECKS);
    }

    @Test
    void theCommittedDecksAreExactlyTheCuratedSubset() throws Exception {
        Set<String> committed = new TreeSet<>();
        try (var files = Files.list(COMMITTED)) {
            files.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".pptx"))
                    .forEach(committed::add);
        }
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

        assertThat(committed)
                .describedAs("the committed decks and the curated list have to agree: a deck "
                        + "added to the folder without a decision, or removed from it without "
                        + "one, is exactly what this list exists to surface")
                .isEqualTo(new TreeSet<>(CURATED_DECKS));
        Set<String> accountedFor = new TreeSet<>(CURATED_DECKS);
        accountedFor.addAll(UNPUBLISHED_DECKS);
        assertThat(generated.keySet())
                .describedAs("the catalogue and the two lists have to cover each other exactly: a "
                        + "deck listed but not generated is a committed file nothing can refresh, "
                        + "and a deck generated but on neither list is one nobody has decided to "
                        + "publish or to leave out")
                .isEqualTo(accountedFor);
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

    /**
     * The same path drawn in a different box is one shape; a path that moves is not.
     *
     * <p>Dropping the box is the load-bearing half of the comparison, so it is checked in
     * both directions: the three shapes below are the same freeform normalised the two ways
     * two machines normalised it, and then genuinely moved. Absorbing the third as well
     * would leave the guard reporting success on a deck whose artwork had shifted.</p>
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

        assertThat(freeformsInSlideSpace(sameLineOtherBox))
                .describedAs("the same line from (1000,2000) to (1600,2400), normalised against "
                        + "an origin 50 EMU away — the difference two machines produce")
                .isEqualTo(freeformsInSlideSpace(box));
        assertThat(freeformsInSlideSpace(movedLine))
                .describedAs("a line that actually moved by 50 EMU must not be absorbed")
                .isNotEqualTo(freeformsInSlideSpace(box));
    }

    /**
     * The rewrite reaches the markup POI actually writes, not just the shape of it.
     *
     * <p>{@link #theBoxIsAbsorbedButAMoveIsNot} works on markup written here, so it would
     * still pass if POI reordered an attribute and the rewrite quietly stopped matching —
     * leaving a comparison that absorbs nothing and a red gate nobody can read. A deck the
     * repository commits has to come out changed.</p>
     */
    @Test
    void theRewriteReachesRealMarkup() throws Exception {
        String slide = part(COMMITTED.resolve("twin-output.pptx"), "ppt/slides/slide1.xml");

        assertThat(freeformsInSlideSpace(slide))
                .describedAs("no freeform in a deck full of them was rewritten — the markup no "
                        + "longer looks the way the patterns expect")
                .isNotEqualTo(slide);
    }

    /**
     * An image of the same size is not the same image.
     *
     * <p>Reading a raster part by its dimensions would let a logo be swapped, a screenshot
     * be replaced or a watermark be retyped without the comparison noticing, which is a
     * quiet way for a gate to report that nothing changed. Only the one part measured as
     * machine-dependent is read that way; the fixtures below are the same watermark from
     * two machines, so they are the closest two images this repository has, and even they
     * must come out different when compared as images.</p>
     */
    @Test
    void sameDimensionsButDifferentPixelsAreNotEqual() throws Exception {
        BufferedImage windows = decode(WATERMARK_WINDOWS);
        BufferedImage linux = decode(WATERMARK_LINUX);
        assertThat(windows.getWidth()).isEqualTo(linux.getWidth());
        assertThat(windows.getHeight()).isEqualTo(linux.getHeight());

        assertThat(canonicalise("business-report.pptx", WATERMARK_PART, fixture(WATERMARK_WINDOWS)))
                .describedAs("two images of one size are not one image — and the deck this part "
                        + "sits in is not the one the allowlist names")
                .isNotEqualTo(canonicalise("business-report.pptx", WATERMARK_PART,
                        fixture(WATERMARK_LINUX)));
    }

    /**
     * The named part absorbs the difference that was measured, and the fixtures carry it.
     *
     * <p>The two files are {@code ppt/media/image1.png} of the showcase deck rendered on
     * Windows and on the runner: same glyphs in the same places, different coverage along
     * their edges. Pinning them here keeps the allowlist honest — if the difference ever
     * stops being antialiasing the entry stops being justified, and this is where that
     * shows.</p>
     */
    @Test
    void theNamedUnstableRasterAbsorbsTheMeasuredDifference() throws Exception {
        byte[] windows = fixture(WATERMARK_WINDOWS);
        byte[] linux = fixture(WATERMARK_LINUX);
        assertThat(windows)
                .describedAs("the fixtures have to be two renders, not one file twice")
                .isNotEqualTo(linux);

        assertThat(canonicalise(WATERMARK_DECK, WATERMARK_PART, windows))
                .describedAs("the watermark named in the allowlist is the one difference the "
                        + "comparison is allowed to pass over")
                .isEqualTo(canonicalise(WATERMARK_DECK, WATERMARK_PART, linux));
    }

    /**
     * A part written with either line ending is one part.
     *
     * <p>The normalisation this pins is the difference that failed six decks on the runner,
     * and it is the one canonicalisation whose absence shows up nowhere else: every test
     * here runs on one machine, where both sides carry the same line ending and the bug
     * hides.</p>
     */
    @Test
    void theSamePartWithEitherLineEndingIsOnePart() throws Exception {
        String declaration = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
        byte[] unix = (declaration + "\n<p:sld/>").getBytes(StandardCharsets.UTF_8);
        byte[] windows = (declaration + "\r\n<p:sld/>").getBytes(StandardCharsets.UTF_8);

        assertThat(canonicalise(WATERMARK_DECK, "ppt/slides/slide1.xml", windows))
                .describedAs("POI ends the XML declaration with the platform's line separator; "
                        + "that is the machine writing, not the document")
                .isEqualTo(canonicalise(WATERMARK_DECK, "ppt/slides/slide1.xml", unix));
    }

    /**
     * Every allowlisted part names a part that is really there.
     *
     * <p>A misspelled deck or part would match nothing, and an entry that matches nothing
     * exempts nothing — the comparison would quietly go back to reading the watermark by
     * its pixels and fail on a runner for a reason the entry was added to explain.</p>
     */
    @Test
    void everyAllowlistedRasterExists() throws Exception {
        for (String entry : new TreeSet<>(UNSTABLE_RASTERS)) {
            String[] split = entry.split("!", 2);
            assertThat(split).describedAs("%s is not deck!part", entry).hasSize(2);
            for (Path deck : List.of(COMMITTED.resolve(split[0]), generatedDeck(split[0]))) {
                assertThat(deck).describedAs("allowlisted deck %s is missing", split[0]).exists();
                assertThat(partNames(deck))
                        .describedAs("allowlisted part %s is not in %s", split[1], deck)
                        .contains(split[1]);
            }
        }
    }

    private static Path generatedDeck(String name) throws IOException {
        try (var decks = Files.walk(GeneratedCatalogue.ROOT)) {
            return decks.filter(path -> path.getFileName().toString().equals(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "the catalogue no longer renders " + name));
        }
    }

    private static Set<String> partNames(Path pptx) throws IOException {
        Set<String> names = new TreeSet<>();
        try (ZipInputStream zip =
                     new ZipInputStream(new ByteArrayInputStream(Files.readAllBytes(pptx)))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                names.add(entry.getName());
            }
        }
        return names;
    }

    private static final String WATERMARK_WINDOWS = "watermark-windows.png";
    private static final String WATERMARK_LINUX = "watermark-linux.png";
    private static final String WATERMARK_DECK = "master-showcase.pptx";
    private static final String WATERMARK_PART = "ppt/media/image1.png";

    private static byte[] fixture(String name) throws IOException {
        try (var in = PptxCanonicalContentTest.class.getResourceAsStream("/pptx-media/" + name)) {
            assertThat(in).describedAs("missing test fixture %s", name).isNotNull();
            return in.readAllBytes();
        }
    }

    private static BufferedImage decode(String name) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(fixture(name)));
        assertThat(image).describedAs("fixture %s does not decode as an image", name).isNotNull();
        return image;
    }

    private static String part(Path pptx, String name) throws Exception {
        try (ZipInputStream zip =
                     new ZipInputStream(new ByteArrayInputStream(Files.readAllBytes(pptx)))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (entry.getName().equals(name)) {
                    return new String(zip.readAllBytes(), StandardCharsets.UTF_8)
                            .replace("\r\n", "\n");
                }
            }
        }
        throw new IllegalStateException(pptx + " has no part " + name);
    }

    private static String freeform(String xfrm, String from, String to) {
        return "<p:sp><p:nvSpPr><p:cNvPr name=\"Freeform 1\" id=\"1\"/></p:nvSpPr><p:spPr>"
               + "<a:xfrm>" + xfrm + "</a:xfrm><a:custGeom><a:pathLst>"
               + "<a:path h=\"400\" w=\"600\">"
               + "<a:moveTo>" + from + "</a:moveTo><a:lnTo>" + to + "</a:lnTo>"
               + "</a:path></a:pathLst></a:custGeom></p:spPr></p:sp>";
    }

    /** The package's parts and their contents, with everything the machine adds left out. */
    private static String canonicalDigest(Path pptx) throws Exception {
        Map<String, byte[]> parts = new TreeMap<>();
        try (ZipInputStream zip =
                     new ZipInputStream(new ByteArrayInputStream(Files.readAllBytes(pptx)))) {
            String deck = pptx.getFileName().toString();
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (!entry.isDirectory()) {
                    parts.put(entry.getName(),
                            canonicalise(deck, entry.getName(), zip.readAllBytes()));
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
     * Reduces a part to what the document decides, dropping what the machine decides.
     *
     * <p>Three differences were measured between decks written on Windows and the same
     * decks written on the Linux runner, none of them a change to the document:</p>
     *
     * <ul>
     *   <li><b>Line endings.</b> POI ends the XML declaration with the platform's line
     *       separator, so every XML part differs by one byte — sixteen parts of an
     *       otherwise untouched deck.</li>
     *   <li><b>Freeform boxes.</b> The box a freeform declares, and the origin its path
     *       coordinates are measured from, are not stable: one icon came out with every
     *       point shifted by a constant 272 EMU across and 489 down, and its declared
     *       extent smaller by the same amount, so that the path landed on exactly the same
     *       place on the slide. The points are therefore compared where they land, and the
     *       box that only says how they were normalised is dropped. A freeform that
     *       actually moves still moves its points.</li>
     *   <li><b>Rasterised text.</b> One embedded image — a text watermark — differed along
     *       the glyph edges alone: same glyphs, same positions, different antialiasing
     *       coverage. Nothing canonicalises that, so {@link #UNSTABLE_RASTERS} names the
     *       part and it alone is read by its dimensions. Every other image is compared
     *       pixel for pixel.</li>
     * </ul>
     */
    private static byte[] canonicalise(String deck, String name, byte[] content)
            throws IOException {
        if (name.startsWith("ppt/media/")) {
            return image(content, UNSTABLE_RASTERS.contains(deck + "!" + name));
        }
        if (!name.endsWith(".xml") && !name.endsWith(".rels")) {
            return content;
        }
        String text = new String(content, StandardCharsets.UTF_8).replace("\r\n", "\n");
        return freeformsInSlideSpace(text).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * The one raster part a machine is allowed to disagree about, as {@code deck!part}.
     *
     * <p>Naming it costs a line and buys the difference between a comparison that tolerates
     * one measured artefact and one that stops reading images altogether. Anything added
     * here stops being compared by content, so it wants the same measurement behind it that
     * put this entry here.</p>
     */
    private static final Set<String> UNSTABLE_RASTERS =
            Set.of(WATERMARK_DECK + "!" + WATERMARK_PART);

    /**
     * An image's pixels in one colour model, or — for a named unstable part — its size.
     *
     * <p>{@code getRGB} converts whatever the decoder produced into sRGB, so a part is
     * compared by what it looks like rather than by how it was stored. A part that decodes
     * to nothing is vector, and its bytes are its content.</p>
     */
    private static byte[] image(byte[] content, boolean sizeOnly) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
        if (image == null) {
            return content;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        if (sizeOnly) {
            return "%dx%d antialiasing not compared".formatted(width, height)
                    .getBytes(StandardCharsets.UTF_8);
        }
        ByteBuffer pixels = ByteBuffer.allocate(8 + 4 * width * height);
        pixels.putInt(width).putInt(height);
        for (int argb : image.getRGB(0, 0, width, height, null, 0, width)) {
            pixels.putInt(argb);
        }
        return pixels.array();
    }

    private static final Pattern SHAPE = Pattern.compile("<p:sp>.*?</p:sp>", Pattern.DOTALL);
    private static final Pattern OFFSET = Pattern.compile("<a:off x=\"(-?\\d+)\" y=\"(-?\\d+)\"/>");
    private static final Pattern EXTENT = Pattern.compile("<a:ext cx=\"(\\d+)\" cy=\"(\\d+)\"/>");
    private static final Pattern PATH_TAG = Pattern.compile("<a:path ([^>]*)>");
    private static final Pattern POINT = Pattern.compile("<a:pt x=\"(-?\\d+)\" y=\"(-?\\d+)\"/>");
    private static final Pattern BOX_ATTRIBUTE = Pattern.compile(" [wh]=\"\\d+\"");
    private static final Pattern PATH_WIDTH = Pattern.compile("\\bw=\"(\\d+)\"");
    private static final Pattern PATH_HEIGHT = Pattern.compile("\\bh=\"(\\d+)\"");

    /** Reads each freeform's path where it lands on the slide rather than inside its box. */
    private static String freeformsInSlideSpace(String xml) {
        Matcher shapes = SHAPE.matcher(xml);
        StringBuilder out = new StringBuilder();
        while (shapes.find()) {
            shapes.appendReplacement(out, Matcher.quoteReplacement(inSlideSpace(shapes.group())));
        }
        shapes.appendTail(out);
        return out.toString();
    }

    private static String inSlideSpace(String shape) {
        Matcher offset = OFFSET.matcher(shape);
        Matcher extent = EXTENT.matcher(shape);
        Matcher pathTag = PATH_TAG.matcher(shape);
        if (!offset.find() || !extent.find() || !pathTag.find()) {
            return shape;
        }
        long pathWidth = attribute(PATH_WIDTH, pathTag.group(1));
        long pathHeight = attribute(PATH_HEIGHT, pathTag.group(1));
        if (pathWidth <= 0 || pathHeight <= 0) {
            return shape;
        }
        long offsetX = Long.parseLong(offset.group(1));
        long offsetY = Long.parseLong(offset.group(2));
        double scaleX = Long.parseLong(extent.group(1)) / (double) pathWidth;
        double scaleY = Long.parseLong(extent.group(2)) / (double) pathHeight;

        Matcher points = POINT.matcher(shape);
        StringBuilder out = new StringBuilder();
        while (points.find()) {
            long x = offsetX + Math.round(Long.parseLong(points.group(1)) * scaleX);
            long y = offsetY + Math.round(Long.parseLong(points.group(2)) * scaleY);
            points.appendReplacement(out,
                    Matcher.quoteReplacement("<a:pt x=\"%d\" y=\"%d\"/>".formatted(x, y)));
        }
        points.appendTail(out);

        String slideSpace = OFFSET.matcher(out.toString()).replaceFirst("<a:off/>");
        slideSpace = EXTENT.matcher(slideSpace).replaceFirst("<a:ext/>");
        return withoutPathBox(slideSpace);
    }

    private static String withoutPathBox(String shape) {
        Matcher tags = PATH_TAG.matcher(shape);
        StringBuilder out = new StringBuilder();
        while (tags.find()) {
            String rest = BOX_ATTRIBUTE.matcher(" " + tags.group(1)).replaceAll("").trim();
            tags.appendReplacement(out, Matcher.quoteReplacement(
                    rest.isEmpty() ? "<a:path>" : "<a:path " + rest + ">"));
        }
        tags.appendTail(out);
        return out.toString();
    }

    private static long attribute(Pattern attribute, String attributes) {
        Matcher value = attribute.matcher(attributes);
        return value.find() ? Long.parseLong(value.group(1)) : -1;
    }
}
