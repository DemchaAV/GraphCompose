package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.backend.fixed.pdf.handlers.InlineSvgRasters;
import com.demcha.compose.document.layout.payloads.ResolvedSvgLayer;
import com.demcha.compose.document.node.InlineShapeRun;
import com.demcha.compose.document.node.ShapeLayer;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentPathSegment;
import com.demcha.compose.document.style.ShapeOutline;
import com.demcha.compose.document.style.ShapePoint;
import com.demcha.compose.engine.components.content.shape.Stroke;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * An inline shape — a dot, an arrow, a chevron, a checkbox — as a transparent picture.
 *
 * <p>Word has no drawing in a line of text, so a shape among the words is written the way an
 * SVG icon is: drawn into a picture by the raster the icons use ({@link InlineSvgRasters}),
 * from the same outline, fill and stroke the page draws. Each outline becomes normalised
 * path segments, and each layer is centred in the run's box, as the page centres a checkmark
 * inside its checkbox.</p>
 *
 * <p>The page draws a stroke centred on the outline, so part of it lies outside the run's
 * box — half its width past an edge, more at a sharp corner — and the pixels that smooth an
 * edge lie just past that. A picture is clipped to its own edges, so it is larger on each
 * side by as far as its ink reaches there ({@link #overhang}) and a pixel more; the caller
 * lowers it by what it reaches below, keeping the outline where the page has it.</p>
 */
final class DocxShapePictures {

    /** Where a quarter circle's control points sit, as a fraction of its radius. */
    private static final double KAPPA = 0.5522847498;

    /**
     * Empty room around the drawing, in points — a pixel of the raster, which draws four a
     * point. A shape reaches its box's edges, and the raster clips to them, so the shaded
     * pixels that smooth an edge were cut off: measured in LibreOffice, a 10pt chevron came
     * out 9.96pt tall and a dot's rim flattened where it met the picture's edge.
     */
    static final double EDGE = 0.25;

    private DocxShapePictures() {
    }

    /**
     * An inline shape drawn as a picture.
     *
     * @param png    the picture
     * @param width  its width in points: the run's, and the overhang either side
     * @param height its height in points: the run's, and the overhang above and below
     * @param below  how far the picture reaches below the run's box, in points — the ink's
     *               reach there and {@link #EDGE}; the caller lowers it by this much
     */
    record Picture(byte[] png, double width, double height, double below) {
    }

    /**
     * How far ink reaches past the run's box on each side, in points, never less than zero.
     *
     * @param left   past the left edge
     * @param right  past the right edge
     * @param top    past the top edge
     * @param bottom past the bottom edge
     */
    record Overhang(double left, double right, double top, double bottom) {

        static final Overhang NONE = new Overhang(0, 0, 0, 0);

        Overhang max(Overhang other) {
            return new Overhang(Math.max(left, other.left), Math.max(right, other.right),
                    Math.max(top, other.top), Math.max(bottom, other.bottom));
        }

        double farthest() {
            return Math.max(Math.max(left, right), Math.max(top, bottom));
        }
    }

    /**
     * Draws an inline shape as a picture.
     *
     * @param run the shape
     * @return the picture: the run's box, its ink's overhang on each side, and a pixel
     */
    static Picture of(InlineShapeRun run) {
        double width = run.width();
        double height = run.height();
        Overhang ink = Overhang.NONE;
        for (ShapeLayer layer : run.layers()) {
            ink = ink.max(overhang(layer, width, height));
        }
        double left = ink.left() + EDGE;
        double right = ink.right() + EDGE;
        double top = ink.top() + EDGE;
        double below = ink.bottom() + EDGE;
        double boxWidth = width + left + right;
        double boxHeight = height + top + below;
        List<ResolvedSvgLayer> layers = new ArrayList<>(run.layers().size());
        for (ShapeLayer layer : run.layers()) {
            ShapeOutline outline = layer.outline();
            double layerLeft = left + (width - outline.width()) / 2.0;
            double layerBottom = below + (height - outline.height()) / 2.0;
            List<DocumentPathSegment> segments = place(unitSegments(outline),
                    layerLeft / boxWidth, layerBottom / boxHeight,
                    outline.width() / boxWidth, outline.height() / boxHeight);
            Stroke stroke = layer.stroke() == null ? null
                    : new Stroke(layer.stroke().color().color(), layer.stroke().width());
            layers.add(new ResolvedSvgLayer(segments,
                    layer.fill() == null ? null : layer.fill().color(),
                    null, stroke, null, null, null, null, null));
        }
        byte[] png = InlineSvgRasters.rasterize(layers, boxWidth, boxHeight).getBytes();
        return new Picture(png, boxWidth, boxHeight, below);
    }

    /**
     * How far a layer's ink reaches past the run's box on each side, in points.
     *
     * <p>Measured on the ink the raster draws rather than worked out: half a stroke past a
     * straight edge, but further at a sharp corner, where the page's miter join runs out to
     * the corner's point — a stroked star's tip reaches over twice as far — and wherever a
     * path's curves leave the box. Each side is its own: a stroked arrow's tip reaches three
     * points past its box on the right and under one on the left, and a picture as wide on
     * both sides would push the next word over and stand higher than the page puts it.</p>
     */
    static Overhang overhang(ShapeLayer layer, double width, double height) {
        ShapeOutline outline = layer.outline();
        Shape ink = InlineSvgRasters.path(unitSegments(outline),
                new Rectangle2D.Double(0, 0, outline.width(), outline.height()));
        if (layer.stroke() != null && layer.stroke().width() > 0) {
            // The join, cap and miter limit the raster strokes with.
            ink = new BasicStroke((float) layer.stroke().width(), BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER, 10.0f).createStrokedShape(ink);
        }
        Rectangle2D bounds = inkBounds(ink);
        // The layer is centred in the run's box; the path's y grows downwards.
        double left = (width - outline.width()) / 2.0;
        double top = (height - outline.height()) / 2.0;
        return new Overhang(
                Math.max(0, -(left + bounds.getMinX())),
                Math.max(0, left + bounds.getMaxX() - width),
                Math.max(0, -(top + bounds.getMinY())),
                Math.max(0, top + bounds.getMaxY() - height));
    }

    /**
     * The bounds of what a shape paints, its curves flattened first. {@code getBounds2D}
     * counts a curve's control points on some Java releases and not on others, so the same
     * path measured a picture several points larger on Java 17 than on Java 24.
     */
    private static Rectangle2D inkBounds(Shape shape) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double[] point = new double[6];
        for (PathIterator it = shape.getPathIterator(null, FLATNESS); !it.isDone(); it.next()) {
            if (it.currentSegment(point) != PathIterator.SEG_CLOSE) {
                minX = Math.min(minX, point[0]);
                minY = Math.min(minY, point[1]);
                maxX = Math.max(maxX, point[0]);
                maxY = Math.max(maxY, point[1]);
            }
        }
        return minX > maxX ? new Rectangle2D.Double() : new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
    }

    /** How far a flattened curve may stray from the true one, in points. */
    private static final double FLATNESS = 0.01;

    /**
     * An outline as path segments in the unit box, y growing upwards — the convention of a
     * polygon's points and a path's segments, and of the raster that draws them.
     */
    static List<DocumentPathSegment> unitSegments(ShapeOutline outline) {
        if (outline instanceof ShapeOutline.Rectangle) {
            return List.of(
                    DocumentPathSegment.moveTo(0, 0),
                    DocumentPathSegment.lineTo(1, 0),
                    DocumentPathSegment.lineTo(1, 1),
                    DocumentPathSegment.lineTo(0, 1),
                    DocumentPathSegment.close());
        }
        if (outline instanceof ShapeOutline.RoundedRectangle rounded) {
            double radius = rounded.cornerRadius();
            return roundedRectangle(outline, radius, radius, radius, radius);
        }
        if (outline instanceof ShapeOutline.RoundedRectanglePerCorner rounded) {
            DocumentCornerRadius corners = rounded.corners();
            return roundedRectangle(outline, corners.topLeft(), corners.topRight(),
                    corners.bottomRight(), corners.bottomLeft());
        }
        if (outline instanceof ShapeOutline.Ellipse) {
            return ellipse();
        }
        if (outline instanceof ShapeOutline.Polygon polygon) {
            List<DocumentPathSegment> segments = new ArrayList<>(polygon.points().size() + 1);
            for (ShapePoint point : polygon.points()) {
                segments.add(segments.isEmpty()
                        ? DocumentPathSegment.moveTo(point.x(), point.y())
                        : DocumentPathSegment.lineTo(point.x(), point.y()));
            }
            segments.add(DocumentPathSegment.close());
            return segments;
        }
        if (outline instanceof ShapeOutline.Path path) {
            return path.segments();
        }
        throw new IllegalStateException("an inline outline this export does not draw: " + outline);
    }

    /**
     * A rectangle with rounded corners, each radius clamped to half the shorter side as the
     * page clamps it.
     */
    private static List<DocumentPathSegment> roundedRectangle(ShapeOutline outline, double topLeft,
                                                              double topRight, double bottomRight,
                                                              double bottomLeft) {
        double width = outline.width();
        double height = outline.height();
        double largest = Math.min(width, height) / 2.0;
        // Each corner's radius as a fraction of the width (x) and of the height (y).
        double tlx = Math.min(topLeft, largest) / width;
        double tly = Math.min(topLeft, largest) / height;
        double trx = Math.min(topRight, largest) / width;
        double trY = Math.min(topRight, largest) / height;
        double brx = Math.min(bottomRight, largest) / width;
        double bry = Math.min(bottomRight, largest) / height;
        double blx = Math.min(bottomLeft, largest) / width;
        double bly = Math.min(bottomLeft, largest) / height;
        return List.of(
                DocumentPathSegment.moveTo(blx, 0),
                DocumentPathSegment.lineTo(1 - brx, 0),
                DocumentPathSegment.cubicTo(1 - brx * (1 - KAPPA), 0, 1, bry * (1 - KAPPA), 1, bry),
                DocumentPathSegment.lineTo(1, 1 - trY),
                DocumentPathSegment.cubicTo(1, 1 - trY * (1 - KAPPA), 1 - trx * (1 - KAPPA), 1, 1 - trx, 1),
                DocumentPathSegment.lineTo(tlx, 1),
                DocumentPathSegment.cubicTo(tlx * (1 - KAPPA), 1, 0, 1 - tly * (1 - KAPPA), 0, 1 - tly),
                DocumentPathSegment.lineTo(0, bly),
                DocumentPathSegment.cubicTo(0, bly * (1 - KAPPA), blx * (1 - KAPPA), 0, blx, 0),
                DocumentPathSegment.close());
    }

    /** The ellipse filling the unit box, as four cubic quarters. */
    private static List<DocumentPathSegment> ellipse() {
        double k = 0.5 * KAPPA;
        return List.of(
                DocumentPathSegment.moveTo(1, 0.5),
                DocumentPathSegment.cubicTo(1, 0.5 + k, 0.5 + k, 1, 0.5, 1),
                DocumentPathSegment.cubicTo(0.5 - k, 1, 0, 0.5 + k, 0, 0.5),
                DocumentPathSegment.cubicTo(0, 0.5 - k, 0.5 - k, 0, 0.5, 0),
                DocumentPathSegment.cubicTo(0.5 + k, 0, 1, 0.5 - k, 1, 0.5),
                DocumentPathSegment.close());
    }

    /** Unit-box segments moved into a sub-box of the picture, all in the picture's unit box. */
    private static List<DocumentPathSegment> place(List<DocumentPathSegment> segments, double left,
                                                   double bottom, double width, double height) {
        List<DocumentPathSegment> placed = new ArrayList<>(segments.size());
        for (DocumentPathSegment segment : segments) {
            if (segment instanceof DocumentPathSegment.MoveTo move) {
                placed.add(DocumentPathSegment.moveTo(left + move.x() * width, bottom + move.y() * height));
            } else if (segment instanceof DocumentPathSegment.LineTo line) {
                placed.add(DocumentPathSegment.lineTo(left + line.x() * width, bottom + line.y() * height));
            } else if (segment instanceof DocumentPathSegment.CubicTo curve) {
                placed.add(DocumentPathSegment.cubicTo(
                        left + curve.control1X() * width, bottom + curve.control1Y() * height,
                        left + curve.control2X() * width, bottom + curve.control2Y() * height,
                        left + curve.x() * width, bottom + curve.y() * height));
            } else {
                placed.add(segment);
            }
        }
        return placed;
    }
}
