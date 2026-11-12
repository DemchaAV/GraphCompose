package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.InlineHighlightRun;
import com.demcha.compose.document.node.InlineRun;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.node.ListItem;
import com.demcha.compose.document.node.ListNode;
import com.demcha.compose.document.node.PageFieldNode;
import com.demcha.compose.document.node.PageReferenceNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.font.DefaultFonts;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.font.FontName;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;
import org.apache.poi.openxml4j.opc.TargetMode;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Puts the document's own faces inside the package, so it reads in the font it was
 * written in, and still does after an editor saves it.
 *
 * <p>A face was named and never shipped: the export declared {@code Lato} on its runs and
 * embedded nothing, so on a machine without Lato installed — which is most of them — Word
 * substituted another face. A substituted face has different glyph widths, so every line
 * breaks somewhere else and no amount of correct geometry above it survives. Measured on
 * this machine, where Lato is not installed, that is exactly what happened.</p>
 *
 * <p>Only a face the document may be edited in is written. An OpenType face states its
 * terms in {@code OS/2}, and the format takes them seriously enough to distinguish
 * "embeddable for reading and printing" from "embeddable in a document someone will type
 * into" — this writes the second and refuses the first, with a line in the log naming the
 * family rather than a silent omission.</p>
 *
 * <p>One face set per name an editor reads. A document can ask two families to travel under one
 * name — {@link FontFamilyDefinition#wordFamily()} defaults to the name a family is registered
 * under and can be set apart from it — and an editor then draws both families' text from one of
 * them: measured in
 * Word 16 and LibreOffice 24.2, a document declaring two face sets under one name, and one
 * declaring two whose names differ only in case, had both paragraphs set in the face declared
 * first, the second program ignored. So the family that comes to a name already shipped under is
 * given up here instead, and named ({@link #refuseForASharedName}).</p>
 *
 * <p>The standard PDF faces are never written, and not because of their terms: they are
 * names, not files. Nothing ships Helvetica, and a reader opening the document has Word's
 * own substitution for it, which is what a PDF viewer does with the same document.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocxFontTable {

    private static final Logger LOG = LoggerFactory.getLogger(DocxFontTable.class);

    private static final String FONT_TABLE_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.fontTable+xml";
    private static final String FONT_TABLE_RELATION =
            "http://schemas.openxmlformats.org/officeDocument/2006/relationships/fontTable";
    private static final String FONT_PART_TYPE =
            "application/vnd.openxmlformats-officedocument.obfuscatedFont";
    private static final String FONT_RELATION =
            "http://schemas.openxmlformats.org/officeDocument/2006/relationships/font";

    private static final String W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final String R_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";

    private DocxFontTable() {
    }

    /**
     * Writes a font table for every family the document uses that can be embedded, and, when
     * it ships at least one face, asks in the settings for embedded fonts to be saved back.
     *
     * @param document  the document being written
     * @param graph     the document's own tree, read for the faces it names
     * @param zones     the content of every section's page zones, which the header and footer parts
     *                  are written from, read for the faces it names
     * @param bands     the style each header and footer band of text sets its text in
     * @param custom    families the session registered, which win over the bundled ones
     * @param report    where a face that may not travel is recorded
     * @return the line an editor sets each shipped face in
     * @throws IOException if a part cannot be written
     */
    static ShippedLines write(XWPFDocument document,
                              DocumentGraph graph,
                              List<DocumentNode> zones,
                              List<DocumentTextStyle> bands,
                              Collection<FontFamilyDefinition> custom,
                              DocxExportReport.Builder report) throws IOException {
        Map<FontName, FontFamilyDefinition> families = familiesByName(custom);
        List<Embedded> embedded = resolve(graph, zones, bands, families, report);
        ShippedLines lines = new ShippedLines(families, embedded);
        if (embedded.isEmpty()) {
            return lines;
        }

        OPCPackage pkg = document.getPackage();
        PackagePartName tableName = partName("/word/fontTable.xml");
        PackagePart table = pkg.createPart(tableName, FONT_TABLE_TYPE);
        document.getPackagePart().addRelationship(tableName, TargetMode.INTERNAL, FONT_TABLE_RELATION);

        int index = 0;
        for (Embedded family : embedded) {
            for (Face face : family.faces()) {
                PackagePartName facePart = partName("/word/fonts/font" + (++index) + ".odttf");
                PackagePart part = pkg.createPart(facePart, FONT_PART_TYPE);
                try (OutputStream out = part.getOutputStream()) {
                    out.write(face.bytes());
                }
                face.relationshipId(table.addRelationship(facePart, TargetMode.INTERNAL, FONT_RELATION)
                        .getId());
            }
        }

        try (OutputStream out = table.getOutputStream()) {
            out.write(toXml(embedded).getBytes(StandardCharsets.UTF_8));
        }
        keepTheFacesWhenTheDocumentIsSaved(document);
        return lines;
    }

    /**
     * Asks the editor that saves the document to write its faces back into it.
     *
     * <p>In LibreOffice a face in the package travels only as far as the first save. LibreOffice
     * 24.2 and 26.8 read the faces a document ships, but save them again only when the settings
     * ask for embedded fonts. Without that request their save leaves every face behind, and the
     * document reopens in substitutes. Word 16 turned the request on by itself when it opened an
     * export without it, and kept the faces through a save.</p>
     *
     * <p>The request is the whole of it. {@code w:saveSubsetFonts} is never written: with it,
     * Word saved only the characters typed so far, and characters typed after the document was
     * reopened, on a machine without the family, were set in another face.</p>
     *
     * @param document the document whose faces were just written
     */
    private static void keepTheFacesWhenTheDocumentIsSaved(XWPFDocument document) {
        var settings = document.getSettings().getCTSettings();
        if (!settings.isSetEmbedTrueTypeFonts()) {
            settings.addNewEmbedTrueTypeFonts();
        }
    }

    /**
     * The line an editor sets each face the document ships in where nothing fixes the line's
     * height ({@link DocxNaturalLine}). A face it does not ship — a standard PDF name, one that
     * may not travel — is set in whatever the reader's machine substitutes, so it has none.
     *
     * <p>Kept under the declared name exactly as the runs spell it. One family is written per name
     * ({@link #resolve}), so no two faces can meet over one of these keys.</p>
     *
     * <p>A family given up for a name another one declares keeps no line of its own here, by
     * either route: where the two spellings are the same its runs read the key the holder filled,
     * and where they differ in case the key their spelling makes is not in the map at all. The two
     * are not the same answer — a line read from the holder is one the writer can hold a paragraph
     * to, and no line at all is not — so which of them a document gets turns on how its author
     * spelled a name that an editor reads one way either way. Recorded as a limit in the backend
     * capability details, and pinned by a test per route.</p>
     */
    static final class ShippedLines {
        /** No face shipped: an export written without a font table. */
        static final ShippedLines NONE = new ShippedLines(Map.of(), List.of());

        private final Map<FontName, FontFamilyDefinition> families;
        private final Map<String, DocxNaturalLine> lines = new java.util.HashMap<>();
        /** The file of each face with a line whose character map is not read yet. */
        private final Map<String, byte[]> files = new java.util.HashMap<>();
        /** Each face's character map, read the first time a text is asked of it; empty where unreadable. */
        private final Map<String, java.util.Optional<org.apache.fontbox.ttf.CmapLookup>> characters =
                new java.util.HashMap<>();

        private ShippedLines(Map<FontName, FontFamilyDefinition> families, List<Embedded> embedded) {
            this.families = families;
            for (Embedded family : embedded) {
                for (Face face : family.faces()) {
                    if (face.line() != null) {
                        lines.put(family.wordFamily() + '\n' + face.element(), face.line());
                        files.put(family.wordFamily() + '\n' + face.element(), face.font());
                    }
                }
            }
        }

        /**
         * Whether a face the document ships, with a line, holds a glyph of its own for every letter
         * of a text, so that an editor sets the text in it, not in a face it falls back to.
         *
         * @param fontName the face a style names
         * @param slot     which of its family's faces, as {@link #of(FontName, String)}
         * @param text     the letters
         * @return whether it does; false where the document does not ship that face, it has no line,
         *         or its Unicode character map cannot be read
         */
        boolean holds(FontName fontName, String slot, String text) {
            String wordFamily = wordFamilyOf(fontName);
            return wordFamily != null && holds(wordFamily, slot, text);
        }

        /**
         * The same, by the family a run names in Word.
         *
         * @param wordFamily the family's name as the font table writes it
         * @param slot       which of its faces, as {@link #of(FontName, String)}
         * @param text       the letters
         * @return whether it does, as {@link #holds(FontName, String, String)}
         */
        boolean holds(String wordFamily, String slot, String text) {
            String face = wordFamily + '\n' + slot;
            java.util.Optional<org.apache.fontbox.ttf.CmapLookup> map = characters.get(face);
            if (map == null) {
                byte[] file = files.remove(face);
                if (file == null) {
                    return false;
                }
                map = characterMapOf(file);
                characters.put(face, map);
            }
            org.apache.fontbox.ttf.CmapLookup read = map.orElse(null);
            return read != null && text.codePoints().allMatch(letter -> read.getGlyphId(letter) > 0);
        }

        /** A face's Unicode character map; empty where it holds none, or cannot be read. */
        private static java.util.Optional<org.apache.fontbox.ttf.CmapLookup> characterMapOf(byte[] file) {
            try (org.apache.fontbox.ttf.TrueTypeFont font = new org.apache.fontbox.ttf.TTFParser()
                    .parse(new org.apache.pdfbox.io.RandomAccessReadBuffer(file))) {
                return java.util.Optional.ofNullable(font.getUnicodeCmapLookup(true));
            } catch (IOException | RuntimeException unreadable) {
                return java.util.Optional.empty();
            }
        }

        /**
         * The line a face is set in.
         *
         * @param fontName the face a style names
         * @param slot     which of its family's faces: {@code embedRegular}, {@code embedBold},
         *                 {@code embedItalic} or {@code embedBoldItalic}
         * @return its line, or null where the document does not ship that face
         */
        DocxNaturalLine of(FontName fontName, String slot) {
            String wordFamily = wordFamilyOf(fontName);
            return wordFamily == null ? null : of(wordFamily, slot);
        }

        /** The family a style's face is written as in Word, or null where none is known. */
        private String wordFamilyOf(FontName fontName) {
            FontFamilyDefinition family = fontName == null ? null : families.get(FontLibrary.resolveFamily(fontName));
            return family == null ? null : family.wordFamily();
        }

        /**
         * The line a face is set in, by the family a run names in Word.
         *
         * @param wordFamily the family's name as the font table writes it
         * @param slot       which of its faces, as {@link #of(FontName, String)}
         * @return its line, or null where the document does not ship that face
         */
        DocxNaturalLine of(String wordFamily, String slot) {
            return lines.get(wordFamily + '\n' + slot);
        }
    }

    /**
     * Every family an export can name, by the logical name a style asks for.
     *
     * <p>A family the session registered wins over a bundled one of the same name: it was
     * registered to be used, and the document was laid out with it.</p>
     *
     * @param custom families the session registered, possibly null
     * @return the families, bundled ones first
     */
    static Map<FontName, FontFamilyDefinition> familiesByName(Collection<FontFamilyDefinition> custom) {
        Map<FontName, FontFamilyDefinition> families = new LinkedHashMap<>();
        for (FontFamilyDefinition family : DefaultFonts.bundledFamilies()) {
            families.put(family.name(), family);
        }
        if (custom != null) {
            for (FontFamilyDefinition family : custom) {
                families.put(family.name(), family);
            }
        }
        return families;
    }

    /**
     * One family that will be written, and the faces of it that may be.
     *
     * @param registered the name the family was registered under, which a refusal crossing two
     *                   families needs: the two agree on {@code wordFamily} and this is what
     *                   tells them apart
     * @param wordFamily the name the font table declares the family under
     * @param faces      the faces of it the document ships
     */
    private record Embedded(FontName registered, String wordFamily, List<Face> faces) {
    }

    /**
     * One face of a family: which slot it fills, its file and the bytes the part stores, how the
     * part refers to it, and the line an editor sets it in.
     */
    private static final class Face {
        private final String element;
        private final byte[] font;
        private final byte[] bytes;
        private final String fontKey;
        private final DocxNaturalLine line;
        private String relationshipId;

        Face(String element, byte[] font, byte[] bytes, String fontKey, DocxNaturalLine line) {
            this.element = element;
            this.font = font;
            this.bytes = bytes;
            this.fontKey = fontKey;
            this.line = line;
        }

        String element() {
            return element;
        }

        /** The face's own file, as read. */
        byte[] font() {
            return font;
        }

        /** The file as the part stores it, obfuscated. */
        byte[] bytes() {
            return bytes;
        }

        DocxNaturalLine line() {
            return line;
        }

        void relationshipId(String id) {
            this.relationshipId = id;
        }
    }

    /**
     * Reads every face the document could be set in, and keeps the ones it may ship.
     *
     * <p>A face name resolves to its family first: a style naming {@code Helvetica-Bold}
     * asks for the Helvetica family, which is a name rather than a file and ships
     * nothing.</p>
     *
     * <p>One face set per name the editor reads. Two families can declare one name — the same
     * name, or one differing only in case — because the families are kept by the name they were
     * registered under, while the name they declare defaults to that one and may be set apart
     * from it. One of the two set apart is enough, which is what a family declaring a bundled
     * family's name does. The first to ship a
     * face keeps the name and the rest are given up ({@link #refuseForASharedName}). Which is
     * first is decided by the document: the faces are read in the order the document's tree names
     * them, then the page zones', then the header and footer bands', so a family registered
     * earlier and set in text later gives way to one set in text before it, and a family set only
     * in a page zone or a band gives way to any family the body is set in. That is the opposite of
     * the PPTX backend, where a registered family is carried before anything is drawn and so holds
     * the name whether its text is on a slide or not.</p>
     *
     * <p>A family whose every face is given up — one that may not travel, one that cannot be read
     * — writes no {@code w:font} and so keeps no name: the name is taken where a face is
     * actually shipped under it, not where one was asked for.</p>
     */
    private static List<Embedded> resolve(DocumentGraph graph,
                                          List<DocumentNode> zones,
                                          List<DocumentTextStyle> bands,
                                          Map<FontName, FontFamilyDefinition> families,
                                          DocxExportReport.Builder report) {
        Map<FontName, Set<Slot>> used = new LinkedHashMap<>();
        for (DocumentNode root : graph.roots()) {
            collectFonts(root, used);
        }
        for (DocumentNode zone : zones) {
            collectFonts(zone, used);
        }
        bands.forEach(style -> add(style, used));
        if (used.isEmpty()) {
            return List.of();
        }

        // Read by family and not by the name a style used: a style can name a face of a family
        // and the family itself, and both ask for that one family. Folded first, the family is
        // read once with every slot asked of it anywhere, so it writes one element holding them
        // all; read per name it wrote one element per name, which is the collision below with a
        // family on both sides of it.
        Map<FontName, Set<Slot>> byFamily = new LinkedHashMap<>();
        used.forEach((named, slots) -> byFamily
                .computeIfAbsent(FontLibrary.resolveFamily(named), key -> java.util.EnumSet.noneOf(Slot.class))
                .addAll(slots));

        List<Embedded> embedded = new ArrayList<>();
        for (Map.Entry<FontName, Set<Slot>> entry : byFamily.entrySet()) {
            FontFamilyDefinition family = families.get(entry.getKey());
            if (family == null || family.fontSourceSet().isEmpty()) {
                // A standard-14 name, or one nothing registered: there is no file to ship.
                continue;
            }
            Embedded holder = declaring(embedded, family.wordFamily());
            if (holder != null) {
                refuseForASharedName(family, holder, report);
                continue;
            }
            List<Face> faces = facesOf(family, entry.getValue(), report);
            if (!faces.isEmpty()) {
                embedded.add(new Embedded(family.name(), family.wordFamily(), faces));
            }
        }
        return embedded;
    }

    /**
     * The family already written under that name, or null where the name is free.
     *
     * <p>A list walked with {@link String#equalsIgnoreCase}, not a map on a case-folded key:
     * the comparison has to be case-insensitive, because both editors were measured to draw a
     * run from a face set declared under the name in another case, and it has to fold the same
     * way wherever the export runs. {@code FontName}'s own equality, which this would otherwise
     * reuse, does not: it normalizes with {@code toLowerCase()} and so follows the default locale,
     * which would fold two names together on one machine and not on another. A document declares
     * a handful of families, so the walk costs nothing worth saving.</p>
     */
    private static Embedded declaring(List<Embedded> embedded, String wordFamily) {
        for (Embedded written : embedded) {
            if (written.wordFamily().equalsIgnoreCase(wordFamily)) {
                return written;
            }
        }
        return null;
    }

    /**
     * Gives up a family whose name another one already declares, and says so.
     *
     * <p>Whole, as a family with an unreadable face is given up whole: a face of it bound for a
     * slot the holder leaves empty does not travel either, because a second {@code w:font} under
     * the name is not read at all — measured, a family declaring only {@code embedBold} beside a
     * holder declaring only {@code embedRegular} had its bold program ignored by both editors.
     * The PPTX sibling ({@code PptxRenderEnvironment.SlotLedger}) is finer than this and lets two
     * families sharing a name both travel where their slots do not clash, because a deck holds one
     * entry with four elements in it and a second family can fill an element the first left empty.
     * A document holds no such shared element to fill.</p>
     *
     * <p>Its runs still name it, so they are set in whatever that name resolves to where the
     * document is read, which is the holder's face in both editors measured, except where the
     * editor knows the name itself: Word 16 drew its own Lato for a document declaring Lato over
     * another family's program. What this does not do is make the given-up family's text right —
     * it is still set in a face it was not laid out in. It makes the export say so.</p>
     *
     * <p>This comes before the family's faces are read, so a family given up for its name is
     * reported for that and not for the terms or the readability of faces that are never opened:
     * one line about a family, naming what stopped it first.</p>
     */
    private static void refuseForASharedName(FontFamilyDefinition family,
                                             Embedded holder,
                                             DocxExportReport.Builder report) {
        LOG.warn("DocxSemanticBackend: '{}' is set in the Word name '{}', which '{}' already ships "
                 + "as '{}'; a document ships one face set per name, matched without case, so '{}' "
                 + "travels with nothing and its text is set in whatever that name resolves to",
                family.name().name(),
                family.wordFamily(), holder.registered().name(), holder.wordFamily(),
                family.name().name());
        // Both families by the name they were registered under, where the other notes on an
        // embedded font name a family by the name it declares: these two declare one name, and it
        // is the registered one that tells them apart.
        report.add(DocxExportReport.Severity.DROPPED, "embedded font", null,
                "'" + family.name().name() + "' is set in the Word name '" + family.wordFamily()
                + "', which '" + holder.registered().name() + "' already ships as '"
                + holder.wordFamily() + "'; a document ships one face set per name, matched "
                + "without case, so '" + family.name().name() + "' travels with nothing and its "
                + "runs, which carry its own name, are set in whatever that name resolves to — the "
                + "face '" + holder.registered().name() + "' ships under it, or, where the editor "
                + "knows the name itself, its own");
    }

    /** Which of a family's four faces a document asked for. */
    private enum Slot {
        REGULAR("embedRegular"),
        BOLD("embedBold"),
        ITALIC("embedItalic"),
        BOLD_ITALIC("embedBoldItalic");

        private final String element;

        Slot(String element) {
            this.element = element;
        }

        String element() {
            return element;
        }

        /**
         * The face a style is set in.
         *
         * <p>An underline or a strikethrough is drawn over the regular face rather than
         * being a face of its own, so they answer the same as no decoration at all.</p>
         */
        static Slot of(DocumentTextStyle style) {
            if (style.decoration() == null) {
                return REGULAR;
            }
            return switch (style.decoration()) {
                case BOLD -> BOLD;
                case ITALIC -> ITALIC;
                case BOLD_ITALIC -> BOLD_ITALIC;
                default -> REGULAR;
            };
        }
    }

    /**
     * The faces the document uses, minus the ones that may not travel in an edited file.
     *
     * <p>Only what is used: the four faces of one family are two and a half megabytes, and
     * a document setting one line in a family has no use for the other three. A reader who
     * later bolds a word gets whatever their machine does for a missing bold face, which is
     * what happens in any document that does not carry one.</p>
     */
    private static List<Face> facesOf(FontFamilyDefinition family, Set<Slot> slots,
                                      DocxExportReport.Builder report) {
        FontFamilyDefinition.FontSourceSet sources = family.fontSourceSet().orElseThrow();
        List<Face> faces = new ArrayList<>(slots.size());
        for (Slot slot : Slot.values()) {
            if (!slots.contains(slot)) {
                continue;
            }
            addFace(faces, family, slot, report, switch (slot) {
                case REGULAR -> sources.regular();
                case BOLD -> sources.bold();
                case ITALIC -> sources.italic();
                case BOLD_ITALIC -> sources.boldItalic();
            });
        }
        return faces;
    }

    private static void addFace(List<Face> faces,
                                FontFamilyDefinition family,
                                Slot slot,
                                DocxExportReport.Builder report,
                                FontFamilyDefinition.FontBinarySource source) {
        if (source == null) {
            return;
        }
        byte[] bytes;
        // Any source may fail unchecked; the classpath one does for a missing resource.
        try (InputStream in = source.openStream()) {
            bytes = in.readAllBytes();
        } catch (IOException | RuntimeException failure) {
            LOG.warn("DocxSemanticBackend: '{}' face of '{}' could not be read ({}); the "
                     + "document declares the family and ships this face without it",
                    slot.element(), family.wordFamily(), failure.toString());
            report.add(DocxExportReport.Severity.DROPPED, "embedded font", null,
                    "the " + slot.element() + " face of '" + family.wordFamily()
                    + "' could not be read (" + failure + "), so a reader without it "
                    + "installed sees a substituted face");
            return;
        }
        switch (DocxFontEmbedding.permissionOf(bytes)) {
            case RESTRICTED -> {
                LOG.warn("DocxSemanticBackend: '{}' does not permit embedding (OS/2 fsType), so "
                         + "it is named but not shipped; a reader without it installed sees a "
                         + "substituted face", family.wordFamily());
                report.add(DocxExportReport.Severity.DROPPED, "embedded font", null,
                        "'" + family.wordFamily() + "' does not permit embedding (OS/2 fsType), "
                        + "so a reader without it installed sees a substituted face");
                return;
            }
            case PREVIEW_ONLY -> {
                LOG.warn("DocxSemanticBackend: '{}' permits embedding for reading and printing "
                         + "only, which a document meant to be edited cannot rely on, so it is "
                         + "named but not shipped", family.wordFamily());
                report.add(DocxExportReport.Severity.DROPPED, "embedded font", null,
                        "'" + family.wordFamily() + "' permits embedding for reading and "
                        + "printing only, which a document meant to be edited cannot rely on");
                return;
            }
            default -> {
            }
        }
        DocxFontEmbedding.Obfuscated obfuscated = DocxFontEmbedding.obfuscate(bytes);
        faces.add(new Face(slot.element(), bytes, obfuscated.bytes(), obfuscated.fontKey(), DocxNaturalLine.of(bytes)));
    }

    /**
     * Collects every face the tree names, wherever a style can sit — a table's cell built from a
     * node, a page reference and a page number, a list's items and markers made of runs among them
     * — and the faces the markdown of a paragraph or a list's item sets pieces of it in
     * ({@link DocxMarkdown}): the table is written before any paragraph is, so a face the pieces
     * ask for is shipped whether or not the page sets them — a session that reads no markdown
     * ships one it does not use.
     */
    private static void collectFonts(DocumentNode node, Map<FontName, Set<Slot>> into) {
        if (node instanceof ParagraphNode paragraph) {
            add(paragraph.textStyle(), into);
            if (paragraph.textStyle() != null && DocxMarkdown.mayRead(paragraph)) {
                DocxMarkdown.read(paragraph.text(), paragraph.textStyle()).forEach(piece -> add(piece.style(), into));
            }
            addRuns(paragraph.inlineRuns(), into);
        } else if (node instanceof PageReferenceNode reference) {
            add(reference.textStyle(), into);
        } else if (node instanceof PageFieldNode field) {
            add(field.textStyle(), into);
        } else if (node instanceof ListNode list) {
            add(list.textStyle(), into);
            if (list.textStyle() != null) {
                // Read as the page reads them: a typed marker taken off, a tree's indent and
                // marker read with the item, in the face the page sets them in.
                for (DocxMarkdown.ItemReading item : DocxMarkdown.items(list)) {
                    if (DocxMarkdown.holdsAMark(item.text())) {
                        DocxMarkdown.read(item.text(), list.textStyle()).forEach(piece -> add(piece.style(), into));
                    }
                }
            }
            addRuns(list.marker().runs(), into);
            addItems(list.nestedItems(), into);
        } else if (node instanceof TableNode table) {
            add(styleOf(table.defaultCellStyle()), into);
            table.rowStyles().values().forEach(style -> add(styleOf(style), into));
            table.columnStyles().values().forEach(style -> add(styleOf(style), into));
            table.rows().forEach(row -> row.forEach(cell -> add(styleOf(cell), into)));
        }
        for (DocumentNode child : DocxWrittenTree.under(node)) {
            collectFonts(child, into);
        }
    }

    /** The faces a sequence of runs is set in: a run's text and a chip's, in their own styles. */
    private static void addRuns(List<InlineRun> runs, Map<FontName, Set<Slot>> into) {
        for (InlineRun run : runs) {
            if (run instanceof InlineTextRun text) {
                add(text.textStyle(), into);
            } else if (run instanceof InlineHighlightRun highlight) {
                add(highlight.textStyle(), into);
            }
        }
    }

    /** The faces a tree of list items made of runs is set in, their own markers' included. */
    private static void addItems(List<ListItem> items, Map<FontName, Set<Slot>> into) {
        for (ListItem item : items) {
            addRuns(item.runs(), into);
            if (item.marker() != null) {
                addRuns(item.marker().runs(), into);
            }
            addItems(item.children(), into);
        }
    }

    private static DocumentTextStyle styleOf(DocumentTableCell cell) {
        return cell == null ? null : styleOf(cell.style());
    }

    private static DocumentTextStyle styleOf(DocumentTableStyle style) {
        return style == null ? null : style.textStyle();
    }

    private static void add(DocumentTextStyle style, Map<FontName, Set<Slot>> into) {
        if (style != null && style.fontName() != null) {
            into.computeIfAbsent(style.fontName(), key -> java.util.EnumSet.noneOf(Slot.class))
                    .add(Slot.of(style));
        }
    }

    /** The font table part, naming each family and pointing at the faces shipped for it. */
    private static String toXml(List<Embedded> embedded) {
        StringBuilder xml = new StringBuilder(512);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                .append("<w:fonts xmlns:w=\"").append(W_NS).append("\" xmlns:r=\"").append(R_NS).append("\">");
        for (Embedded family : embedded) {
            xml.append("<w:font w:name=\"").append(escape(family.wordFamily())).append("\">");
            for (Face face : family.faces()) {
                xml.append("<w:").append(face.element())
                        .append(" r:id=\"").append(face.relationshipId)
                        .append("\" w:fontKey=\"").append(face.fontKey)
                        .append("\"/>");
            }
            xml.append("</w:font>");
        }
        return xml.append("</w:fonts>").toString();
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;");
    }

    private static PackagePartName partName(String path) throws IOException {
        try {
            return PackagingURIHelper.createPartName(path);
        } catch (org.apache.poi.openxml4j.exceptions.InvalidFormatException failure) {
            throw new IOException("cannot name the part " + path, failure);
        }
    }
}
