package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.style.DocumentPathSegment;
import com.demcha.compose.document.style.ShapePoint;

import java.util.ArrayList;
import java.util.List;

/**
 * An outline the presets have no name for — a path, a polygon — as DrawingML custom geometry.
 *
 * <p>The engine states such an outline in its box's own unit square, {@code x} from the left
 * and {@code y} up from the bottom. DrawingML states it in a coordinate space of its own that
 * it stretches over the shape's extent, {@code y} down from the top; the space here is
 * {@value #SIZE} units a side, so a point lands on the same share of the box, and precision
 * is a hundred-thousandth of it.</p>
 */
final class DocxCustomGeometry {

    /** The side of the coordinate space the path is written in. */
    static final long SIZE = 100_000;

    private DocxCustomGeometry() {
    }

    /**
     * A polygon's vertex ring as a closed path.
     *
     * @param points the ring, in the unit box
     * @return the path, empty when the ring has fewer than two points
     */
    static List<DocumentPathSegment> ofPolygon(List<ShapePoint> points) {
        if (points.size() < 2) {
            return List.of();
        }
        List<DocumentPathSegment> path = new ArrayList<>(points.size() + 1);
        path.add(DocumentPathSegment.moveTo(points.get(0).x(), points.get(0).y()));
        for (int index = 1; index < points.size(); index++) {
            path.add(DocumentPathSegment.lineTo(points.get(index).x(), points.get(index).y()));
        }
        path.add(DocumentPathSegment.close());
        return path;
    }

    /**
     * The {@code a:custGeom} element drawing a path.
     *
     * @param path   the path, in the unit box
     * @param filled whether the path is filled, so that an open subpath is closed for the fill
     *               as the page closes it
     * @return the element
     */
    static String xml(List<DocumentPathSegment> path, boolean filled) {
        StringBuilder commands = new StringBuilder();
        for (DocumentPathSegment segment : path) {
            if (segment instanceof DocumentPathSegment.MoveTo move) {
                commands.append("<a:moveTo>").append(point(move.x(), move.y())).append("</a:moveTo>");
            } else if (segment instanceof DocumentPathSegment.LineTo line) {
                commands.append("<a:lnTo>").append(point(line.x(), line.y())).append("</a:lnTo>");
            } else if (segment instanceof DocumentPathSegment.CubicTo curve) {
                commands.append("<a:cubicBezTo>")
                        .append(point(curve.control1X(), curve.control1Y()))
                        .append(point(curve.control2X(), curve.control2Y()))
                        .append(point(curve.x(), curve.y()))
                        .append("</a:cubicBezTo>");
            } else {
                commands.append("<a:close/>");
            }
        }
        return "<a:custGeom><a:avLst/><a:gdLst/><a:ahLst/><a:cxnLst/>"
                + "<a:rect l=\"0\" t=\"0\" r=\"r\" b=\"b\"/>"
                + "<a:pathLst><a:path w=\"" + SIZE + "\" h=\"" + SIZE + "\""
                + (filled ? "" : " fill=\"none\"") + ">"
                + commands
                + "</a:path></a:pathLst></a:custGeom>";
    }

    private static String point(double x, double y) {
        // Up from the bottom on the page, down from the top in DrawingML.
        return "<a:pt x=\"" + Math.round(x * SIZE) + "\" y=\"" + Math.round((1 - y) * SIZE) + "\"/>";
    }
}
