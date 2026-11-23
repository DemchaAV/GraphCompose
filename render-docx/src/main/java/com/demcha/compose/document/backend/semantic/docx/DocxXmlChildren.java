package com.demcha.compose.document.backend.semantic.docx;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * An element's descendants by a path of child names: what an XPath of child steps alone —
 * {@code ./w:pPr/w:spacing} — selects, in the same order, without running the path.
 *
 * <p>The passes over a written document ask each paragraph and run for a few of its children.
 * Asked through {@link XmlObject#selectPath}, each question ran an XPath engine of its own, once a
 * paragraph or a run. Walking the children answers them alike, as the path reads the element's
 * typed and untyped content alike — a text box's paragraphs, which the file holds as XML of no
 * Word type, included. A whole part's paragraphs are still selected by path: over a whole part,
 * the XPath engine walks no slower than a cursor does.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocxXmlChildren {

    private DocxXmlChildren() {
    }

    /**
     * The elements a path of child steps leads to from an element, in document order.
     *
     * @param from  the element the path starts from
     * @param steps the name of each step's children, from the first; one step or more
     * @return what the path selects; empty where nothing is at its end
     */
    static List<XmlObject> path(XmlObject from, QName... steps) {
        List<XmlObject> found = new ArrayList<>();
        try (XmlCursor cursor = from.newCursor()) {
            walk(cursor, steps, 0, found);
        }
        return found;
    }

    /**
     * The first element a path of child steps leads to from an element, in document order.
     *
     * @param from  the element the path starts from
     * @param steps the name of each step's children, from the first; one step or more
     * @return the first the path selects, or {@code null} where nothing is at its end
     */
    static XmlObject first(XmlObject from, QName... steps) {
        List<XmlObject> found = path(from, steps);
        return found.isEmpty() ? null : found.get(0);
    }

    private static void walk(XmlCursor cursor, QName[] steps, int step, List<XmlObject> found) {
        for (boolean more = cursor.toChild(steps[step]); more; more = cursor.toNextSibling(steps[step])) {
            if (step == steps.length - 1) {
                found.add(cursor.getObject());
            } else {
                try (XmlCursor below = cursor.newCursor()) {
                    walk(below, steps, step + 1, found);
                }
            }
        }
    }
}
