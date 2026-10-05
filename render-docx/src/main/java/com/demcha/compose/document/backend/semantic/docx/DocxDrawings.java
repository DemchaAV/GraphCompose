package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.EllipseFragmentPayload;
import com.demcha.compose.document.layout.payloads.LineFragmentPayload;
import com.demcha.compose.document.layout.payloads.PathFragmentPayload;
import com.demcha.compose.document.layout.payloads.PolygonFragmentPayload;
import com.demcha.compose.document.layout.payloads.ShapeFragmentPayload;
import com.demcha.compose.document.layout.payloads.SideBorders;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentPathSegment;
import com.demcha.compose.engine.components.content.shape.Stroke;
import org.apache.poi.util.Units;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDrawing;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Drawing the page paints and no paragraph carries, as a shape anchored where the page draws it.
 *
 * <p>A timeline's rail and the dots on it, a badge's circle, a ring round a portrait: the export
 * wrote none of them, since none is text, and a CV came out as its words on a bare page. Each is
 * a floating DrawingML shape at the layout's position, behind the text, in the order
 * the page paints them — a rectangle, a rectangle with rounded corners, an ellipse or a line,
 * filled and outlined as on the page.</p>
 *
 * <p>A shape standing beside a paragraph's text is anchored in that paragraph and placed down from
 * its top ({@link #drawingInParagraph}, or {@link #drawingInCell} for one in a table cell): a
 * reader who edits the text above moves the paragraph, and the shape with it. A shape beside no
 * paragraph is placed from the page's edges and stays there whatever the text does. Which is
 * which, {@link DocxDrawingAnchors} decides. A drawing that is all a table cell holds is anchored
 * in that cell ({@link #drawingInCell}), and moves with its row.</p>
 *
 * <p>A picture a badge holds is drawn the same way, over the badge: written in the flow, as a
 * paragraph of its own, it stood on its own line above the title beside the badge instead of in
 * the middle of the circle.</p>
 */
final class DocxDrawings {

    private DocxDrawings() {
    }

    /** What a shape is drawn as. */
    enum Kind {
        RECT("rect"),
        ROUND_RECT("roundRect"),
        ELLIPSE("ellipse"),
        LINE("line"),
        /** A path or a polygon, drawn as custom geometry (see {@link DocxCustomGeometry}). */
        CUSTOM(null),
        /** A picture the document holds, drawn over its box. */
        PICTURE(null);

        private final String preset;

        Kind(String preset) {
            this.preset = preset;
        }
    }

    /**
     * One shape, measured from the page's top-left corner.
     *
     * @param kind        its geometry
     * @param x           from the page's left edge, in points
     * @param top         from the page's top edge, in points
     * @param width       in points; 0 for a vertical line
     * @param height      in points; 0 for a horizontal line
     * @param fill        its fill, alpha included, or {@code null}
     * @param stroke      its outline colour, or {@code null}
     * @param strokeWidth its outline width, in points
     * @param radius      a rounded rectangle's corner radius, in points
     * @param flipH       whether a line runs from its top-right corner
     * @param page        the page the layout drew it on, from 0
     * @param path        a custom shape's outline in its box's unit square, empty for a preset
     * @param front       whether it is drawn in front of the text rather than behind it
     * @param picture     a picture's relationship in the document part, {@code null} for a shape
     * @param text        a paragraph the shape holds, as {@code w:p} markup — a badge's initials,
     *                    or a line of text laid over the flow — or {@code null}
     * @param textAtTop   whether the paragraph is set from the box's top-left corner, as the page
     *                    sets a line in its box, rather than centred in the shape
     */
    record Shape(Kind kind, double x, double top, double width, double height, Color fill,
                 Color stroke, double strokeWidth, double radius, boolean flipH, int page,
                 List<DocumentPathSegment> path, boolean front, String picture, String text, boolean textAtTop) {
        Shape {
            path = path == null ? List.of() : List.copyOf(path);
        }

        Shape(Kind kind, double x, double top, double width, double height, Color fill,
              Color stroke, double strokeWidth, double radius, boolean flipH, int page,
              List<DocumentPathSegment> path, boolean front, String picture) {
            this(kind, x, top, width, height, fill, stroke, strokeWidth, radius, flipH, page, path, front, picture,
                    null, false);
        }

        Shape(Kind kind, double x, double top, double width, double height, Color fill,
              Color stroke, double strokeWidth, double radius, boolean flipH, int page,
              List<DocumentPathSegment> path, boolean front) {
            this(kind, x, top, width, height, fill, stroke, strokeWidth, radius, flipH, page, path, front, null);
        }

        Shape(Kind kind, double x, double top, double width, double height, Color fill,
              Color stroke, double strokeWidth, double radius, boolean flipH, int page,
              List<DocumentPathSegment> path) {
            this(kind, x, top, width, height, fill, stroke, strokeWidth, radius, flipH, page, path, false);
        }

        Shape(Kind kind, double x, double top, double width, double height, Color fill,
              Color stroke, double strokeWidth, double radius, boolean flipH, int page) {
            this(kind, x, top, width, height, fill, stroke, strokeWidth, radius, flipH, page, List.of());
        }

        /** The same shape in front of the text rather than behind it. */
        Shape inFront() {
            return new Shape(kind, x, top, width, height, fill, stroke, strokeWidth, radius, flipH, page, path, true,
                    picture, text, textAtTop);
        }

        /**
         * The same shape holding a paragraph centred in it.
         *
         * @param paragraph the paragraph, as {@code w:p} markup
         */
        Shape holding(String paragraph) {
            return new Shape(kind, x, top, width, height, fill, stroke, strokeWidth, radius, flipH, page, path, front,
                    picture, paragraph, false);
        }

        /**
         * A box holding a paragraph set from its top-left corner, with nothing drawn round it: a
         * line of text laid over the flow, where the page sets it. It stands in front of the
         * text, where no shading and no shape behind the text covers it; drawing nothing, it
         * covers nothing either.
         *
         * @param x         from the page's left edge, in points
         * @param top       from the page's top edge, in points
         * @param width     in points
         * @param height    in points
         * @param page      the page the layout set it on, from 0
         * @param paragraph the paragraph, as {@code w:p} markup
         */
        static Shape textBox(double x, double top, double width, double height, int page, String paragraph) {
            return new Shape(Kind.RECT, x, top, width, height, null, null, 0, 0, false, page, List.of(), true,
                    null, paragraph, true);
        }

        /**
         * A picture the document holds, measured from the page's top-left corner.
         *
         * @param relationship the picture's relationship in the document part
         */
        static Shape picture(double x, double top, double width, double height, int page, String relationship) {
            return new Shape(Kind.PICTURE, x, top, width, height, null, null, 0, 0, false, page, List.of(), false,
                    relationship);
        }
    }

    /**
     * The shapes a fragment paints, empty when it paints nothing a shape can show — neither
     * fill nor outline, or a payload this does not draw.
     *
     * @param fragment   a placed fragment
     * @param pageHeight the page's height, in points
     */
    static List<Shape> of(PlacedFragment fragment, double pageHeight) {
        double top = pageHeight - fragment.y() - fragment.height();
        List<Shape> shapes = new ArrayList<>();
        int page = fragment.pageIndex();
        if (fragment.payload() instanceof EllipseFragmentPayload ellipse) {
            add(shapes, visible(new Shape(Kind.ELLIPSE, fragment.x(), top, fragment.width(), fragment.height(),
                    ellipse.fillColor(), colourOf(ellipse.stroke()), widthOf(ellipse.stroke()), 0, false, page)));
        } else if (fragment.payload() instanceof ShapeFragmentPayload shape) {
            double radius = radiusOf(shape.cornerRadius());
            add(shapes, visible(new Shape(radius > 0 ? Kind.ROUND_RECT : Kind.RECT, fragment.x(), top,
                    fragment.width(), fragment.height(), shape.fillColor(), colourOf(shape.stroke()),
                    widthOf(shape.stroke()), radius, false, page)));
            // A side border is a line along that edge, as the page strokes it — a timeline's rail
            // is a box with only its left side drawn.
            SideBorders sides = shape.sideBorders();
            if (sides != null && sides.hasAny()) {
                double x = fragment.x();
                double width = fragment.width();
                double height = fragment.height();
                add(shapes, edge(sides.top(), x, top, width, 0, page));
                add(shapes, edge(sides.right(), x + width, top, 0, height, page));
                add(shapes, edge(sides.bottom(), x, top + height, width, 0, page));
                add(shapes, edge(sides.left(), x, top, 0, height, page));
            }
        } else if (fragment.payload() instanceof LineFragmentPayload line) {
            // The line's ends are in the fragment's box, measured up from its bottom-left corner.
            double x1 = fragment.x() + line.startX();
            double x2 = fragment.x() + line.endX();
            double y1 = pageHeight - (fragment.y() + line.startY());
            double y2 = pageHeight - (fragment.y() + line.endY());
            // Running from the upper-left to the lower-right is the preset's own direction; the
            // other diagonal flips it.
            boolean flipH = (x2 - x1) * (y2 - y1) < 0;
            add(shapes, visible(new Shape(Kind.LINE, Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1),
                    Math.abs(y2 - y1), null, colourOf(line.stroke()), widthOf(line.stroke()), 0, flipH, page)));
        } else if (fragment.payload() instanceof PathFragmentPayload path) {
            add(shapes, visible(new Shape(Kind.CUSTOM, fragment.x(), top, fragment.width(), fragment.height(),
                    path.fillColor(), colourOf(path.stroke()), widthOf(path.stroke()), 0, false, page,
                    path.segments())));
        } else if (fragment.payload() instanceof PolygonFragmentPayload polygon) {
            add(shapes, visible(new Shape(Kind.CUSTOM, fragment.x(), top, fragment.width(), fragment.height(),
                    polygon.fillColor(), colourOf(polygon.stroke()), widthOf(polygon.stroke()), 0, false, page,
                    DocxCustomGeometry.ofPolygon(polygon.points()))));
        }
        return shapes;
    }

    private static Shape edge(Stroke stroke, double x, double top, double width, double height, int page) {
        return stroke == null ? null
                : visible(new Shape(Kind.LINE, x, top, width, height, null, colourOf(stroke), widthOf(stroke),
                        0, false, page));
    }

    private static void add(List<Shape> shapes, Shape shape) {
        if (shape != null) {
            shapes.add(shape);
        }
    }

    private static Shape visible(Shape shape) {
        boolean filled = shape.fill() != null && shape.fill().getAlpha() > 0;
        boolean outlined = shape.stroke() != null && shape.stroke().getAlpha() > 0 && shape.strokeWidth() > 0;
        if (!filled && !outlined) {
            return null;
        }
        boolean hasExtent = shape.kind() == Kind.LINE
                ? shape.width() > 0 || shape.height() > 0
                : shape.width() > 0 && shape.height() > 0;
        if (!hasExtent || shape.kind() == Kind.CUSTOM && shape.path().isEmpty()) {
            return null;
        }
        return shape;
    }

    private static Color colourOf(Stroke stroke) {
        return stroke == null || stroke.strokeColor() == null ? null : stroke.strokeColor().color();
    }

    private static double widthOf(Stroke stroke) {
        return stroke == null ? 0 : stroke.width();
    }

    private static double radiusOf(DocumentCornerRadius radius) {
        if (radius == null || radius.isZero()) {
            return 0;
        }
        return Math.max(Math.max(radius.topLeft(), radius.topRight()),
                Math.max(radius.bottomRight(), radius.bottomLeft()));
    }

    /**
     * Where a drawing stands among the ones behind the text: its place counted up from the
     * height Word gives the first drawing of a document, 1024 apart, as Word numbers them.
     * LibreOffice stacks a header's shapes and the body's together by these heights, so they
     * stand above the page backgrounds (see {@link DocxPageBackgrounds}), which a timeline's
     * rail would otherwise disappear under.
     *
     * @param order the drawing's place among the body's: a later one is drawn over an earlier one
     * @return the value for {@code wp:anchor/@relativeHeight}
     */
    static long stackHeight(int order) {
        return 251_658_240L + (order + 1L) * 1024L;
    }

    /**
     * A shape or a picture as a drawing anchored to the page, behind the text or in front of it.
     *
     * @param shape the shape
     * @param id    an identifier for the drawing, unique in the document
     * @param order its place among the drawings: a later one is drawn over an earlier one
     * @return the drawing, to be added to a run
     */
    static CTDrawing drawing(Shape shape, long id, int order) {
        return drawing(shape, id, order, null);
    }

    /**
     * A shape or a picture as a drawing anchored in a table cell's paragraph, placed from the
     * cell's text column and that paragraph's top, so it moves with the row wherever Word sets
     * it.
     *
     * @param shape  the shape
     * @param id     an identifier for the drawing, unique in the document
     * @param order  its place among the drawings: a later one is drawn over an earlier one
     * @param origin where the page puts the cell's text column and the paragraph's top, in
     *               points from the page's left and top edges
     * @return the drawing, to be added to a run of that paragraph
     */
    static CTDrawing drawingInCell(Shape shape, long id, int order, CellOrigin origin) {
        return drawing(shape, id, order, origin);
    }

    /**
     * A shape or a picture as a drawing anchored in a paragraph of the body, placed across from
     * the page's left edge and down from that paragraph's top, so it moves with the paragraph
     * wherever Word sets it.
     *
     * @param shape        the shape
     * @param id           an identifier for the drawing, unique in the document
     * @param order        its place among the drawings: a later one is drawn over an earlier one
     * @param paragraphTop where the page puts the paragraph's top, the space above its first line
     *                     included, in points from the page's top edge
     * @return the drawing, to be added to a run of that paragraph
     */
    static CTDrawing drawingInParagraph(Shape shape, long id, int order, double paragraphTop) {
        return drawing(shape, id, order, new CellOrigin(Double.NaN, paragraphTop));
    }

    /**
     * Where the page puts a cell's text column and a paragraph's top in it.
     *
     * @param x   from the page's left edge, in points; NaN for a paragraph of the body, whose
     *            shapes are placed across from the page's left edge
     * @param top from the page's top edge, in points
     */
    record CellOrigin(double x, double top) {
    }

    private static CTDrawing drawing(Shape shape, long id, int order, CellOrigin origin) {
        long cx = Units.toEMU(shape.width());
        long cy = Units.toEMU(shape.height());
        boolean picture = shape.kind() == Kind.PICTURE;
        String graphic = picture ? pictureGraphic(shape, id, cx, cy) : shapeGraphic(shape, cx, cy);
        boolean inCell = origin != null && !Double.isNaN(origin.x());
        String xml = "<w:drawing"
                + " xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\""
                + " xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\""
                + " xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\""
                + (picture
                   ? " xmlns:pic=\"http://schemas.openxmlformats.org/drawingml/2006/picture\""
                     + " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                   : " xmlns:wps=\"http://schemas.microsoft.com/office/word/2010/wordprocessingShape\">")
                + "<wp:anchor distT=\"0\" distB=\"0\" distL=\"0\" distR=\"0\" simplePos=\"0\""
                + " relativeHeight=\"" + stackHeight(order) + "\" behindDoc=\"" + (shape.front() ? 0 : 1)
                + "\" locked=\"0\""
                + " layoutInCell=\"" + (inCell ? 1 : 0) + "\" allowOverlap=\"1\">"
                + "<wp:simplePos x=\"0\" y=\"0\"/>"
                + "<wp:positionH relativeFrom=\"" + (inCell ? "column" : "page") + "\"><wp:posOffset>"
                + Units.toEMU(inCell ? shape.x() - origin.x() : shape.x())
                + "</wp:posOffset></wp:positionH>"
                + "<wp:positionV relativeFrom=\"" + (origin == null ? "page" : "paragraph") + "\"><wp:posOffset>"
                + Units.toEMU(origin == null ? shape.top() : shape.top() - origin.top())
                + "</wp:posOffset></wp:positionV>"
                + "<wp:extent cx=\"" + cx + "\" cy=\"" + cy + "\"/>"
                + "<wp:effectExtent l=\"0\" t=\"0\" r=\"0\" b=\"0\"/>"
                + "<wp:wrapNone/>"
                + "<wp:docPr id=\"" + id + "\" name=\"Drawing " + (order + 1) + "\"/>"
                + "<wp:cNvGraphicFramePr/>"
                + graphic
                + "</wp:anchor></w:drawing>";
        // Parsed as the drawing's content rather than a document around it: set onto a run's
        // new w:drawing, a parsed document would nest a second w:drawing inside the first, and
        // the editors draw nothing.
        XmlOptions options = new XmlOptions();
        options.setLoadReplaceDocumentElement(null);
        try {
            return CTDrawing.Factory.parse(xml, options);
        } catch (XmlException failure) {
            throw new IllegalStateException("could not build a drawing", failure);
        }
    }

    /** A picture filling its box, as Word writes one. */
    private static String pictureGraphic(Shape shape, long id, long cx, long cy) {
        return "<a:graphic><a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">"
                + "<pic:pic><pic:nvPicPr><pic:cNvPr id=\"" + id + "\" name=\"Picture " + id + "\"/>"
                + "<pic:cNvPicPr><a:picLocks noChangeAspect=\"1\"/></pic:cNvPicPr></pic:nvPicPr>"
                + "<pic:blipFill><a:blip r:embed=\"" + shape.picture() + "\"/>"
                + "<a:stretch><a:fillRect/></a:stretch></pic:blipFill>"
                + "<pic:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"" + cx + "\" cy=\"" + cy + "\"/></a:xfrm>"
                + "<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></pic:spPr>"
                + "</pic:pic></a:graphicData></a:graphic>";
    }

    /** A shape filled and outlined as on the page. */
    private static String shapeGraphic(Shape shape, long cx, long cy) {
        boolean filled = shape.fill() != null && shape.fill().getAlpha() > 0;
        String geometry = shape.kind() == Kind.CUSTOM
                ? DocxCustomGeometry.xml(shape.path(), filled)
                : "<a:prstGeom prst=\"" + shape.kind().preset + "\"><a:avLst>"
                  + (shape.kind() == Kind.ROUND_RECT ? roundness(shape) : "")
                  + "</a:avLst></a:prstGeom>";
        String fill = filled
                ? "<a:solidFill>" + colour(shape.fill()) + "</a:solidFill>"
                : "<a:noFill/>";
        String outline = shape.stroke() == null || shape.stroke().getAlpha() == 0 || !(shape.strokeWidth() > 0)
                ? "<a:ln><a:noFill/></a:ln>"
                : "<a:ln w=\"" + Units.toEMU(shape.strokeWidth()) + "\"><a:solidFill>"
                  + colour(shape.stroke()) + "</a:solidFill></a:ln>";
        return "<a:graphic><a:graphicData uri=\"http://schemas.microsoft.com/office/word/2010/wordprocessingShape\">"
                + "<wps:wsp><wps:cNvSpPr/><wps:spPr>"
                + "<a:xfrm" + (shape.flipH() ? " flipH=\"1\"" : "") + "><a:off x=\"0\" y=\"0\"/><a:ext cx=\"" + cx
                + "\" cy=\"" + cy + "\"/></a:xfrm>"
                + geometry + fill + outline
                + "</wps:spPr>" + textOf(shape) + "</wps:wsp>"
                + "</a:graphicData></a:graphic>";
    }

    /**
     * What a shape holds in its text body: a paragraph centred across and down, with no inset,
     * so a badge's initials stand in its middle as the page sets them; or nothing.
     *
     * <p>The text wraps in the shape's box, which the initials fit. Left unwrapped, Word sized
     * the shape to its text: {@code ObsidianInvoice}'s 35.5pt disc came out 16.9pt wide round its
     * "K".</p>
     *
     * <p>A preset wraps its text in a rectangle of its own inside the shape — an ellipse the
     * square inscribed in it, a rounded rectangle its box less part of each corner — where the
     * page sets the initials across the outline's whole width. Word broke "MWM" in a 36pt disc
     * after "MW". The insets reach out by as much, so the text wraps at the shape's edges.</p>
     */
    private static String textOf(Shape shape) {
        if (shape.text() == null) {
            return "<wps:bodyPr/>";
        }
        if (shape.textAtTop()) {
            // Set from the corner, with no inset, as the page sets a line in its box.
            return "<wps:txbx><w:txbxContent>" + shape.text() + "</w:txbxContent></wps:txbx>"
                   + "<wps:bodyPr rot=\"0\" vert=\"horz\" wrap=\"square\" lIns=\"0\" tIns=\"0\" rIns=\"0\""
                   + " bIns=\"0\" anchor=\"t\" anchorCtr=\"0\"><a:noAutofit/></wps:bodyPr>";
        }
        double across = 0;
        double down = 0;
        if (shape.kind() == Kind.ELLIPSE) {
            // The inscribed rectangle stands in by (1 - cos 45°) / 2 of each side.
            across = shape.width() * (1 - Math.sqrt(0.5)) / 2;
            down = shape.height() * (1 - Math.sqrt(0.5)) / 2;
        } else if (shape.kind() == Kind.ROUND_RECT) {
            // By the corner's radius, as the preset caps it, times 1 - cos 45°.
            double corner = Math.min(shape.radius(), Math.min(shape.width(), shape.height()) / 2);
            across = corner * (1 - Math.sqrt(0.5));
            down = across;
        }
        String sides = " lIns=\"" + -Units.toEMU(across) + "\" tIns=\"" + -Units.toEMU(down)
                       + "\" rIns=\"" + -Units.toEMU(across) + "\" bIns=\"" + -Units.toEMU(down) + "\"";
        return "<wps:txbx><w:txbxContent>" + shape.text() + "</w:txbxContent></wps:txbx>"
               + "<wps:bodyPr rot=\"0\" vert=\"horz\" wrap=\"square\"" + sides
               + " anchor=\"ctr\" anchorCtr=\"0\"><a:noAutofit/></wps:bodyPr>";
    }

    /** A rounded rectangle's corner, as the preset states it: a share of its shorter side. */
    private static String roundness(Shape shape) {
        double shorter = Math.min(shape.width(), shape.height());
        long share = shorter > 0 ? Math.min(50_000, Math.round(shape.radius() / shorter * 100_000)) : 0;
        return "<a:gd name=\"adj\" fmla=\"val " + share + "\"/>";
    }

    private static String colour(Color color) {
        String rgb = String.format(Locale.ROOT, "%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
        String alpha = color.getAlpha() == 255 ? ""
                : "<a:alpha val=\"" + Math.round(color.getAlpha() * 100000.0 / 255.0) + "\"/>";
        return "<a:srgbClr val=\"" + rgb + "\">" + alpha + "</a:srgbClr>";
    }
}
