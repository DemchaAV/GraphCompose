package com.demcha.compose.document.backend.semantic.docx;

import java.io.IOException;

/**
 * Reads the name of the typeface a font program says it is a face of.
 *
 * <p>The export compares two programs by it to tell whether they are faces of one typeface
 * ({@link DocxFontFaces}).</p>
 *
 * @author Artem Demchyshyn
 */
final class DocxTypeface {

    private DocxTypeface() {
    }

    /**
     * What a program calls its own typeface: the typographic family ({@code name} id 16) where it
     * states one, the family (id 1) otherwise, or null where it states neither or cannot be read.
     *
     * <p>Both are read from one record — Windows, Unicode BMP, en-US — and only that one, so two
     * faces are never compared across two namings of one typeface. Taking whichever id 16 a file
     * happens to state first would read a localized record on one face against an English one on
     * its sibling; falling back to {@code NamingTable.getFontFamily()} would do the same, since it
     * searches the Unicode and Macintosh platforms too. Either way the answer is a name read under
     * terms its sibling's was not, and a difference between two such names says nothing. A face
     * that states this record for neither id is one this cannot compare, and says so with null.</p>
     *
     * @param program the face's own file, as read
     * @return the typeface's name, or null
     */
    static String typefaceOf(byte[] program) {
        try (org.apache.fontbox.ttf.TrueTypeFont font = new org.apache.fontbox.ttf.TTFParser()
                .parse(new org.apache.pdfbox.io.RandomAccessReadBuffer(program))) {
            org.apache.fontbox.ttf.NamingTable naming = font.getNaming();
            if (naming == null) {
                return null;
            }
            String typographic = stated(naming.getName(16, 3, 1, 0x409));
            return typographic != null ? typographic : stated(naming.getName(1, 3, 1, 0x409));
        } catch (IOException | RuntimeException unreadable) {
            return null;
        }
    }

    /** A name a face states, or null where it states none worth comparing. */
    private static String stated(String name) {
        return name == null || name.isBlank() ? null : name;
    }
}
