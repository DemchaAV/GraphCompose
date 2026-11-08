package com.demcha.compose.document.backend.semantic.docx;

import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeaderFooter;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes the shaded spaces that close a paragraph as no-break spaces, so that Word fills them.
 *
 * <p>Word draws nothing for the spaces at the end of a paragraph, their run's shading included.
 * A chip of spaces closing its paragraph — the white cut-out {@code TealPulse} takes out of the
 * rule behind a section heading — came out in Word as the letters' fill alone: the cut-out ended
 * 9.8pt short of where the page ends it, and the rule ran on that much nearer the heading.
 * Word fills a no-break space where it stands: written as three, the cut-out ends 2.3pt short of
 * where the page ends it, and nothing else on that page moves (measured in Word 16). LibreOffice
 * fills neither, and its chip ends at its letters (measured in LibreOffice 24.2); the export says
 * so in the chip's note.</p>
 *
 * <p>Only the spaces after the paragraph's last character take the place of no-break spaces, in
 * shaded runs: a space Word draws stays a space, and so does one with nothing behind it to fill.
 * A drawing anchored in the paragraph takes no room in its line and is passed over; an inline
 * picture, a tab, a break or a field character closes the line itself, and nothing before it is
 * changed. A simple field's result is read as text; the export writes its fields with field
 * characters.</p>
 */
final class DocxClosingSpaces {

    private static final String W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

    private static final String NAMESPACES = "declare namespace w='" + W + "' "
            + "declare namespace wp='http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing' ";

    /** Every paragraph of a part, those in its tables, cells and text boxes included. */
    private static final String PARAGRAPHS = NAMESPACES + ".//w:p";

    /** A paragraph's own runs in reading order, those inside its links and simple fields included. */
    private static final String RUNS = NAMESPACES + "./w:r | ./w:hyperlink/w:r | ./w:fldSimple/w:r";

    private static final char NO_BREAK_SPACE = (char) 0x00A0;

    private DocxClosingSpaces() {
    }

    /**
     * Writes the shaded spaces closing each paragraph of a document, its headers and footers
     * included, as no-break spaces.
     *
     * @param document the document written
     * @return the runs whose spaces were written so, in the order they were met: a {@link CTR}
     *         each, but in a text box's content, which the file holds as XML of no Word type until
     *         it is read back
     */
    static List<XmlObject> fill(XWPFDocument document) {
        List<XmlObject> filled = new ArrayList<>();
        fill(document.getDocument(), filled);
        // Read off the document's parts: a header or footer a policy creates is one of them, and
        // is in no list of the document's headers and footers until the file is read back.
        for (POIXMLDocumentPart part : document.getRelations()) {
            if (part instanceof XWPFHeaderFooter headerOrFooter) {
                fill(headerOrFooter._getHdrFtr(), filled);
            }
        }
        return filled;
    }

    private static void fill(XmlObject part, List<XmlObject> filled) {
        for (XmlObject paragraph : part.selectPath(PARAGRAPHS)) {
            XmlObject[] runs = paragraph.selectPath(RUNS);
            for (int index = runs.length - 1; index >= 0; index--) {
                XmlObject run = runs[index];
                Closing closing = fillTheClosingSpaces(run);
                if (closing == Closing.FILLED || closing == Closing.FILLED_AND_ENDED) {
                    filled.add(run);
                }
                if (closing == Closing.ENDED || closing == Closing.FILLED_AND_ENDED) {
                    break;
                }
            }
        }
    }

    /** What a run read from its end does to the walk back along its paragraph. */
    private enum Closing {
        /** Nothing but spaces or what takes no room: the walk goes on to the run before. */
        PASSED,
        /** Shaded spaces written as no-break spaces, and nothing else: the walk goes on. */
        FILLED,
        /** Something the line shows: the walk ends. */
        ENDED,
        /** Shaded spaces written as no-break spaces after something the line shows: the walk ends. */
        FILLED_AND_ENDED
    }

    /**
     * Reads a run's content from its end: its text's closing spaces, which it writes as no-break
     * spaces where the run is shaded, up to the first thing the line shows.
     */
    private static Closing fillTheClosingSpaces(XmlObject run) {
        boolean shaded = isShaded(run);
        boolean filled = false;
        List<XmlObject> content = new ArrayList<>();
        try (XmlCursor cursor = run.newCursor()) {
            if (cursor.toFirstChild()) {
                do {
                    content.add(cursor.getObject());
                } while (cursor.toNextSibling());
            }
        }
        for (int index = content.size() - 1; index >= 0; index--) {
            XmlObject child = content.get(index);
            String name = child.getDomNode().getLocalName();
            if ("rPr".equals(name)) {
                continue;
            }
            if ("drawing".equals(name) && child.selectPath(NAMESPACES + "./wp:inline").length == 0) {
                // Anchored, it takes no room in the line.
                continue;
            }
            if (!"t".equals(name)) {
                return filled ? Closing.FILLED_AND_ENDED : Closing.ENDED;
            }
            String text;
            try (XmlCursor cursor = child.newCursor()) {
                text = cursor.getTextValue();
            }
            int end = text.length();
            while (end > 0 && text.charAt(end - 1) == ' ') {
                end--;
            }
            if (shaded && end < text.length()) {
                try (XmlCursor cursor = child.newCursor()) {
                    cursor.setTextValue(text.substring(0, end)
                                        + String.valueOf(NO_BREAK_SPACE).repeat(text.length() - end));
                }
                filled = true;
            }
            if (end > 0) {
                return filled ? Closing.FILLED_AND_ENDED : Closing.ENDED;
            }
        }
        return filled ? Closing.FILLED : Closing.PASSED;
    }

    /**
     * Whether a run is shaded: its properties fill the box behind its letters. Read off its XML,
     * which a text box's content holds untyped.
     */
    private static boolean isShaded(XmlObject run) {
        XmlObject[] shadings = run.selectPath(NAMESPACES + "./w:rPr/w:shd");
        if (shadings.length == 0) {
            return false;
        }
        try (XmlCursor cursor = shadings[0].newCursor()) {
            String fill = cursor.getAttributeText(new QName(W, "fill"));
            return fill != null && !"auto".equalsIgnoreCase(fill)
                   && !"nil".equals(cursor.getAttributeText(new QName(W, "val")));
        }
    }
}
