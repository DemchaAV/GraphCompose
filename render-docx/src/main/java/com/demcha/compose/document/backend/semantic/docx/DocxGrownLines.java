package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.poi.ooxml.util.POIXMLUnits;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTOnOff;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;

import java.util.Set;

/**
 * How tall a paragraph's lines stand in an editor, and where its text stands in them, from what is
 * written: an exact line, or a least one its text may grow.
 *
 * <p>An exact line is as tall as written, whatever its text, and Word and LibreOffice stand its
 * baseline four fifths of the way down it ({@link DocxTextBands#BASELINE_SHARE}). A least line is
 * as tall as written or as its runs' own line, whichever is taller, and keeps the spare above its
 * text: the baseline stands the line's deepest run's descent above the line's foot. A run's own
 * line is its face's ({@link DocxNaturalLine}), and a line of several runs reaches as high as the
 * tallest and as deep as the deepest of them, the paragraph's mark among them in its last line
 * (measured in Word and in LibreOffice 24.2: each face of the templates alone to 0.15pt, mixed
 * faces and sizes in one line to 0.1pt).</p>
 *
 * <p>A text's position does not move it in a least line: Word, raising every run of a line, keeps
 * the text on the line's foot until it passes the line's top, and LibreOffice moves it and grows
 * the line. So only a paragraph whose runs hold no position, no picture set in the line and no raise
 * of their own is read — a shape anchored to it takes no room in the line, and is passed over; the
 * rest, and one set in a face the document does not ship, have no reach.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocxGrownLines {

    /** No face shipped, and no default style: every line is read as exact or not at all. */
    static final DocxGrownLines NONE = new DocxGrownLines(DocxFontTable.ShippedLines.NONE, null);

    /** What a paragraph may hold besides its runs and still be read. */
    private static final Set<String> PARAGRAPH_PARTS = Set.of("pPr", "r", "hyperlink", "bookmarkStart", "bookmarkEnd");
    /** What a run may hold and still be read: text, and what sits in a line of it. */
    private static final Set<String> RUN_PARTS = Set.of("rPr", "t", "tab", "br", "fldChar", "instrText");
    /**
     * How far short of its runs' own line, by the faces' tables, a least line may be written and
     * be taken as tall as written, in points: the arithmetic's own error, and no more. A line
     * rounded down to the twip, a hundredth or two short of its own, grows in the editor to its
     * own on every line, and the paragraph under it stands that much lower for each: Word and
     * LibreOffice set a least line no shorter than its runs' own, where an exact one keeps the
     * rounding the export carries (9pt PT Serif lines written at 11.9pt, a fortieth short of their
     * own, moved {@code ClassicSerif}'s lines up to 0.45pt in LibreOffice).
     */
    static final double EPSILON = 0.001;
    /** WordprocessingML's namespace, which a run's properties' attributes are in. */
    private static final String W_NAMESPACE = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    /** Half points in a point, as Word writes sizes. */
    private static final double HALF_POINTS_PER_POINT = 2;
    private static final double POINT_TO_TWIP = 20;

    private final DocxFontTable.ShippedLines faces;
    private final DocumentTextStyle defaults;

    /**
     * @param faces    the line each face the document ships is set in
     * @param defaults the style the document's Normal style states, which a run that names no face
     *                 or size of its own is set in; null where it states none
     */
    DocxGrownLines(DocxFontTable.ShippedLines faces, DocumentTextStyle defaults) {
        this.faces = faces;
        this.defaults = defaults;
    }

    /**
     * How far a paragraph's runs and mark reach above and below their baseline, set in their own
     * line, in points.
     *
     * @param paragraph the written paragraph
     * @return the reach, or null where one of its runs is not read: a picture, a raise, a face the
     *         document does not ship, a run Word sets right to left, or a marker Word draws itself
     */
    Reach reachOf(CTP paragraph) {
        CTPPr properties = paragraph.getPPr();
        if (properties != null && properties.isSetNumPr()) {
            // Word draws a list's marker in the numbering's own face.
            return null;
        }
        // The mark is set in what it states, or else in the Normal style, as a run is: Word counts
        // it into the paragraph's last line.
        Reach reach = reachOf(properties != null && properties.isSetRPr() ? properties.getRPr() : null, Reach.NONE);
        if (reach == null || !holdsOnly(paragraph, PARAGRAPH_PARTS)) {
            return null;
        }
        boolean anyRun = false;
        try (XmlCursor cursor = paragraph.newCursor()) {
            cursor.selectPath("declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' "
                              + "./w:r | ./w:hyperlink/w:r");
            while (cursor.toNextSelection()) {
                XmlObject run = cursor.getObject();
                if (!(run instanceof org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR text)) {
                    return null;
                }
                if (holdsOnlyAnchors(text)) {
                    // A shape anchored to the paragraph takes no room in its line, and its run's
                    // face sets none of it (measured: a 14pt Normal run holding one left a 7.5pt
                    // least line as tall, in Word and in LibreOffice).
                    continue;
                }
                if (!holdsOnly(text, RUN_PARTS)) {
                    return null;
                }
                reach = reachOf(text.isSetRPr() ? text.getRPr() : null, reach);
                if (reach == null) {
                    return null;
                }
                anyRun = true;
            }
        }
        return anyRun ? reach : null;
    }

    /**
     * Whether a paragraph's line, at the height written, is at least as tall as its runs' own
     * line, so that written at a least height it stays that height, with every line's text the
     * same height off the line's foot.
     *
     * <p>Word stands each least line's baseline its own deepest run's descent above the foot, not
     * the paragraph's: a line of 8pt bold Lato over a line of 7.5pt Lato stood its second line
     * 0.1pt further down than its first, and its grid made it a step more. Only a paragraph whose
     * runs and mark share one descent, to {@link #EPSILON}, has one place for its text in every line.</p>
     *
     * @param paragraph the written paragraph
     * @return whether its runs are read ({@link #reachOf}), reach no taller than its line, to
     *         {@link #EPSILON}, and share one descent
     */
    boolean mayGrow(CTP paragraph) {
        CTPPr properties = paragraph.getPPr();
        CTSpacing spacing = properties != null && properties.isSetSpacing() ? properties.getSpacing() : null;
        if (spacing == null || !(spacing.getLine() instanceof Number twips)) {
            return false;
        }
        Reach reach = reachOf(paragraph);
        return reach != null && twips.doubleValue() / POINT_TO_TWIP + EPSILON >= reach.height()
               && reach.below() - reach.shallowest() <= EPSILON;
    }

    /**
     * How tall an editor stands a paragraph's line and where in it the baseline: an exact line as
     * written, a least line as written or as its runs' own line where that is taller.
     *
     * @param paragraph the written paragraph
     * @return the line, or null where its height is the editor's own, or a least line's runs are
     *         not read ({@link #reachOf})
     */
    Line lineOf(CTP paragraph) {
        CTPPr properties = paragraph.getPPr();
        CTSpacing spacing = properties != null && properties.isSetSpacing() ? properties.getSpacing() : null;
        if (spacing == null || !spacing.isSetLineRule() || !(spacing.getLine() instanceof Number twips)) {
            return null;
        }
        double written = twips.doubleValue() / POINT_TO_TWIP;
        if (spacing.getLineRule() == STLineSpacingRule.EXACT) {
            return new Line(written, written * DocxTextBands.BASELINE_SHARE);
        }
        if (spacing.getLineRule() != STLineSpacingRule.AT_LEAST) {
            return null;
        }
        Reach reach = reachOf(paragraph);
        if (reach == null) {
            return null;
        }
        double height = Math.max(written, reach.height());
        return new Line(height, height - reach.below());
    }

    private Reach reachOf(XmlObject runProperties, Reach so) {
        Run run = Run.of(runProperties);
        if (run == null) {
            return null;
        }
        // The Normal style states a face and a size, and never a weight or a slant.
        boolean bold = Boolean.TRUE.equals(run.bold());
        boolean italic = Boolean.TRUE.equals(run.italic());
        String slot = bold ? italic ? "embedBoldItalic" : "embedBold" : italic ? "embedItalic" : "embedRegular";
        DocxNaturalLine line = run.family() != null ? faces.of(run.family(), slot)
                : defaults != null ? faces.of(defaults.fontName(), slot) : null;
        double size = run.halfPoints() > 0 ? run.halfPoints() / HALF_POINTS_PER_POINT
                : defaults != null ? Math.round(defaults.size() * HALF_POINTS_PER_POINT) / HALF_POINTS_PER_POINT
                : Double.NaN;
        if (line == null || !(size > 0)) {
            return null;
        }
        return new Reach(Math.max(so.above(), line.ascent() * size), Math.max(so.below(), line.descent() * size),
                Math.min(so.shallowest(), line.descent() * size));
    }

    /**
     * Whether a run holds drawings anchored to its paragraph and nothing else of its own: no text,
     * no picture set in the line.
     */
    private static boolean holdsOnlyAnchors(org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR run) {
        if (run.sizeOfDrawingArray() == 0) {
            return false;
        }
        try (XmlCursor cursor = run.newCursor()) {
            for (boolean more = cursor.toFirstChild(); more; more = cursor.toNextSibling()) {
                String part = cursor.getName().getLocalPart();
                if ("drawing".equals(part)) {
                    try (XmlCursor drawing = cursor.getObject().newCursor()) {
                        for (boolean shape = drawing.toFirstChild(); shape; shape = drawing.toNextSibling()) {
                            if (!"anchor".equals(drawing.getName().getLocalPart())) {
                                return false;
                            }
                        }
                    }
                } else if (!"rPr".equals(part)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Whether every child element of {@code element} is one of {@code parts}. */
    private static boolean holdsOnly(XmlObject element, Set<String> parts) {
        try (XmlCursor cursor = element.newCursor()) {
            for (boolean more = cursor.toFirstChild(); more; more = cursor.toNextSibling()) {
                if (!parts.contains(cursor.getName().getLocalPart())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * How far a line's text reaches above and below its baseline, in points.
     *
     * @param above      how far above, its faces' line gap included
     * @param below      how far below: its deepest run's descent
     * @param shallowest its shallowest run's descent
     */
    record Reach(double above, double below, double shallowest) {
        static final Reach NONE = new Reach(0, 0, Double.POSITIVE_INFINITY);

        /** The line's height. */
        double height() {
            return above + below;
        }
    }

    /**
     * How tall an editor stands a paragraph's line, and its baseline, from the line's top, in
     * points.
     *
     * @param height   the line's height
     * @param baseline how far below the line's top the baseline stands
     */
    record Line(double height, double baseline) {
    }

    /**
     * What a run's properties say of its face: the family, size and slant they name, each null or
     * 0 where they leave it to the style. Null where they say something this does not read.
     */
    private record Run(String family, int halfPoints, Boolean bold, Boolean italic) {
        private static final Run UNSTATED = new Run(null, 0, null, null);

        static Run of(XmlObject properties) {
            if (properties == null) {
                return UNSTATED;
            }
            String family = null;
            int halfPoints = 0;
            Boolean bold = null;
            Boolean italic = null;
            try (XmlCursor cursor = properties.newCursor()) {
                for (boolean more = cursor.toFirstChild(); more; more = cursor.toNextSibling()) {
                    String name = cursor.getName().getLocalPart();
                    XmlObject value = cursor.getObject();
                    switch (name) {
                        case "rFonts" -> family = attribute(value, "ascii");
                        case "sz" -> {
                            halfPoints = number(attribute(value, "val"));
                            if (halfPoints <= 0) {
                                return null;
                            }
                        }
                        case "b" -> bold = value instanceof CTOnOff on ? POIXMLUnits.parseOnOff(on) : Boolean.TRUE;
                        case "i" -> italic = value instanceof CTOnOff on ? POIXMLUnits.parseOnOff(on) : Boolean.TRUE;
                        // A raise, a script, a style or a right-to-left run sets its letters elsewhere.
                        case "position", "vertAlign", "rStyle", "rtl", "cs" -> {
                            if (!"position".equals(name) || number(attribute(value, "val")) != 0) {
                                return null;
                            }
                        }
                        default -> {
                        }
                    }
                }
            }
            return new Run(family, halfPoints, bold, italic);
        }

        private static String attribute(XmlObject element, String name) {
            try (XmlCursor cursor = element.newCursor()) {
                return cursor.getAttributeText(new javax.xml.namespace.QName(W_NAMESPACE, name));
            }
        }

        private static int number(String text) {
            try {
                return text == null ? 0 : Integer.parseInt(text.trim());
            } catch (NumberFormatException notANumber) {
                return -1;
            }
        }
    }
}
