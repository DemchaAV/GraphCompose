package com.demcha.examples;

import com.demcha.examples.flagships.MavenBannerPptxExample;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

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
 * <p>Nor can it be compared part for part: three things about a package are decided by the
 * machine rather than by the document, and all three were measured on these decks rather
 * than assumed. See {@link #canonicalise(String, byte[])}. So the comparison is defined
 * here as the sorted package parts reduced to what the document decides — every shape,
 * relationship and run of text, with each freeform's path read where it lands on the slide
 * and each image read by its dimensions. This is the comparator the asset gate will use,
 * exercised now on the decks the repository commits, so the gate arrives with its
 * comparison already proven instead of discovering the problem when it first runs red.</p>
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
                        + "comparison ignores everything the machine decides, so this is a content "
                        + "change: re-render the asset, or revert whatever changed the example")
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
     *   <li><b>Rasterised text.</b> An embedded image of a text watermark differed along
     *       the glyph edges alone — same glyphs, same positions, different antialiasing
     *       coverage. Nothing canonicalises that, so an image is compared by its
     *       dimensions and pixel layout instead of its pixels; an image that goes missing,
     *       changes size or changes format still fails.</li>
     * </ul>
     */
    private static byte[] canonicalise(String name, byte[] content) throws IOException {
        if (name.startsWith("ppt/media/")) {
            return imageShape(content);
        }
        if (!name.endsWith(".xml") && !name.endsWith(".rels")) {
            return content;
        }
        String text = new String(content, StandardCharsets.UTF_8).replace("\r\n", "\n");
        return freeformsInSlideSpace(text).getBytes(StandardCharsets.UTF_8);
    }

    /** An image's dimensions and pixel layout — a vector part is left as it is. */
    private static byte[] imageShape(byte[] content) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
        if (image == null) {
            return content;
        }
        return "%dx%d type=%d".formatted(image.getWidth(), image.getHeight(), image.getType())
                .getBytes(StandardCharsets.UTF_8);
    }

    private static final Pattern SHAPE = Pattern.compile("<p:sp>.*?</p:sp>", Pattern.DOTALL);
    private static final Pattern OFFSET = Pattern.compile("<a:off x=\"(-?\\d+)\" y=\"(-?\\d+)\"/>");
    private static final Pattern EXTENT = Pattern.compile("<a:ext cx=\"(\\d+)\" cy=\"(\\d+)\"/>");
    private static final Pattern PATH_TAG = Pattern.compile("<a:path ([^>]*)>");
    private static final Pattern POINT = Pattern.compile("<a:pt x=\"(-?\\d+)\" y=\"(-?\\d+)\"/>");
    private static final Pattern BOX_ATTRIBUTE = Pattern.compile(" [wh]=\"\\d+\"");

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
        long pathWidth = attribute(pathTag.group(1), 'w');
        long pathHeight = attribute(pathTag.group(1), 'h');
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

    private static long attribute(String attributes, char name) {
        Matcher value = Pattern.compile(name + "=\"(\\d+)\"").matcher(attributes);
        return value.find() ? Long.parseLong(value.group(1)) : -1;
    }
}
