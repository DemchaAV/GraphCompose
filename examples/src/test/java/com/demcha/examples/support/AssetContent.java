package com.demcha.examples.support;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

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
import java.util.TreeSet;
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
 * came out byte-identical under the reduction below. The five that did not are accounted for by
 * {@link #UNSTABLE_PARTS} and {@link #UNSTABLE_SHAPES} or are decks the repository does not
 * commit. So the comparison stays exact: no tolerance, no sampling, and every exemption names the
 * one thing it covers and the two renders it covers it between — never a rule applied to
 * everything of that kind.</p>
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

    /**
     * The shapes a machine is allowed to disagree about, as {@code document!part!shape}.
     *
     * <p>Two, and both in one deck: the box a freeform declares, and the origin its points are
     * measured from, came out differently on the two machines — one icon with every point shifted
     * by a constant 272 EMU across and 489 down and its extent smaller by exactly as much, so the
     * path landed on the same place on the slide to the unit. Comparing all six committed decks
     * shape by shape found these two and nothing else.</p>
     *
     * <p>Like {@link #UNSTABLE_PARTS}, the value is the digest of each machine's version, so the
     * exemption is for the pair of renders rather than for the shape: change either one and it
     * stops being absorbed. Every other shape in every deck is compared as it was written,
     * including the box — which is not decoration, but the centre a {@code rot} turns a shape
     * around and the axis a {@code flipH} mirrors it in.</p>
     */
    public static final Map<String, Set<String>> UNSTABLE_SHAPES = Map.of(
            "twin-output.pptx!ppt/slides/slide1.xml!Freeform 41",
            Set.of("18dacede01e07a4408f19b8f2a6fc072887e6d3f5796064db4caf1b933a0f741",
                    "4d126750bda49ababc68b4020984bc1295d7e94b02dd2629bc3d971d05346656"),
            "twin-output.pptx!ppt/slides/slide1.xml!Freeform 51",
            Set.of("c59aeb5cc0676a9dc35e01aff745d0b2d66500ab7dfca4afb24d4f4a60c15262",
                    "33904bc4a82c84c86384832ca4b6c9cad738bcca558d756a43d25b9676e1b65e"));

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
     * separator after each XML declaration, the creation stamp in {@code docProps}, and two
     * freeform shapes in one deck. The first two are dropped wherever they appear; the third is
     * dropped only for those two shapes — see {@link #UNSTABLE_SHAPES}.</p>
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
        return knownShapesCollapsed(document + "!" + name, text)
                .getBytes(StandardCharsets.UTF_8);
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

    /**
     * A PDF read as the document it draws rather than as the bytes it was written to.
     *
     * <p>For a PDF whose content carries a clock — {@code pdf-chrome.pdf} prints the
     * {@code {date}} header token, which the engine resolves from {@code LocalDate.now()} — the
     * byte comparison is red on every day but the one the file was committed on. The date is
     * inside a compressed stream, so nothing can be masked without decompressing first.</p>
     *
     * <p>So it is decompressed: page count, then each page's operators with any ISO date
     * replaced, then the metadata the document declares and the outline it builds. That covers
     * what {@code pdf-chrome.pdf} exists to demonstrate — a watermark drawn behind the content,
     * a header and footer, an information dictionary, bookmarks — and it stops covering only the
     * eight characters that change by themselves. Excluding the file instead would have left all
     * of that unchecked, which is a larger hole than the one being closed.</p>
     *
     * <p>This is the second-best fix. The first is a render date the caller can pin, the way both
     * fixed backends already accept a {@code deterministic(...)} instant; with that, this method
     * and its caller go away and the file rejoins the ordinary comparison.</p>
     *
     * @param pdf the file to read
     * @return a hex SHA-256 over what the document draws and declares
     * @throws IOException if the file cannot be read or parsed
     */
    public static String clockIndependentPdfDigest(Path pdf) throws IOException {
        MessageDigest digest = sha256();
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            digest.update("pages=%d".formatted(document.getNumberOfPages())
                    .getBytes(StandardCharsets.UTF_8));
            for (PDPage page : document.getPages()) {
                digest.update(withoutDates(page.getContents().readAllBytes()));
            }
            PDDocumentInformation information = document.getDocumentInformation();
            for (String key : new TreeSet<>(information.getMetadataKeys())) {
                digest.update(key.getBytes(StandardCharsets.UTF_8));
                digest.update(withoutDates(
                        String.valueOf(information.getCustomMetadataValue(key))
                                .getBytes(StandardCharsets.UTF_8)));
            }
            digest.update(outline(document).getBytes(StandardCharsets.UTF_8));
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /** Every bookmark title, in order, with the page each one lands on. */
    private static String outline(PDDocument document) throws IOException {
        PDDocumentOutline root = document.getDocumentCatalog().getDocumentOutline();
        if (root == null) {
            return "no outline";
        }
        StringBuilder bookmarks = new StringBuilder();
        for (PDOutlineItem item : root.children()) {
            bookmarks.append(item.getTitle()).append('@')
                    .append(document.getPages().indexOf(item.findDestinationPage(document)))
                    .append(';');
        }
        return bookmarks.toString();
    }

    private static byte[] withoutDates(byte[] content) {
        return ISO_DATE.matcher(new String(content, StandardCharsets.ISO_8859_1))
                .replaceAll("<date>")
                .getBytes(StandardCharsets.ISO_8859_1);
    }

    private static final Pattern ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

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
    private static final Pattern SHAPE_NAME = Pattern.compile("<p:cNvPr name=\"([^\"]+)\"");

    /**
     * Replaces the shapes named in {@link #UNSTABLE_SHAPES} with a token, and nothing else.
     *
     * <p>An earlier version of this rewrote every freeform: it read the path in slide
     * coordinates and dropped the box those coordinates were measured against. That absorbed the
     * measured difference and a good deal more — the box a shape declares is also the centre a
     * {@code rot} turns it around and the axis a {@code flipH} mirrors it in, and five of the six
     * committed decks carry one or the other. Two shapes drawn around different centres would
     * have compared equal while PowerPoint drew them differently.</p>
     *
     * <p>What was actually measured is two shapes in one deck, so two shapes in one deck are what
     * is exempted — by the digest of each machine's version of them, the way an unstable image
     * is. Every other shape is compared as it was written.</p>
     */
    static String knownShapesCollapsed(String part, String xml) {
        if (UNSTABLE_SHAPES.keySet().stream().noneMatch(key -> key.startsWith(part + "!"))) {
            return xml;
        }
        Matcher shapes = SHAPE.matcher(xml);
        StringBuilder out = new StringBuilder();
        while (shapes.find()) {
            shapes.appendReplacement(out, Matcher.quoteReplacement(collapse(part, shapes.group())));
        }
        shapes.appendTail(out);
        return out.toString();
    }

    private static String collapse(String part, String shape) {
        Matcher name = SHAPE_NAME.matcher(shape);
        if (!name.find()) {
            return shape;
        }
        String key = part + "!" + name.group(1);
        String digest = HexFormat.of().formatHex(
                sha256().digest(shape.getBytes(StandardCharsets.UTF_8)));
        return UNSTABLE_SHAPES.getOrDefault(key, Set.of()).contains(digest)
                ? "<a known render of " + key + "/>"
                : shape;
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
