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
 * the line. So only a paragraph whose runs of text hold no position and no raise of their own is
 * read — a shape anchored to it takes no room in the line, and is passed over; the rest, and one
 * set in a face the document does not ship, have no reach.</p>
 *
 * <p>A picture set in the line counts by where each editor stands it: Word lowers it by its run's
 * position, in a least line too, and LibreOffice stands it {@link #PICTURE_RISE} above the
 * baseline, whatever the position; a picture run's own face sets none of the line. A least line's
 * height is its runs' and pictures' reach, the tallest above the baseline and the deepest below
 * (measured: a 9pt picture lowered 1.5pt beside 8pt Lato left a 9.95pt least line as tall in Word,
 * and grew it to 10.75pt in LibreOffice). Only a line whose pictures stay within its text in both
 * places — no higher than the room the line leaves above its text's descent, no lower than that
 * descent — stands its text where it would without them (see {@link Reach#holdsItsPictures}).</p>
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
    /** EMUs in a point, as a drawing's extent is written. */
    private static final double EMU_PER_POINT = 12700;
    /**
     * How far above the baseline LibreOffice stands a picture set in a line, its run's position
     * aside, in points (measured in LibreOffice 24.2: a tenth of a point beside 8pt and 10pt Lato,
     * for pictures 6 to 11pt tall and lowered by 0 to 3pt). Word lowers it by the position. The
     * export's own reckoning of a picture's reach stands LibreOffice's on the baseline
     * (DocxSemanticBackend.PictureReach); the tenth counts here, in whether a least line holds it.
     */
    static final double PICTURE_RISE = 0.1;
    /** What a run holding pictures set in the line may hold besides them. */
    private static final Set<String> PICTURE_RUN_PARTS = Set.of("rPr", "drawing");

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
     * @return the reach, its pictures' among it, or null where one of its runs is not read: a raise,
     *         a face the document does not ship, a run Word sets right to left, a marker Word draws
     *         itself, a picture whose size or run's position is not a number, or pictures in a
     *         paragraph whose lines are broken, one of which may hold a picture alone, or that holds
     *         no letter beside them
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
        boolean broken = false;
        boolean lettered = false;
        double pictureTop = 0;
        double pictureFoot = Double.NEGATIVE_INFINITY;
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
                if (text.sizeOfDrawingArray() > 0 && holdsOnly(text, PICTURE_RUN_PARTS)) {
                    // Its face sets none of the line either (measured: a 24pt picture run left an
                    // 8pt least line as tall, in both editors).
                    double[] pictures = picturesIn(text);
                    if (pictures == null) {
                        return null;
                    }
                    pictureTop = Math.max(pictureTop, pictures[0]);
                    pictureFoot = Math.max(pictureFoot, pictures[1]);
                    continue;
                }
                if (!holdsOnly(text, RUN_PARTS)) {
                    return null;
                }
                broken |= text.sizeOfBrArray() > 0;
                for (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText letter : text.getTList()) {
                    // A no-break space is a space too.
                    lettered |= letter.getStringValue() != null && letter.getStringValue().codePoints()
                            .anyMatch(c -> !Character.isWhitespace(c) && !Character.isSpaceChar(c));
                }
                reach = reachOf(text.isSetRPr() ? text.getRPr() : null, reach);
                if (reach == null) {
                    return null;
                }
                anyRun = true;
            }
        }
        // Pictures among spaces alone, a rating's dots, LibreOffice sets on a taller line (measured:
        // 6.4pt dots between runs of spaces in 8pt Barlow, written at a least 9.7pt, stood the
        // languages under them 0.56pt low there, and a point lower for each row of dots more).
        boolean pictured = pictureFoot > Double.NEGATIVE_INFINITY;
        // A line wrapped where the page wraps it may leave a picture alone on a line too; the
        // corpus has none, and a break is where one is known.
        if (!anyRun || pictured && (broken || !lettered)) {
            return null;
        }
        return new Reach(reach.above(), reach.below(), reach.shallowest(), pictureTop, pictureFoot);
    }

    /**
     * How high above the baseline a run's pictures set in the line reach, the higher of Word's and
     * LibreOffice's places of them, and how far below it Word lowers them, in points; null where a
     * drawing in it is not a picture of a written size set in the line.
     */
    private static double[] picturesIn(org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR run) {
        double lowered = 0;
        if (run.isSetRPr() && run.getRPr().sizeOfPositionArray() > 0) {
            if (!(run.getRPr().getPositionArray(0).getVal() instanceof Number position)) {
                return null;
            }
            lowered = -position.doubleValue() / HALF_POINTS_PER_POINT;
        }
        double top = 0;
        for (org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTInline picture
                : inlinesIn(run)) {
            if (picture == null || picture.getExtent() == null) {
                return null;
            }
            double height = picture.getExtent().getCy() / EMU_PER_POINT;
            top = Math.max(top, Math.max(height - lowered, height + PICTURE_RISE));
        }
        return new double[]{top, lowered};
    }

    /** A run's drawings set in its line, one null for a drawing holding anything else. */
    private static java.util.List<org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTInline> inlinesIn(
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR run) {
        java.util.List<org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTInline> inlines =
                new java.util.ArrayList<>();
        for (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDrawing drawing : run.getDrawingList()) {
            if (drawing.sizeOfAnchorArray() > 0 || drawing.sizeOfInlineArray() == 0) {
                inlines.add(null);
            }
            inlines.addAll(drawing.getInlineList());
        }
        return inlines;
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
     *         {@link #EPSILON}, share one descent, and hold its pictures within them
     *         ({@link Reach#holdsItsPictures})
     */
    boolean mayGrow(CTP paragraph) {
        CTPPr properties = paragraph.getPPr();
        CTSpacing spacing = properties != null && properties.isSetSpacing() ? properties.getSpacing() : null;
        if (spacing == null || !(spacing.getLine() instanceof Number twips)) {
            return false;
        }
        Reach reach = reachOf(paragraph);
        double line = twips.doubleValue() / POINT_TO_TWIP;
        return reach != null && line + EPSILON >= reach.height()
               && reach.below() - reach.shallowest() <= EPSILON && reach.holdsItsPictures(line);
    }

    /**
     * How tall an editor stands a paragraph's line and where in it the baseline: an exact line as
     * written, a least line as written or as its runs' own line where that is taller.
     *
     * @param paragraph the written paragraph
     * @return the line, or null where its height is the editor's own, or a least line's runs are
     *         not read ({@link #reachOf}) or its pictures reach past its text, where the two editors
     *         stand them apart ({@link Reach#holdsItsPictures})
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
        // A picture past the text is set elsewhere by each editor; and a line holding pictures
        // written shorter than its text's own is one grown to its pictures (see
        // DocxSemanticBackend#makeRoomForPictures), not read here.
        if (reach == null || reach.holdsPictures() && !(written + EPSILON >= reach.height()
                                                         && reach.holdsItsPictures(written))) {
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
                Math.min(so.shallowest(), line.descent() * size), so.pictureTop(), so.pictureFoot());
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
     * How far a line's text and the pictures set in it reach above and below its baseline, in
     * points.
     *
     * @param above       how far its text reaches above, its faces' line gap included
     * @param below       how far its text reaches below: its deepest run's descent
     * @param shallowest  its shallowest run's descent
     * @param pictureTop  how far above its pictures reach, the higher of where Word and LibreOffice
     *                    stand them; 0 for none
     * @param pictureFoot how far below Word lowers its pictures; negative infinity for none
     */
    record Reach(double above, double below, double shallowest, double pictureTop, double pictureFoot) {
        static final Reach NONE = new Reach(0, 0, Double.POSITIVE_INFINITY, 0, Double.NEGATIVE_INFINITY);

        /** The text's line's height. */
        double height() {
            return above + below;
        }

        /**
         * Whether a line this tall holds its pictures within its text in both editors: none reaches
         * above the room the line leaves over its text's descent, nor below its shallowest run's
         * descent, so that neither editor grows the line for them nor stands its text elsewhere.
         *
         * @param line the line's height, in points
         */
        boolean holdsItsPictures(double line) {
            return pictureTop <= line - below + EPSILON && pictureFoot <= shallowest + EPSILON;
        }

        /** Whether its line holds a picture set in it. */
        boolean holdsPictures() {
            return pictureFoot > Double.NEGATIVE_INFINITY;
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
