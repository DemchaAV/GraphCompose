package com.demcha.examples.support;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

/**
 * What a rendered document can be compared by, once the machine is left out of it.
 *
 * <p>Two renders of an unchanged document are not the same file. A PDF carries a clock-seeded
 * {@code /ID}; an OOXML package carries a zip timestamp on every entry and a creation stamp in
 * its properties; POI ends each XML declaration with the platform's line separator. None of
 * that is the document, and a comparison that reads it reports a change on every run.</p>
 *
 * <p>Everything this drops was measured on this repository's own catalogue rather than assumed,
 * by rendering it on Windows and on the Linux runner at the same version: of 104 documents, 99
 * came out byte-identical under the reduction below. The five that did not are named in
 * {@link #UNSTABLE_PARTS} or are decks the repository does not commit — see there for what each
 * one was. So the comparison stays exact: no tolerance, no sampling, and every exemption is a
 * line somebody had to write.</p>
 */
public final class AssetContent {

    private AssetContent() {
    }

    /**
     * The parts a machine is allowed to disagree about, and the renders it may disagree between.
     *
     * <p>One entry, and it earns itself: the showcase deck embeds an image of a text watermark,
     * and the same glyphs in the same places came out with different antialiasing coverage along
     * their edges on the two machines. Averaging it away is not available — 4-pixel blocks still
     * differ by 138 of 255.</p>
     *
     * <p>What is exempted is not the part but the pair of renders. Each value is the pixel digest
     * of one machine's version of that part; those two collapse to one token, and anything else —
     * a different watermark, a swapped logo, an empty image of the same size — keeps its own
     * digest and fails the comparison. Adding a key here without its digests would exempt the
     * part itself, which is the hole this shape exists to close.</p>
     */
    public static final Map<String, Set<String>> UNSTABLE_PARTS = Map.of(
            "master-showcase.pptx!ppt/media/image1.png",
            Set.of("1f629c6a16dd2d5c18ead1594788ce04e0341360f57af68421b428d56cfb03a8",
                    "9b4a3b3d0dcae564393372bccb71430b0367b6c189e36bfff664a4a67f515225"));

    /** Extensions this can reduce; anything else is compared as the bytes it is. */
    private static final Set<String> PACKAGES = Set.of(".pptx", ".docx");

    private static final Pattern PDF_ID =
            Pattern.compile("/ID \\[<[0-9A-Fa-f]+> <[0-9A-Fa-f]+>\\]");
    /**
     * The one element in a package's properties that records when it was written.
     *
     * <p>Named rather than matched by shape: a pattern for "any ISO instant under
     * {@code docProps}" would also drop a date somebody meant, and a document whose custom
     * property is a date has every right to be compared by it.</p>
     */
    private static final Pattern CREATION_STAMP =
            Pattern.compile("(<dcterms:created[^>]*>)[^<]*(</dcterms:created>)");

    /**
     * A stable identity for a rendered document.
     *
     * <p>Two documents with the same digest are the same document; two with different digests
     * differ in something an author wrote. The name matters — it selects the reduction, and for
     * a package it also selects any {@link #UNSTABLE_PARTS} entry — so pass the name the file is
     * committed under, not a temporary one.</p>
     *
     * @param document the file to read
     * @return a hex SHA-256 over the document's content
     * @throws IOException if the file cannot be read
     */
    public static String digestOf(Path document) throws IOException {
        String name = document.getFileName().toString();
        byte[] bytes = Files.readAllBytes(document);
        if (PACKAGES.stream().anyMatch(name::endsWith)) {
            return digest(packageParts(name, bytes));
        }
        if (name.endsWith(".pdf")) {
            return digest(Map.of(name, withoutPdfId(bytes)));
        }
        return digest(Map.of(name, bytes));
    }

    /** The zip's parts, each reduced, with the entry order and timestamps dropped. */
    private static Map<String, byte[]> packageParts(String document, byte[] bytes)
            throws IOException {
        Map<String, byte[]> parts = new TreeMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (!entry.isDirectory()) {
                    parts.put(entry.getName(),
                            part(document, entry.getName(), zip.readAllBytes()));
                }
            }
        }
        return parts;
    }

    /**
     * Reduces one part of a package to what the document decides.
     *
     * <p>Three differences were measured between a package written on Windows and the same
     * package written on the runner, none of them a change to the document: the platform's line
     * separator after each XML declaration, the creation stamp in {@code docProps}, and the box a
     * freeform's path is normalised against. The first two are dropped; the third is why the
     * points are read where they land — see {@link #freeformsInSlideSpace}.</p>
     */
    static byte[] part(String document, String name, byte[] content) throws IOException {
        if (name.startsWith("ppt/media/") || name.startsWith("word/media/")) {
            return raster(document + "!" + name, content);
        }
        if (!name.endsWith(".xml") && !name.endsWith(".rels")) {
            return content;
        }
        String text = new String(content, StandardCharsets.UTF_8).replace("\r\n", "\n");
        if (name.equals("docProps/core.xml")) {
            text = CREATION_STAMP.matcher(text).replaceAll("$1$2");
        }
        return freeformsInSlideSpace(text).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * A raster part read by its pixels, unless it is one of a pair a machine writes differently.
     *
     * <p>An exempted part is not waved through: its pixels still decide, and only the two renders
     * written down in {@link #UNSTABLE_PARTS} collapse onto one token. Anything else keeps the
     * digest of what it actually is, so an image swapped for another of the same size differs
     * from both the token and the other machine's render.</p>
     */
    private static byte[] raster(String key, byte[] content) throws IOException {
        String pixels = pixelDigest(content);
        if (pixels == null) {
            return content;
        }
        if (UNSTABLE_PARTS.getOrDefault(key, Set.of()).contains(pixels)) {
            return ("a known render of " + key).getBytes(StandardCharsets.UTF_8);
        }
        return pixels.getBytes(StandardCharsets.UTF_8);
    }

    /** A PDF with the one thing in it that the clock writes taken out. */
    static byte[] withoutPdfId(byte[] content) {
        String text = new String(content, StandardCharsets.ISO_8859_1);
        return PDF_ID.matcher(text).replaceAll("/ID []").getBytes(StandardCharsets.ISO_8859_1);
    }

    /**
     * A digest of an image's size and pixels in one colour model, or {@code null} if it is vector.
     *
     * <p>{@code getRGB} converts whatever the decoder produced into sRGB, so an image is compared
     * by what it looks like rather than by how it was stored. The pixels are read a row at a time
     * and folded into the digest rather than buffered: an embedded screenshot runs to megapixels,
     * and holding two of those as byte arrays to compare them is a waste of a test's heap.</p>
     *
     * @param content the bytes of the part
     * @return a hex SHA-256 over the image, or {@code null} when nothing decodes it
     * @throws IOException if the bytes cannot be read
     */
    public static String pixelDigest(byte[] content) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
        if (image == null) {
            return null;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        ByteBuffer row = ByteBuffer.allocate(4 * width);
        MessageDigest digest = sha256();
        digest.update("%dx%d".formatted(width, height).getBytes(StandardCharsets.UTF_8));
        int[] pixels = new int[width];
        for (int y = 0; y < height; y++) {
            image.getRGB(0, y, width, 1, pixels, 0, width);
            row.clear();
            for (int argb : pixels) {
                row.putInt(argb);
            }
            digest.update(row.array());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static final Pattern SHAPE = Pattern.compile("<p:sp>.*?</p:sp>", Pattern.DOTALL);
    private static final Pattern OFFSET =
            Pattern.compile("<a:off x=\"(-?\\d+)\" y=\"(-?\\d+)\"/>");
    private static final Pattern EXTENT = Pattern.compile("<a:ext cx=\"(\\d+)\" cy=\"(\\d+)\"/>");
    private static final Pattern PATH_TAG = Pattern.compile("<a:path ([^>]*)>");
    private static final Pattern POINT = Pattern.compile("<a:pt x=\"(-?\\d+)\" y=\"(-?\\d+)\"/>");
    private static final Pattern BOX_ATTRIBUTE = Pattern.compile(" [wh]=\"\\d+\"");
    private static final Pattern PATH_WIDTH = Pattern.compile("\\bw=\"(\\d+)\"");
    private static final Pattern PATH_HEIGHT = Pattern.compile("\\bh=\"(\\d+)\"");

    /**
     * Reads each freeform's path where it lands on the slide rather than inside its box.
     *
     * <p>The box a freeform declares, and the origin its coordinates are measured from, are not
     * stable: one icon came out with every point shifted by a constant 272 EMU across and 489
     * down and its declared extent smaller by exactly as much, so that the path landed on the
     * same place on the slide to the unit. Only the normalisation moved. A freeform that
     * actually moves still moves its points.</p>
     */
    static String freeformsInSlideSpace(String xml) {
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

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required of every JVM", impossible);
        }
    }

    private static String digest(Map<String, byte[]> parts) {
        MessageDigest digest = sha256();
        new TreeMap<>(parts).forEach((name, content) -> {
            digest.update(name.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(content);
            digest.update((byte) 0);
        });
        return HexFormat.of().formatHex(digest.digest());
    }
}
