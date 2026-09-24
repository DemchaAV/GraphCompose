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
 * <p>The page draws a stroke centred on the outline, so half of it lies outside the run's
 * box. A picture is clipped to its own edges, so it is that half-stroke larger on every
 * side; the caller lowers it by the same amount, keeping the outline where the page has
 * it.</p>
 */
final class DocxShapePictures {

    /** Where a quarter circle's control points sit, as a fraction of its radius. */
    private static final double KAPPA = 0.5522847498;

    private DocxShapePictures() {
    }

    /**
     * An inline shape drawn as a picture.
     *
     * @param png      the picture
     * @param width    its width in points, the run's and the overhang either side
     * @param height   its height in points, the same
     * @param overhang how far the widest stroke reaches past the run's box, in points
     */
    record Picture(byte[] png, double width, double height, double overhang) {
    }

    /**
     * Draws an inline shape as a picture.
     *
     * @param run the shape
     * @return the picture, the run's size plus the stroke's overhang
     */
    static Picture of(InlineShapeRun run) {
        double width = run.width();
        double height = run.height();
        double overhang = 0;
        for (ShapeLayer layer : run.layers()) {
            if (layer.stroke() != null) {
                overhang = Math.max(overhang, layer.stroke().width() / 2.0);
            }
        }
        double boxWidth = width + 2 * overhang;
        double boxHeight = height + 2 * overhang;
        List<ResolvedSvgLayer> layers = new ArrayList<>(run.layers().size());
        for (ShapeLayer layer : run.layers()) {
            ShapeOutline outline = layer.outline();
            double left = overhang + (width - outline.width()) / 2.0;
            double bottom = overhang + (height - outline.height()) / 2.0;
            List<DocumentPathSegment> segments = place(unitSegments(outline),
                    left / boxWidth, bottom / boxHeight,
                    outline.width() / boxWidth, outline.height() / boxHeight);
            Stroke stroke = layer.stroke() == null ? null
                    : new Stroke(layer.stroke().color().color(), layer.stroke().width());
            layers.add(new ResolvedSvgLayer(segments,
                    layer.fill() == null ? null : layer.fill().color(),
                    null, stroke, null, null, null, null, null));
        }
        byte[] png = InlineSvgRasters.rasterize(layers, boxWidth, boxHeight).getBytes();
        return new Picture(png, boxWidth, boxHeight, overhang);
    }

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
