package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.style.DocumentBorders;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentStroke;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeaderFooter;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;

import javax.xml.namespace.QName;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * A colour's alpha, where Word holds it and where it does not.
 *
 * <p>Text holds it. Word 2010 added a text fill to a run's properties, {@code w14:textFill},
 * whose colour carries a transparency — Word's own Font, Text Effects, Transparency — and both
 * editors draw it (measured in Word 16.0.20430 and LibreOffice: red at three quarters
 * transparency comes out {@code (255, 193, 193)} over white in each). The two read it apart:
 * Word takes the colour from the text fill, LibreOffice takes it from {@code w:color} and the
 * transparency from the text fill, so {@code w:color} keeps the colour as authored — flattened,
 * LibreOffice would lighten it twice. Word sets such text as drawing when it saves a PDF, so
 * its PDF of the file holds no text layer for it; the file itself holds the text.</p>
 *
 * <p>A cell's shading, a border and a run's shading do not: they take an RGB and nothing else.
 * A translucent fill or rule written there is flattened first against the colour under it
 * ({@link #flatten}), so it shows the colour the page shows, and is no longer translucent —
 * recoloured underneath in Word, it stays the colour it was flattened to. The backend names
 * each one in its report.</p>
 */
final class DocxTranslucency {

    /** Word 2010's namespace, which the text fill is in. */
    private static final String W14 = "http://schemas.microsoft.com/office/word/2010/wordml";
    /** Markup compatibility, which says what a reader that does not know a namespace may skip. */
    private static final String MC = "http://schemas.openxmlformats.org/markup-compatibility/2006";

    private static final QName TEXT_FILL = new QName(W14, "textFill", "w14");
    private static final QName IGNORABLE = new QName(MC, "Ignorable", "mc");

    private DocxTranslucency() {
    }

    /**
     * Composites a colour over what sits beneath it, so a translucent fill survives a format
     * that has no alpha. An opaque colour is returned untouched.
     *
     * @param colour the colour laid on top, alpha included
     * @param under  the opaque colour beneath it
     * @return the opaque colour the two make
     */
    static Color flatten(Color colour, Color under) {
        int alpha = colour.getAlpha();
        if (alpha >= 255) {
            return colour;
        }
        double weight = alpha / 255.0;
        return new Color(
                blend(colour.getRed(), under.getRed(), weight),
                blend(colour.getGreen(), under.getGreen(), weight),
                blend(colour.getBlue(), under.getBlue(), weight));
    }

    private static int blend(int over, int under, double weight) {
        return (int) Math.round(over * weight + under * (1 - weight));
    }

    /**
     * Whether a colour is translucent: drawn, and not at full strength.
     *
     * @param colour a colour, or {@code null}
     * @return true for an alpha between 1 and 254
     */
    static boolean translucent(Color colour) {
        return colour != null && colour.getAlpha() > 0 && colour.getAlpha() < 255;
    }

    /**
     * Whether a fill is one a cell's shading holds only by flattening: translucent.
     *
     * @param fill a fill, or {@code null}
     * @return true for a translucent one
     */
    static boolean flattensFill(DocumentColor fill) {
        return fill != null && translucent(fill.color());
    }

    /**
     * Whether a stroke is one a border holds only by flattening: one with a width, in a colour not
     * at full strength. A wholly transparent one is flattened too — into the colour under it — so
     * the border keeps the room the page's rule holds in Word's row and cell geometry.
     *
     * @param stroke a stroke, or {@code null}
     * @return true where the border is written flattened
     */
    static boolean flattensStroke(DocumentStroke stroke) {
        return stroke != null && stroke.width() > 0 && stroke.color() != null
               && stroke.color().color().getAlpha() < 255;
    }

    /**
     * Whether any of a block's sides is a stroke a border holds only flattened.
     *
     * @param borders the sides, or {@code null}
     * @return true where one is
     */
    static boolean flattensSides(DocumentBorders borders) {
        return borders != null && (flattensStroke(borders.top()) || flattensStroke(borders.right())
                                   || flattensStroke(borders.bottom()) || flattensStroke(borders.left()));
    }

    /**
     * A fill as a cell's shading holds it: flattened against the colour under it where it is
     * translucent, and none where it is wholly transparent, as the page draws nothing.
     *
     * @param fill  the authored fill, or {@code null}
     * @param under the opaque colour under the block
     * @return the fill to write, or {@code null} for none
     */
    static DocumentColor flattenedFill(DocumentColor fill, Color under) {
        if (fill == null || fill.color().getAlpha() == 0) {
            return null;
        }
        return fill.color().getAlpha() >= 255 ? fill : DocumentColor.of(flatten(fill.color(), under));
    }

    /**
     * A stroke as a border holds it: a colour not at full strength flattened against the colour
     * the page draws it over.
     *
     * @param stroke the authored stroke, or {@code null}
     * @param under  the opaque colour under the stroke
     * @return the stroke to write
     */
    static DocumentStroke flattenedStroke(DocumentStroke stroke, Color under) {
        if (!flattensStroke(stroke)) {
            return stroke;
        }
        return new DocumentStroke(DocumentColor.of(flatten(stroke.color().color(), under)), stroke.width());
    }

    /**
     * A block's sides as borders hold them, each flattened as {@link #flattenedStroke} does.
     *
     * @param borders the authored sides, or {@code null}
     * @param under   the opaque colour under the sides
     * @return the sides to write
     */
    static DocumentBorders flattenedBorders(DocumentBorders borders, Color under) {
        return borders == null ? null : new DocumentBorders(flattenedStroke(borders.top(), under),
                flattenedStroke(borders.right(), under), flattenedStroke(borders.bottom(), under),
                flattenedStroke(borders.left(), under));
    }

    /**
     * A colour as Word writes one: six hex digits, its alpha dropped.
     *
     * @param colour a colour
     * @return {@code RRGGBB}
     */
    static String hex(Color colour) {
        return String.format("%02X%02X%02X", colour.getRed(), colour.getGreen(), colour.getBlue());
    }

    /**
     * Writes a run's transparency as its text fill, or takes away a text fill an opaque colour
     * no longer wants.
     *
     * <p>Word reads the transparency — not the opacity — as hundred-thousandths: three quarters
     * transparent is {@code 75000}. The fill declares its own namespace, so a part with no
     * text fill is not touched; {@link #settle} puts it last among the run's properties,
     * where Word writes it, once nothing more is written on them.</p>
     *
     * <p>A run follows its style's text fill where it writes none, and Word draws the fill's
     * colour over the run's own: an opaque run under a translucent default would come out in the
     * default's colour. Such a run writes an opaque fill of its own.</p>
     *
     * @param properties         a run's properties — a run's, a style's or a numbering level's —
     *                           its {@code w:color} already written
     * @param colour             the run's colour, alpha included
     * @param styleIsTranslucent whether the style the run follows carries a text fill
     * @return whether a text fill was written
     */
    static boolean writeTextAlpha(XmlObject properties, Color colour, boolean styleIsTranslucent) {
        removeTextFill(properties);
        if (colour.getAlpha() >= 255 && !styleIsTranslucent) {
            return false;
        }
        try (XmlCursor cursor = properties.newCursor()) {
            cursor.toEndToken();
            cursor.beginElement(TEXT_FILL);
            cursor.beginElement(new QName(W14, "solidFill", "w14"));
            cursor.beginElement(new QName(W14, "srgbClr", "w14"));
            cursor.insertAttributeWithValue(new QName(W14, "val", "w14"), hex(colour));
            if (colour.getAlpha() < 255) {
                long transparency = Math.round((255 - colour.getAlpha()) * 100000.0 / 255.0);
                cursor.beginElement(new QName(W14, "alpha", "w14"));
                cursor.insertAttributeWithValue(new QName(W14, "val", "w14"), Long.toString(transparency));
            }
        }
        return true;
    }

    /**
     * Copies a run's text fill onto other properties — a paragraph's mark, in whose style a
     * list's marker is drawn where its level states none — replacing any they held.
     *
     * @param from the properties a text fill is read from
     * @param to   the properties it is written on
     */
    static void copyTextFill(XmlObject from, XmlObject to) {
        removeTextFill(to);
        for (XmlObject fill : from.selectChildren(TEXT_FILL)) {
            try (XmlCursor source = fill.newCursor(); XmlCursor target = to.newCursor()) {
                target.toEndToken();
                source.copyXml(target);
            }
        }
    }

    private static void removeTextFill(XmlObject properties) {
        for (XmlObject fill : properties.selectChildren(TEXT_FILL)) {
            try (XmlCursor cursor = fill.newCursor()) {
                cursor.removeXml();
            }
        }
    }

    /**
     * Settles the text fills of a finished document. Each is moved last among its properties,
     * after what was written on them since — a decoration, a chip's shading, a direction —
     * where Word writes it. Each part holding one marks Word 2010's namespace on its root as one a
     * reader that does not know it may skip ({@code mc:Ignorable}). A part holding none is not
     * touched.
     *
     * @param document the finished document
     */
    static void settle(XWPFDocument document) {
        List<XmlObject> roots = new ArrayList<>();
        roots.add(document.getDocument());
        // The styles and numbering parts' roots as the document holds them, read from one of
        // their children: XWPFDocument.getStyle() parses a copy of the part.
        if (document.getStyles() != null && !document.getStyles().getStyles().isEmpty()) {
            roots.add(parentOf(document.getStyles().getStyles().get(0).getCTStyle()));
        }
        if (document.getNumbering() != null && !document.getNumbering().getAbstractNums().isEmpty()) {
            roots.add(parentOf(document.getNumbering().getAbstractNums().get(0).getCTAbstractNum()));
        }
        // From the document's relations: a header or footer made in this export is related to the
        // document, but POI lists in getHeaderList() and getFooterList() only the parts it read.
        for (org.apache.poi.ooxml.POIXMLDocumentPart part : document.getRelations()) {
            if (part instanceof XWPFHeaderFooter headerOrFooter) {
                roots.add(headerOrFooter._getHdrFtr());
            }
        }
        for (XmlObject root : roots) {
            if (root != null) {
                settlePart(root);
            }
        }
    }

    private static XmlObject parentOf(XmlObject child) {
        try (XmlCursor cursor = child.newCursor()) {
            cursor.toParent();
            return cursor.getObject();
        }
    }

    private static void settlePart(XmlObject root) {
        XmlObject[] fills = root.selectPath("declare namespace w14='" + W14 + "' .//w14:textFill");
        if (fills.length == 0) {
            return;
        }
        for (XmlObject fill : fills) {
            try (XmlCursor source = fill.newCursor(); XmlCursor target = fill.newCursor()) {
                target.toParent();
                target.toEndToken();
                source.moveXml(target);
            }
        }
        String ignorable;
        try (XmlCursor cursor = root.newCursor()) {
            ignorable = cursor.getAttributeText(IGNORABLE);
            if (ignorable != null && (" " + ignorable + " ").contains(" w14 ")) {
                return;
            }
            // Declared first, each once, so the attribute below takes the prefixes they name.
            boolean declaresW14 = W14.equals(cursor.namespaceForPrefix("w14"));
            boolean declaresMc = MC.equals(cursor.namespaceForPrefix("mc"));
            cursor.toNextToken();
            if (!declaresW14) {
                cursor.insertNamespace("w14", W14);
            }
            if (!declaresMc) {
                cursor.insertNamespace("mc", MC);
            }
        }
        try (XmlCursor cursor = root.newCursor()) {
            cursor.setAttributeText(IGNORABLE, ignorable == null ? "w14" : ignorable + " w14");
        }
    }
}
