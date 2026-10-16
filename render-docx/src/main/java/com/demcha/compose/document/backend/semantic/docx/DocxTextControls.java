package com.demcha.compose.document.backend.semantic.docx;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeaderFooter;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;

/**
 * Takes out of a Word file's text the control characters the page draws nothing for.
 *
 * <p>The layout removes a control character — a bell, a null, a tab — from the text it sets
 * (it is in Unicode's category C), and keeps the line breaks, which it breaks lines at. Written
 * to Word, a character XML cannot hold came out as a question mark, "Total? due" where the page
 * reads "Total due", and a tab stood as a gap the page does not leave. A line break left in a
 * run's text is Word's space, and stays.</p>
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
     * Takes the control characters out of the text of a document's body, headers and footers.
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
     * A text without the control characters the page draws nothing for: every one but the line
     * breaks.
     *
     * @param text the text as written, or {@code null}
     * @return the text kept, the same string where there is nothing to take out
     */
    static String kept(String text) {
        if (text == null) {
            return null;
        }
        StringBuilder kept = null;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            boolean drawsNothing = Character.getType(character) == Character.CONTROL
                                   && character != '\n' && character != '\r';
            if (drawsNothing && kept == null) {
                kept = new StringBuilder(text.length()).append(text, 0, index);
            } else if (!drawsNothing && kept != null) {
                kept.append(character);
            }
        }
        return kept == null ? text : kept.toString();
    }
}
