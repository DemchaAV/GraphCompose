package com.demcha.compose.document.templates.fidelity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * A DOCX the export wrote, changed afterwards in one way a reader would see: a paragraph gone,
 * another font, a page break that was not there, a card's row held to a height its text does not
 * fit, a table replaced by a picture of it.
 *
 * <p>The fidelity gate is there to fail on exactly these, so the corpus run feeds it one
 * document changed each way, through the same editor and the same measurement as the corpus
 * ({@link DocxFidelityCorpusTest}), and requires it to fail. A change is made to the bytes, not
 * to the document the engine composes, so it is the gate that is tested, not the export.</p>
 *
 * <p>A change refuses what it would leave as it was — {@link #unchanged()} aside, which is made
 * to: one finding what it changes by the letters it holds refuses unless exactly one element
 * directly in the body, or in the table, holds them, the row change refuses a row already
 * written as it would write it, and the font change refuses a document whose every
 * {@code w:rFonts} already reads as it would write it. A change that silently changed nothing
 * would leave the gate passing for want of anything to catch. The same DOCX changed the same way gives
 * the same bytes: Word converts outside the build and is measured only on a DOCX whose digest
 * matches the one this run makes ({@link WordConversion}), which a change stamped with the time
 * of the run would never do.</p>
 *
 * @param name      the change, in the file name of the DOCX it makes
 * @param costs     what the change must cost the document, beyond failing the gate
 * @param edit      the change, made to the package's parts
 */
record DocxTamper(String name, Cost costs, Edit edit) {

    /** What a change must cost the document as the editor sets it, beyond any difference at all. */
    enum Cost {
        /** Some difference the gate names: a line moved, lost or gained, a page more or less. */
        ANY,
        /** Lines of the page's that the editor no longer shows as the page does. */
        LOST_LINES,
        /** Another number of pages. */
        PAGES
    }

    /** A change made to a package's parts, by name; it may add a part. */
    @FunctionalInterface
    interface Edit {
        void apply(Map<String, byte[]> parts);
    }

    private static final String DOCUMENT = "word/document.xml";
    private static final String RELATIONSHIPS = "word/_rels/document.xml.rels";
    private static final String CONTENT_TYPES = "[Content_Types].xml";

    /** A white picture of one pixel, which a picture standing in for a table is stretched from. */
    static final byte[] PIXEL = whitePixel();

    private static final Pattern TAG = Pattern.compile("<(/?)(w:[A-Za-z]+)\\b[^>]*?(/?)>");
    private static final Pattern TEXT = Pattern.compile("<w:t(?:\\s[^>]*)?>([^<]*)</w:t>");
    /** A {@code w:rFonts} element, which holds attributes only, written empty or as a pair. */
    private static final Pattern FONTS = Pattern.compile("<w:rFonts\\b[^>]*?/>|<w:rFonts\\b[^>]*>\\s*</w:rFonts>");

    /** The DOCX, changed. */
    byte[] applyTo(byte[] docx) throws IOException {
        Map<String, byte[]> parts = new LinkedHashMap<>();
        Map<String, java.time.LocalDateTime> times = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docx))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                parts.put(entry.getName(), zip.readAllBytes());
                times.put(entry.getName(), entry.getTimeLocal());
            }
        }
        edit.apply(parts);
        // Each part keeps the time it was written with, and a part the change adds takes the
        // body's: the zip would otherwise stamp them with the time of this run. Read and written
        // as local times, so no time zone stands between the export run and the word run.
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, byte[]> part : parts.entrySet()) {
                ZipEntry entry = new ZipEntry(part.getKey());
                entry.setTimeLocal(times.getOrDefault(part.getKey(), times.get(DOCUMENT)));
                zip.putNextEntry(entry);
                zip.write(part.getValue());
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    /** The body's paragraph holding {@code letters}, taken out. */
    static DocxTamper withoutParagraph(String letters) {
        return new DocxTamper("dropped-paragraph", Cost.LOST_LINES, parts -> {
            String xml = text(parts, DOCUMENT);
            int[] paragraph = onlyOne(holding(xml, body(xml), Set.of("w:p"), letters), "paragraph", letters);
            put(parts, DOCUMENT, xml.substring(0, paragraph[0]) + xml.substring(paragraph[1]));
        });
    }

    /**
     * The same DOCX, unchanged but for being unpacked and packed again: converted with the changed
     * ones, it is what shows that their conversion sets the document as the corpus's did, so a
     * difference a change shows is the change's, not the second conversion's.
     */
    static DocxTamper unchanged() {
        return new DocxTamper(UNCHANGED, Cost.ANY, parts -> {
        });
    }

    /** The name of {@link #unchanged()}, the copy that must stand where the corpus's conversion set it. */
    static final String UNCHANGED = "unchanged";

    /**
     * Every {@code w:rFonts} of a {@code word/} part — wherever runs, styles, the document's
     * defaults or list levels name a font — written as {@code font} for each of the four scripts,
     * with no theme font left to outrank it. A {@code w:rFonts} already so is not counted as
     * changed, and a document where every one already is refuses.
     */
    static DocxTamper inFont(String font) {
        String written = "<w:rFonts w:ascii=\"" + font + "\" w:hAnsi=\"" + font + "\" w:eastAsia=\"" + font
                         + "\" w:cs=\"" + font + "\"/>";
        return new DocxTamper("changed-font", Cost.ANY, parts -> {
            int changed = 0;
            for (String name : List.copyOf(parts.keySet())) {
                if (!name.startsWith("word/") || !name.endsWith(".xml")) {
                    continue;
                }
                Matcher fonts = FONTS.matcher(text(parts, name));
                StringBuilder part = new StringBuilder();
                while (fonts.find()) {
                    if (!fonts.group().equals(written)) {
                        changed++;
                    }
                    fonts.appendReplacement(part, Matcher.quoteReplacement(written));
                }
                fonts.appendTail(part);
                put(parts, name, part.toString());
            }
            if (changed == 0) {
                throw new IllegalStateException("the document names no font other than " + font);
            }
        });
    }

    /** A page break before the text of the body's paragraph holding {@code letters}. */
    static DocxTamper withPageBreakBefore(String letters) {
        return new DocxTamper("extra-page-break", Cost.PAGES, parts -> {
            String xml = text(parts, DOCUMENT);
            int[] paragraph = onlyOne(holding(xml, body(xml), Set.of("w:p"), letters), "paragraph", letters);
            List<int[]> runs = children(xml, inside(xml, paragraph), Set.of("w:r"));
            if (runs.isEmpty()) {
                throw new IllegalStateException("the paragraph holding \"" + letters + "\" has no run");
            }
            int at = runs.get(0)[0];
            put(parts, DOCUMENT, xml.substring(0, at) + "<w:r><w:br w:type=\"page\"/></w:r>" + xml.substring(at));
        });
    }

    /**
     * The row of a body table holding {@code letters} held to exactly {@code twips}: what a card
     * whose height was fixed looks like when its text needs more. An editor clips the text below
     * the height away, so the change costs the card's own lines, not only the place of what
     * follows it.
     */
    static DocxTamper withExactRowHeight(String letters, int twips) {
        return new DocxTamper("exact-card-height", Cost.LOST_LINES, parts -> {
            String xml = text(parts, DOCUMENT);
            int[] table = onlyOne(holding(xml, body(xml), Set.of("w:tbl"), letters), "table", letters);
            int[] row = onlyOne(holding(xml, inside(xml, table), Set.of("w:tr"), letters), "row", letters);
            String height = "<w:trHeight w:val=\"" + twips + "\" w:hRule=\"exact\"/>";
            String written = xml.substring(row[0], row[1]);
            List<int[]> properties = children(xml, inside(xml, row), Set.of("w:trPr"));
            if (properties.isEmpty()) {
                int open = written.indexOf('>') + 1;
                written = written.substring(0, open) + "<w:trPr>" + height + "</w:trPr>" + written.substring(open);
            } else {
                int[] trPr = properties.get(0);
                String own = xml.substring(trPr[0], trPr[1]).replaceAll("<w:trHeight\\b[^>]*/>", "");
                own = own.endsWith("/>") ? "<w:trPr>" + height + "</w:trPr>" : own.replaceFirst(">", ">" + height);
                written = xml.substring(row[0], trPr[0]) + own + xml.substring(trPr[1], row[1]);
            }
            if (written.equals(xml.substring(row[0], row[1]))) {
                throw new IllegalStateException("the row holding \"" + letters + "\" is already held to exactly "
                                                + twips + " twips");
            }
            put(parts, DOCUMENT, xml.substring(0, row[0]) + written + xml.substring(row[1]));
        });
    }

    /** The body table holding {@code letters} replaced by a picture in a paragraph of its own. */
    static DocxTamper asPicture(String letters) {
        return new DocxTamper("table-as-picture", Cost.LOST_LINES, parts -> {
            String xml = text(parts, DOCUMENT);
            int[] table = onlyOne(holding(xml, body(xml), Set.of("w:tbl"), letters), "table", letters);
            parts.put("word/media/tampered.png", PIXEL);
            String relationships = text(parts, RELATIONSHIPS);
            if (!relationships.contains("</Relationships>")) {
                throw new IllegalStateException("the body's relationships do not close, so the picture "
                                                + "could not be related to it");
            }
            put(parts, RELATIONSHIPS, relationships.replace("</Relationships>",
                    "<Relationship Id=\"rIdTampered\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/"
                    + "relationships/image\" Target=\"media/tampered.png\"/></Relationships>"));
            String types = text(parts, CONTENT_TYPES);
            if (!types.contains("Extension=\"png\"") && !types.contains("</Types>")) {
                throw new IllegalStateException("the package's content types do not close, so the picture's "
                                                + "could not be declared");
            }
            if (!types.contains("Extension=\"png\"")) {
                put(parts, CONTENT_TYPES, types.replace("</Types>",
                        "<Default Extension=\"png\" ContentType=\"image/png\"/></Types>"));
            }
            long width = 5_000_000;
            long height = 1_500_000;
            String picture = "<w:p><w:r><w:drawing>"
                    + "<wp:inline xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\" "
                    + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" "
                    + "distT=\"0\" distB=\"0\" distL=\"0\" distR=\"0\">"
                    + "<wp:extent cx=\"" + width + "\" cy=\"" + height + "\"/><wp:docPr id=\"9999\" name=\"tampered\"/>"
                    + "<a:graphic xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\">"
                    + "<a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">"
                    + "<pic:pic xmlns:pic=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">"
                    + "<pic:nvPicPr><pic:cNvPr id=\"9999\" name=\"tampered.png\"/><pic:cNvPicPr/></pic:nvPicPr>"
                    + "<pic:blipFill><a:blip r:embed=\"rIdTampered\"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>"
                    + "<pic:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"" + width + "\" cy=\"" + height + "\"/></a:xfrm>"
                    + "<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></pic:spPr></pic:pic>"
                    + "</a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>";
            put(parts, DOCUMENT, xml.substring(0, table[0]) + picture + xml.substring(table[1]));
        });
    }

    /** The letters an element's text runs hold, unescaped as far as the letters looked for need. */
    static String letters(String xml) {
        StringBuilder letters = new StringBuilder();
        Matcher runs = TEXT.matcher(xml);
        while (runs.find()) {
            letters.append(runs.group(1));
        }
        return letters.toString().replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&apos;", "'");
    }

    /** Where the body's content stands. */
    static int[] body(String xml) {
        int open = xml.indexOf("<w:body>");
        int close = xml.lastIndexOf("</w:body>");
        if (open < 0 || close < open) {
            throw new IllegalStateException("the document has no body");
        }
        return new int[]{open + "<w:body>".length(), close};
    }

    /** Where an element's content stands, between its own tags. */
    private static int[] inside(String xml, int[] element) {
        int open = xml.indexOf('>', element[0]) + 1;
        int close = xml.lastIndexOf("</", element[1] - 1);
        return new int[]{open, close};
    }

    /** The children of the given tags directly in {@code span}, each holding {@code letters}. */
    private static List<int[]> holding(String xml, int[] span, Set<String> tags, String letters) {
        return children(xml, span, tags).stream()
                .filter(child -> letters(xml.substring(child[0], child[1])).contains(letters)).toList();
    }

    /**
     * The elements of the given tags standing directly in {@code span}, not in another element
     * there: a card's table holds tables of its own, and their rows are not its rows.
     */
    static List<int[]> children(String xml, int[] span, Set<String> tags) {
        List<int[]> children = new ArrayList<>();
        Matcher tag = TAG.matcher(xml).region(span[0], span[1]);
        int depth = 0;
        int start = -1;
        String open = null;
        while (tag.find()) {
            boolean closing = !tag.group(1).isEmpty();
            boolean empty = !tag.group(3).isEmpty();
            if (depth == 0 && !closing) {
                if (empty) {
                    if (tags.contains(tag.group(2))) {
                        children.add(new int[]{tag.start(), tag.end()});
                    }
                    continue;
                }
                start = tag.start();
                open = tag.group(2);
                depth = 1;
            } else if (!empty) {
                depth += closing ? -1 : 1;
                if (depth == 0 && tags.contains(open)) {
                    children.add(new int[]{start, tag.end()});
                }
            }
        }
        if (depth != 0) {
            throw new IllegalStateException("the document's elements do not close where they open");
        }
        return children;
    }

    private static byte[] whitePixel() {
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(1, 1,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0xFFFFFF);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        try {
            if (!javax.imageio.ImageIO.write(image, "png", png)) {
                throw new IllegalStateException("no PNG writer");
            }
        } catch (IOException unwritable) {
            throw new IllegalStateException(unwritable);
        }
        return png.toByteArray();
    }

    private static int[] onlyOne(List<int[]> found, String what, String letters) {
        if (found.size() != 1) {
            throw new IllegalStateException("the document's body holds " + found.size() + " " + what
                                            + "s holding \"" + letters + "\", not one");
        }
        return found.get(0);
    }

    private static String text(Map<String, byte[]> parts, String name) {
        byte[] part = parts.get(name);
        if (part == null) {
            throw new IllegalStateException("the document has no " + name);
        }
        return new String(part, StandardCharsets.UTF_8);
    }

    private static void put(Map<String, byte[]> parts, String name, String xml) {
        parts.put(name, xml.getBytes(StandardCharsets.UTF_8));
    }
}
