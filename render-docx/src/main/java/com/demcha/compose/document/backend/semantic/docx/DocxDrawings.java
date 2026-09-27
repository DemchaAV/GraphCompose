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
 * a DrawingML shape anchored to the page at the layout's position, behind the text, in the order
 * the page paints them — a rectangle, a rectangle with rounded corners, an ellipse or a line,
 * filled and outlined as on the page.</p>
 *
 * <p>The shape does not move with the text. It is anchored in a paragraph written on the same
 * page, so it stays on the page it belongs to; within the page it stands where the layout put it,
 * which is where the text around it stands too as long as the text lands where the page sets it.
 * A reader who then edits the text moves the text, not the drawing — a drawing is decoration,
 * and Word treats a floating shape the same way.</p>
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
        CUSTOM(null);

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
     */
    record Shape(Kind kind, double x, double top, double width, double height, Color fill,
                 Color stroke, double strokeWidth, double radius, boolean flipH, int page,
                 List<DocumentPathSegment> path) {
        Shape {
            path = path == null ? List.of() : List.copyOf(path);
        }

        Shape(Kind kind, double x, double top, double width, double height, Color fill,
              Color stroke, double strokeWidth, double radius, boolean flipH, int page) {
            this(kind, x, top, width, height, fill, stroke, strokeWidth, radius, flipH, page, List.of());
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
     * A shape as a drawing anchored to the page, behind the text.
     *
     * @param shape the shape
     * @param id    an identifier for the drawing, unique in the document
     * @param order its place among the drawings: a later one is drawn over an earlier one
     * @return the drawing, to be added to a run
     */
    static CTDrawing drawing(Shape shape, long id, int order) {
        long cx = Units.toEMU(shape.width());
        long cy = Units.toEMU(shape.height());
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
        String xml = "<w:drawing"
                + " xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\""
                + " xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\""
                + " xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\""
                + " xmlns:wps=\"http://schemas.microsoft.com/office/word/2010/wordprocessingShape\">"
                + "<wp:anchor distT=\"0\" distB=\"0\" distL=\"0\" distR=\"0\" simplePos=\"0\""
                + " relativeHeight=\"" + stackHeight(order) + "\" behindDoc=\"1\" locked=\"0\""
                + " layoutInCell=\"0\" allowOverlap=\"1\">"
                + "<wp:simplePos x=\"0\" y=\"0\"/>"
                + "<wp:positionH relativeFrom=\"page\"><wp:posOffset>" + Units.toEMU(shape.x())
                + "</wp:posOffset></wp:positionH>"
                + "<wp:positionV relativeFrom=\"page\"><wp:posOffset>" + Units.toEMU(shape.top())
                + "</wp:posOffset></wp:positionV>"
                + "<wp:extent cx=\"" + cx + "\" cy=\"" + cy + "\"/>"
                + "<wp:effectExtent l=\"0\" t=\"0\" r=\"0\" b=\"0\"/>"
                + "<wp:wrapNone/>"
                + "<wp:docPr id=\"" + id + "\" name=\"Drawing " + (order + 1) + "\"/>"
                + "<wp:cNvGraphicFramePr/>"
                + "<a:graphic><a:graphicData uri=\"http://schemas.microsoft.com/office/word/2010/wordprocessingShape\">"
                + "<wps:wsp><wps:cNvSpPr/><wps:spPr>"
                + "<a:xfrm" + (shape.flipH() ? " flipH=\"1\"" : "") + "><a:off x=\"0\" y=\"0\"/><a:ext cx=\"" + cx
                + "\" cy=\"" + cy + "\"/></a:xfrm>"
                + geometry + fill + outline
                + "</wps:spPr><wps:bodyPr/></wps:wsp>"
                + "</a:graphicData></a:graphic>"
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
