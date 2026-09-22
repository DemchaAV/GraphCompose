package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.InlineHighlightRun;
import com.demcha.compose.document.node.InlineRun;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.node.ListNode;
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
 * written in.
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
     * Writes a font table for every family the document uses that can be embedded.
     *
     * @param document  the document being written
     * @param graph     the document's own tree, read for the faces it names
     * @param custom    families the session registered, which win over the bundled ones
     * @param report    where a face that may not travel is recorded
     * @throws IOException if a part cannot be written
     */
    static void write(XWPFDocument document,
                      DocumentGraph graph,
                      Collection<FontFamilyDefinition> custom,
                      DocxExportReport.Builder report) throws IOException {
        List<Embedded> embedded = resolve(graph, custom, report);
        if (embedded.isEmpty()) {
            return;
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

    /** One family that will be written, and the faces of it that may be. */
    private record Embedded(String wordFamily, List<Face> faces) {
    }

    /** One face of a family: which slot it fills, its bytes, and how the part refers to it. */
    private static final class Face {
        private final String element;
        private final byte[] bytes;
        private final String fontKey;
        private String relationshipId;

        Face(String element, byte[] bytes, String fontKey) {
            this.element = element;
            this.bytes = bytes;
            this.fontKey = fontKey;
        }

        String element() {
            return element;
        }

        byte[] bytes() {
            return bytes;
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
     */
    private static List<Embedded> resolve(DocumentGraph graph,
                                          Collection<FontFamilyDefinition> custom,
                                          DocxExportReport.Builder report) {
        Map<FontName, Set<Slot>> used = new LinkedHashMap<>();
        for (DocumentNode root : graph.roots()) {
            collectFonts(root, used);
        }
        if (used.isEmpty()) {
            return List.of();
        }

        Map<FontName, FontFamilyDefinition> families = familiesByName(custom);

        List<Embedded> embedded = new ArrayList<>();
        for (Map.Entry<FontName, Set<Slot>> entry : used.entrySet()) {
            FontFamilyDefinition family = families.get(FontLibrary.resolveFamily(entry.getKey()));
            if (family == null || family.fontSourceSet().isEmpty()) {
                // A standard-14 name, or one nothing registered: there is no file to ship.
                continue;
            }
            List<Face> faces = facesOf(family, entry.getValue(), report);
            if (!faces.isEmpty()) {
                embedded.add(new Embedded(family.wordFamily(), faces));
            }
        }
        return embedded;
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
        try (InputStream in = source.openStream()) {
            bytes = in.readAllBytes();
        } catch (IOException failure) {
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
        faces.add(new Face(slot.element(), obfuscated.bytes(), obfuscated.fontKey()));
    }

    /** Collects every face the tree names, wherever a style can sit. */
    private static void collectFonts(DocumentNode node, Map<FontName, Set<Slot>> into) {
        if (node instanceof ParagraphNode paragraph) {
            add(paragraph.textStyle(), into);
            for (InlineRun run : paragraph.inlineRuns()) {
                if (run instanceof InlineTextRun text) {
                    add(text.textStyle(), into);
                } else if (run instanceof InlineHighlightRun highlight) {
                    add(highlight.textStyle(), into);
                }
            }
        } else if (node instanceof ListNode list) {
            add(list.textStyle(), into);
        } else if (node instanceof TableNode table) {
            add(styleOf(table.defaultCellStyle()), into);
            table.rowStyles().values().forEach(style -> add(styleOf(style), into));
            table.columnStyles().values().forEach(style -> add(styleOf(style), into));
            table.rows().forEach(row -> row.forEach(cell -> add(styleOf(cell), into)));
        }
        for (DocumentNode child : node.children()) {
            collectFonts(child, into);
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
