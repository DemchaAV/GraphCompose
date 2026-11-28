package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontFamilyDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The faces of one family a document ships: which of its four slots the text asks for, each
 * face's file read and checked against its terms, and the note on a face of another typeface than
 * the family's regular one.
 *
 * <p>What the font table writes them into, and which family keeps a name, is
 * {@link DocxFontTable}'s.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocxFontFaces {

    // The category the export's lines about its fonts have always been logged under.
    private static final Logger LOG = LoggerFactory.getLogger(DocxFontTable.class);

    private DocxFontFaces() {
    }

    /**
     * One face of a family: which slot it fills, its file and the bytes the part stores, how the
     * part refers to it, and the line an editor sets it in.
     */
    static final class Face {
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

        /** The key the part's bytes were obfuscated against, as the font table writes it. */
        String fontKey() {
            return fontKey;
        }

        /** The relationship the font table points at the face's part by. */
        String relationshipId() {
            return relationshipId;
        }

        void relationshipId(String id) {
            this.relationshipId = id;
        }
    }

    /** Which of a family's four faces a document asked for. */
    enum Slot {
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
    static List<Face> facesOf(FontFamilyDefinition family, Set<Slot> slots,
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
        nameTheFacesLibreOfficeWillNotDraw(family, faces, report);
        return faces;
    }

    /**
     * Names a family's faces whose program is of another typeface than the one its regular slot
     * holds: faces of that kind LibreOffice was measured not to draw.
     *
     * <p>A family's slots are filled one file at a time, so a caller can put a program of one
     * typeface in {@code embedRegular} and a program of another in {@code embedBold}. The export
     * writes both, and the PDF backend draws each paragraph in the program its slot holds. The
     * editors part there, measured with a bold run; the figures and what they cost are in the
     * backend capability details:</p>
     * <ul>
     *   <li>LibreOffice draws neither paragraph from the other program, and draws its runs from the
     *   regular one: 24.2 on Lato beside IBM Plex Mono, and 26.8, under a name no font on the
     *   machine answers, on Arsenal beside Volkhov, emboldening the regular program. 26.8 read the
     *   program once its family record ({@code name} id 1) was rewritten to the family name the
     *   regular program states — in that probe also the declared name.</li>
     *   <li>Word 16 draws it where no font on the machine answers the name, whether or not the
     *   program's names were rewritten. The Lato document showed Word's own Lato — a name Office
     *   also serves from its cloud font cache — and not which embedded program Word reads.</li>
     * </ul>
     * <p>So in LibreOffice the program travels for nothing and those runs are set in a face they
     * were not measured in. Nothing said so; this does.</p>
     *
     * <p>Not a refusal, and not reported as one: both faces are written, and the family is not
     * turned away. It is the document being the nearest thing an editor will read
     * ({@link DocxExportReport.Severity#APPROXIMATED}), which is what the note says.</p>
     *
     * <p>Whether two programs are of one typeface is read off the programs themselves
     * ({@link DocxTypeface#typefaceOf}), compared with {@link String#equalsIgnoreCase}, which folds the same
     * way wherever the export runs — unlike {@code toLowerCase()} and its default locale. No
     * family a document here is set in draws this note: read as that method reads them, all 188
     * pairs of faces within the 35 bundled families state one typeface, and so do the two files the
     * {@code OrangeOps} preset pairs, whose bold face states {@code Oswald SemiBold} as its family
     * and {@code Oswald} as its typographic one — read by its family record alone, that face would
     * be called a face of another typeface.</p>
     *
     * <p>The note follows the typeface. LibreOffice 26.8 was seen to read the family record: it read
     * a program of another typeface once that record was rewritten, to a string that was both the
     * regular program's family and the declared name. What it compares the record with, and
     * whether it reads the typographic one first, is not isolated. So where a face's family record differs from its typographic one, as {@code Oswald-SemiBold}'s
     * does, whether LibreOffice draws it beside the family's regular face is unmeasured, and the
     * note's silence there says nothing about it.</p>
     *
     * <p>Said only where the family ships a regular face and both programs name their typeface:
     * what was measured is an editor drawing a family from the face it reads for the regular slot,
     * and a program whose name cannot be read is one this cannot compare. Silence there is a
     * missing warning rather than a false one. Said for every other slot the family ships and every
     * pair of typefaces it holds: only the bold slot was measured, so the slot cannot be the thing
     * that decides what is said.</p>
     *
     * <p>What this does not do is make the document right. In LibreOffice the program still travels
     * unread and the lines still fall where the page did not put them — what the export can do
     * without writing different bytes is say so. Carrying such a family as two declared names,
     * which LibreOffice was measured to draw, is the alternative not taken.</p>
     */
    private static void nameTheFacesLibreOfficeWillNotDraw(FontFamilyDefinition family,
                                                           List<Face> faces,
                                                           DocxExportReport.Builder report) {
        if (faces.size() < 2) {
            return;
        }
        Face regular = null;
        for (Face face : faces) {
            if (face.element().equals(Slot.REGULAR.element())) {
                regular = face;
                break;
            }
        }
        if (regular == null) {
            return;
        }
        String drawn = DocxTypeface.typefaceOf(regular.font());
        if (drawn == null) {
            return;
        }
        for (Face face : faces) {
            if (face == regular) {
                continue;
            }
            String own = DocxTypeface.typefaceOf(face.font());
            if (own == null || own.equalsIgnoreCase(drawn)) {
                continue;
            }
            LOG.warn("DocxSemanticBackend: the '{}' face of '{}' is a face of '{}' where its "
                     + "regular face is a face of '{}'; LibreOffice draws the runs that reach this "
                     + "face from the regular program and was measured to leave such a program "
                     + "unread, while Word 16 draws it where no font on the machine answers the name",
                     face.element(), family.wordFamily(), own, drawn);
            report.add(DocxExportReport.Severity.APPROXIMATED, "embedded font", null,
                    "the " + face.element() + " face of '" + family.wordFamily() + "' is a face of '"
                    + own + "', where its embedRegular face is a face of '" + drawn + "'; measured "
                    + "with a bold run: LibreOffice (24.2 and 26.8) drew a program of another "
                    + "typeface for neither "
                    + "paragraph and drew those runs from the regular program — where the line is "
                    + "this face's own, at the height it measured rather than the height of the face "
                    + "they are drawn in — while Word 16 drew it where no font on the machine "
                    + "answered the name. LibreOffice 26.8 read such a program once its family "
                    + "record (name id 1) named the regular program's family (in that probe also "
                    + "the declared name); one slot was measured, so this is said of every pair of "
                    + "typefaces a family holds");
        }
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
}
