package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentLineCap;
import com.demcha.compose.document.style.DocumentLineJoin;
import com.demcha.compose.document.style.DocumentPathSegment;
import com.demcha.compose.document.style.ShapePoint;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Where a drawing's paint runs, as the PDF paints it: the outlines of what it fills and strokes,
 * built as the page builds them and measured on the page with y up (see {@link DocxClipInk}).
 *
 * <p>A stroke is the area {@link BasicStroke} makes of it — with its cap, its join and the PDF's
 * miter limit of 10, as {@code DocxShapePictures} strokes an inline shape — rather than geometry
 * of its own. Outlines are flattened, and points along them stand no further apart than
 * {@link #STEP}, so a clip that is not convex is measured along each edge too.</p>
 */
final class DocxInkOutline {

    /** The PDF's default miter limit, which the page strokes every join with. */
    private static final float MITER_LIMIT = 10f;

    /** How far a flattened curve may stray from the true one, in points. */
    private static final double FLATNESS = 0.01;

    /** The longest step between two points measured along an edge. */
    private static final double STEP = 2;

    /** The control distance of a quarter circle's Bézier arc, as the PDF draws a rounded corner. */
    private static final double ARC = 0.552284749831;

    private DocxInkOutline() {
    }

    /** Points along the edge of a filled shape. */
    static void filled(List<double[]> ink, Shape shape) {
        for (List<double[]> ring : rings(shape)) {
            for (int index = 0; index < ring.size(); index++) {
                edge(ink, ring.get(index), ring.get((index + 1) % ring.size()));
            }
        }
    }

    /** Points along the edge of the area a stroke of {@code width} paints along a shape. */
    static void stroked(List<double[]> ink, Shape shape, double width, DocumentLineCap cap, DocumentLineJoin join) {
        if (width > 0) {
            filled(ink, new BasicStroke((float) width, capOf(cap), joinOf(join), MITER_LIMIT).createStrokedShape(shape));
        }
    }

    /**
     * A shape's sub-paths, flattened, each as the points it runs through; a sub-path's close
     * is implied.
     */
    static List<List<double[]>> rings(Shape shape) {
        List<List<double[]>> rings = new ArrayList<>();
        List<double[]> current = null;
        double[] point = new double[6];
        for (PathIterator it = shape.getPathIterator(null, FLATNESS); !it.isDone(); it.next()) {
            int segment = it.currentSegment(point);
            if (segment == PathIterator.SEG_MOVETO) {
                current = new ArrayList<>();
                rings.add(current);
                current.add(new double[]{point[0], point[1]});
            } else if (segment == PathIterator.SEG_LINETO && current != null) {
                current.add(new double[]{point[0], point[1]});
            }
        }
        return rings;
    }

    /** A box, its corners rounded as the PDF rounds them, each radius clamped to half its smaller side. */
    static Shape box(PlacedFragment fragment, DocumentCornerRadius radius) {
        double x = fragment.x();
        double y = fragment.y();
        double right = x + fragment.width();
        double top = y + fragment.height();
        double half = Math.min(fragment.width(), fragment.height()) / 2;
        double topLeft = clamp(radius.topLeft(), half);
        double topRight = clamp(radius.topRight(), half);
        double bottomRight = clamp(radius.bottomRight(), half);
        double bottomLeft = clamp(radius.bottomLeft(), half);
        Path2D.Double path = new Path2D.Double();
        path.moveTo(x + topLeft, top);
        path.lineTo(right - topRight, top);
        path.curveTo(right - topRight + topRight * ARC, top, right, top - topRight + topRight * ARC,
                right, top - topRight);
        path.lineTo(right, y + bottomRight);
        path.curveTo(right, y + bottomRight - bottomRight * ARC, right - bottomRight + bottomRight * ARC, y,
                right - bottomRight, y);
        path.lineTo(x + bottomLeft, y);
        path.curveTo(x + bottomLeft - bottomLeft * ARC, y, x, y + bottomLeft - bottomLeft * ARC, x, y + bottomLeft);
        path.lineTo(x, top - topLeft);
        path.curveTo(x, top - topLeft + topLeft * ARC, x + topLeft - topLeft * ARC, top, x + topLeft, top);
        path.closePath();
        return path;
    }

    static Shape box(double left, double bottom, double right, double top) {
        return new Rectangle2D.Double(left, bottom, right - left, top - bottom);
    }

    static Shape ellipse(double left, double bottom, double width, double height) {
        return new Ellipse2D.Double(left, bottom, width, height);
    }

    static Shape line(double x1, double y1, double x2, double y2) {
        return new Line2D.Double(x1, y1, x2, y2);
    }

    /** A polygon's normalized points, set in a box and closed. */
    static Shape polygon(List<ShapePoint> points, PlacedFragment box) {
        Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
        for (ShapePoint point : points) {
            double x = box.x() + point.x() * box.width();
            double y = box.y() + point.y() * box.height();
            if (path.getCurrentPoint() == null) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        if (path.getCurrentPoint() != null) {
            path.closePath();
        }
        return path;
    }

    /** A path's normalized segments, set in a box; what comes before its first move draws nothing. */
    static Shape path(List<DocumentPathSegment> segments, PlacedFragment box) {
        Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
        for (DocumentPathSegment segment : segments) {
            if (segment instanceof DocumentPathSegment.MoveTo move) {
                path.moveTo(xOf(box, move.x()), yOf(box, move.y()));
            } else if (path.getCurrentPoint() == null) {
                continue;
            } else if (segment instanceof DocumentPathSegment.LineTo line) {
                path.lineTo(xOf(box, line.x()), yOf(box, line.y()));
            } else if (segment instanceof DocumentPathSegment.CubicTo cubic) {
                path.curveTo(xOf(box, cubic.control1X()), yOf(box, cubic.control1Y()),
                        xOf(box, cubic.control2X()), yOf(box, cubic.control2Y()),
                        xOf(box, cubic.x()), yOf(box, cubic.y()));
            } else if (segment instanceof DocumentPathSegment.Close) {
                path.closePath();
            }
        }
        return path;
    }

    private static int capOf(DocumentLineCap cap) {
        if (cap == DocumentLineCap.ROUND) {
            return BasicStroke.CAP_ROUND;
        }
        return cap == DocumentLineCap.SQUARE ? BasicStroke.CAP_SQUARE : BasicStroke.CAP_BUTT;
    }

    private static int joinOf(DocumentLineJoin join) {
        if (join == DocumentLineJoin.ROUND) {
            return BasicStroke.JOIN_ROUND;
        }
        return join == DocumentLineJoin.BEVEL ? BasicStroke.JOIN_BEVEL : BasicStroke.JOIN_MITER;
    }

    /** The points from {@code from} towards {@code to}, no further apart than {@link #STEP}. */
    private static void edge(List<double[]> into, double[] from, double[] to) {
        int steps = Math.max(1, (int) Math.ceil(Math.hypot(to[0] - from[0], to[1] - from[1]) / STEP));
        for (int step = 0; step < steps; step++) {
            double t = (double) step / steps;
            into.add(new double[]{from[0] + (to[0] - from[0]) * t, from[1] + (to[1] - from[1]) * t});
        }
    }

    private static double clamp(double radius, double half) {
        return Math.max(0, Math.min(radius, half));
    }

    private static double xOf(PlacedFragment box, double x) {
        return box.x() + x * box.width();
    }

    private static double yOf(PlacedFragment box, double y) {
        return box.y() + y * box.height();
    }
}
