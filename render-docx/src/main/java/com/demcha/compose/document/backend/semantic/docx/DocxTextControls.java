package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.engine.text.TextControlSanitizer;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeaderFooter;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;

/**
 * Takes out of a Word file's text the characters the page draws nothing for.
 *
 * <p>The layout removes from the text it sets every character of Unicode's category C — a
 * control (a bell, a null, a tab), a format character (a soft hyphen, a zero-width space), one
 * of private use or none assigned — but the formatting controls it reads, the direction marks
 * and the joiners ({@link TextControlSanitizer#removeExceptFormattingControls}), and it keeps the
 * line breaks, which it breaks lines at. Written to Word, one XML cannot hold came out as a
 * question mark, "Total? due" where the page reads "Total due"; a tab stood as a gap the page
 * does not leave, and a soft hyphen or a zero-width space let Word break a word the page sets
 * whole. A line break left in a run's text is Word's space, and stays.</p>
 *
 * <p>Text written into the file as XML rather than as a run's — a badge's initials in its
 * shape, a list level's marker, a picture's description — is cleaned where it is written,
 * with {@link #kept}: written as XML, a character XML cannot hold is a question mark already.</p>
 */
final class DocxTextControls {

    private static final String TEXT_PATH =
            "declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' .//w:t";

    private DocxTextControls() {
    }

    /**
     * Takes the characters the page draws nothing for out of the text of a document's body,
     * headers and footers.
     *
     * @param document the document as written
     */
    static void dropFrom(XWPFDocument document) {
        dropFrom(document.getDocument());
        // Read off the document's parts: a header or footer a policy creates is one of them, and
        // is in no list of the document's headers and footers until the file is read back.
        for (org.apache.poi.ooxml.POIXMLDocumentPart part : document.getRelations()) {
            if (part instanceof XWPFHeaderFooter headerOrFooter) {
                dropFrom(headerOrFooter._getHdrFtr());
            }
        }
    }

    private static void dropFrom(XmlObject part) {
        for (XmlObject text : part.selectPath(TEXT_PATH)) {
            try (XmlCursor cursor = text.newCursor()) {
                String written = cursor.getTextValue();
                String kept = kept(written);
                if (!kept.equals(written)) {
                    // A space the text now starts or ends with stays: one a control character stood
                    // before was the run's own, and Word drops an edge space its text does not keep.
                    if (!kept.isEmpty() && (Character.isWhitespace(kept.charAt(0))
                                            || Character.isWhitespace(kept.charAt(kept.length() - 1)))) {
                        cursor.setAttributeText(SPACE, "preserve");
                    }
                    cursor.setTextValue(kept);
                }
            }
        }
    }

    /** The {@code xml:space} attribute, which keeps a text's edge spaces. */
    private static final javax.xml.namespace.QName SPACE =
            new javax.xml.namespace.QName("http://www.w3.org/XML/1998/namespace", "space");

    /**
     * A text without the characters the page draws nothing for: those of category C but the
     * formatting controls and the line breaks.
     *
     * @param text the text as written, or {@code null}
     * @return the text kept, the same string where there is nothing to take out
     */
    static String kept(String text) {
        if (text == null) {
            return null;
        }
        StringBuilder kept = null;
        for (int index = 0; index < text.length(); ) {
            int codePoint = text.codePointAt(index);
            boolean drawsNothing = codePoint != '\n' && codePoint != '\r' && isCategoryC(codePoint)
                                   && !TextControlSanitizer.isFormattingControl(codePoint);
            if (drawsNothing && kept == null) {
                kept = new StringBuilder(text.length()).append(text, 0, index);
            } else if (!drawsNothing && kept != null) {
                kept.appendCodePoint(codePoint);
            }
            index += Character.charCount(codePoint);
        }
        return kept == null ? text : kept.toString();
    }

    /** Whether a code point is of Unicode's category C, as the layout reads it. */
    private static boolean isCategoryC(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.CONTROL || type == Character.FORMAT || type == Character.PRIVATE_USE
               || type == Character.SURROGATE || type == Character.UNASSIGNED;
    }
}
