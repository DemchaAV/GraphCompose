package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.AnchorMarkerPayload;
import com.demcha.compose.document.layout.payloads.BookmarkMarkerPayload;
import com.demcha.compose.document.layout.payloads.EllipseFragmentPayload;
import com.demcha.compose.document.layout.payloads.ImageFragmentPayload;
import com.demcha.compose.document.layout.payloads.LayoutAnchorPayload;
import com.demcha.compose.document.layout.payloads.LineFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.layout.payloads.ParagraphLineGeometry;
import com.demcha.compose.document.layout.payloads.PathFragmentPayload;
import com.demcha.compose.document.layout.payloads.PolygonFragmentPayload;
import com.demcha.compose.document.layout.payloads.ShapeClipBeginPayload;
import com.demcha.compose.document.layout.payloads.ShapeClipEndPayload;
import com.demcha.compose.document.layout.payloads.ShapeFragmentPayload;
import com.demcha.compose.document.layout.payloads.SideBorders;
import com.demcha.compose.document.layout.payloads.TransformBeginPayload;
import com.demcha.compose.document.layout.payloads.TransformEndPayload;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentLineCap;
import com.demcha.compose.document.style.DocumentLineJoin;
import com.demcha.compose.document.style.ShapeOutline;
import com.demcha.compose.engine.components.content.shape.Stroke;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Whether a clip the page sets cuts any of what is painted inside it.
 *
 * <p>A Word file has no clip a container can set round its layers, so what a layer stack or a
 * shape container clips on the page is written whole. That loses something only where the clip
 * cuts something: an icon's art parked outside its box, the corners of a square tile in a disc,
 * a label longer than its chip. Most clips cut nothing — an icon drawn inside its box, a disc's
 * initials, a chip's label whose line stands past the chip while its letters stay inside.</p>
 *
 * <p>What is painted is measured from the layout's fragments as the Word file draws them,
 * upright, a transform not being carried: a fill to its outline, a fill of a gradient alone not
 * at all, as the file draws none; a stroke as the PDF paints it ({@link DocxInkOutline}), a box's
 * side borders each a line of its own, ended flat at its corners; a picture to its box, or to the
 * ellipse the file crops it to; and a line of text across the width it was set at and from its
 * letters' tops to their feet, read from the outlines of its glyphs (see {@link DocxInk}), or
 * over its whole line where those are not known. Ink within half a point of the clip is not
 * counted as cut.</p>
 *
 * <p>{@code PptxClipSafety} asks the stricter question of the same fragments — whether a clip
 * provably cuts nothing, so that a slide may keep its shapes — and so takes any stroked path, and
 * any text near an edge, as cut. A note in the report must name only what is lost, and this
 * measures what is.</p>
 */
final class DocxClipInk {

    /** How far ink may stand past a clip before the clip is taken to cut it. */
    static final double TOLERANCE = 0.5;

    private DocxClipInk() {
    }

    /**
     * How far a line's letters reach above and below the baseline the page sets it on.
     */
    @FunctionalInterface
    interface LetterReach {
        /**
         * The reach of a line's letters.
         *
         * @param paragraph the paragraph the line is set in
         * @param line      the line
         * @return {@code {above, below}} its baseline in points, or {@code null} when not known
         */
        double[] of(ParagraphFragmentPayload paragraph, ParagraphLine line);
    }

    /**
     * Whether a clip cuts any of what is painted inside it.
     *
     * @param clip               the fragment opening the clip, whose box the clip's outline is
     *                           set in
     * @param painted            what the page paints between the clip's opening and its close
     * @param croppedToAnEllipse the pictures the Word file crops to the ellipse they fill
     * @param letters            how far a line's letters reach
     * @return whether anything painted stands past the clip
     */
    static boolean cuts(PlacedFragment clip, List<PlacedFragment> painted,
                        Predicate<PlacedFragment> croppedToAnEllipse, LetterReach letters) {
        Region region = regionOf((ShapeClipBeginPayload) clip.payload(), clip);
        for (PlacedFragment fragment : painted) {
            for (double[] point : inkOf(fragment, croppedToAnEllipse.test(fragment), letters)) {
                if (!region.holds(point[0], point[1])) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The area a clip keeps, measured on the page with y up. */
    @FunctionalInterface
    private interface Region {
        boolean holds(double x, double y);
    }

    private static Region regionOf(ShapeClipBeginPayload clip, PlacedFragment box) {
        double left = box.x();
        double bottom = box.y();
        double right = left + box.width();
        double top = bottom + box.height();
        Region bounds = (x, y) -> x >= left - TOLERANCE && x <= right + TOLERANCE
                                  && y >= bottom - TOLERANCE && y <= top + TOLERANCE;
        ShapeOutline outline = clip.outline();
        if (clip.policy() != ClipPolicy.CLIP_PATH || outline instanceof ShapeOutline.Rectangle) {
            return bounds;
        }
        if (outline instanceof ShapeOutline.Ellipse) {
            double a = box.width() / 2 + TOLERANCE;
            double b = box.height() / 2 + TOLERANCE;
            double cx = left + box.width() / 2;
            double cy = bottom + box.height() / 2;
            return (x, y) -> square((x - cx) / a) + square((y - cy) / b) <= 1;
        }
        if (outline instanceof ShapeOutline.RoundedRectangle rounded) {
            double r = rounded.cornerRadius();
            return roundedRegion(bounds, left, bottom, right, top, r, r, r, r);
        }
        if (outline instanceof ShapeOutline.RoundedRectanglePerCorner rounded) {
            DocumentCornerRadius corners = rounded.corners();
            return roundedRegion(bounds, left, bottom, right, top,
                    corners.topLeft(), corners.topRight(), corners.bottomRight(), corners.bottomLeft());
        }
        List<List<double[]>> rings = new ArrayList<>();
        if (outline instanceof ShapeOutline.Polygon polygon) {
            rings.add(DocxInkOutline.ring(polygon.points(), box));
        } else {
            for (DocxInkOutline.Run run : DocxInkOutline.flatten(((ShapeOutline.Path) outline).segments(), box)) {
                rings.add(run.points());
            }
        }
        // Past the outline's own box a point is outside it, without walking its edges.
        double[] extent = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (List<double[]> ring : rings) {
            for (double[] point : ring) {
                extent[0] = Math.min(extent[0], point[0]);
                extent[1] = Math.min(extent[1], point[1]);
                extent[2] = Math.max(extent[2], point[0]);
                extent[3] = Math.max(extent[3], point[1]);
            }
        }
        return (x, y) -> x >= extent[0] - TOLERANCE && x <= extent[2] + TOLERANCE
                         && y >= extent[1] - TOLERANCE && y <= extent[3] + TOLERANCE
                         && (winding(rings, x, y) != 0 || nearAnEdge(rings, x, y));
    }

    /** A box whose corners are rounded, each radius clamped to half the smaller side. */
    private static Region roundedRegion(Region bounds, double left, double bottom, double right, double top,
                                        double topLeft, double topRight, double bottomRight, double bottomLeft) {
        double half = Math.min(right - left, top - bottom) / 2;
        double[][] corners = {
                {left, top, -1, 1, Math.min(topLeft, half)},
                {right, top, 1, 1, Math.min(topRight, half)},
                {right, bottom, 1, -1, Math.min(bottomRight, half)},
                {left, bottom, -1, -1, Math.min(bottomLeft, half)}};
        return (x, y) -> {
            if (!bounds.holds(x, y)) {
                return false;
            }
            for (double[] corner : corners) {
                double r = corner[4];
                double cx = corner[0] - corner[2] * r;
                double cy = corner[1] - corner[3] * r;
                // Past the corner's centre on both axes, the point must be inside its arc.
                if (r > 0 && (x - cx) * corner[2] > 0 && (y - cy) * corner[3] > 0
                    && Math.hypot(x - cx, y - cy) > r + TOLERANCE) {
                    return false;
                }
            }
            return true;
        };
    }

    /** Points on the outer edge of what a fragment paints, or none for a marker. */
    private static List<double[]> inkOf(PlacedFragment fragment, boolean croppedToAnEllipse, LetterReach letters) {
        Object payload = fragment.payload();
        List<double[]> ink = new ArrayList<>();
        if (payload instanceof ShapeClipBeginPayload || payload instanceof ShapeClipEndPayload
            || payload instanceof TransformBeginPayload || payload instanceof TransformEndPayload
            || payload instanceof AnchorMarkerPayload || payload instanceof BookmarkMarkerPayload
            || payload instanceof LayoutAnchorPayload) {
            return ink;
        }
        if (payload instanceof ShapeFragmentPayload shape) {
            DocumentCornerRadius radius = shape.cornerRadius() == null ? DocumentCornerRadius.ZERO : shape.cornerRadius();
            if (shape.fillColor() != null) {
                DocxInkOutline.roundedBox(ink, fragment, 0, radius);
            }
            SideBorders sides = shape.sideBorders();
            if (sides != null && sides.hasAny()) {
                // Each side is a line of its own, ended flat at the box's corners, in place of
                // the stroke round it.
                double left = fragment.x();
                double bottom = fragment.y();
                double right = left + fragment.width();
                double top = bottom + fragment.height();
                side(ink, sides.top(), left, top, right, top);
                side(ink, sides.right(), right, top, right, bottom);
                side(ink, sides.bottom(), left, bottom, right, bottom);
                side(ink, sides.left(), left, top, left, bottom);
            } else if (halfOf(shape.stroke()) > 0) {
                DocxInkOutline.roundedBox(ink, fragment, halfOf(shape.stroke()), radius);
            }
        } else if (payload instanceof EllipseFragmentPayload ellipse) {
            double reach = halfOf(ellipse.stroke());
            if (ellipse.fillColor() != null || reach > 0) {
                DocxInkOutline.ellipse(ink, fragment, reach);
            }
        } else if (payload instanceof LineFragmentPayload line) {
            double half = halfOf(line.stroke());
            if (half > 0) {
                DocxInkOutline.stroke(ink, List.of(
                                new double[]{fragment.x() + line.startX(), fragment.y() + line.startY()},
                                new double[]{fragment.x() + line.endX(), fragment.y() + line.endY()}),
                        false, half, line.lineCap(), DocumentLineJoin.MITER);
            }
        } else if (payload instanceof PathFragmentPayload path) {
            double half = halfOf(path.stroke());
            for (DocxInkOutline.Run run : DocxInkOutline.flatten(path.segments(), fragment)) {
                if (path.fillColor() != null) {
                    DocxInkOutline.filled(ink, run.points());
                }
                if (half > 0) {
                    DocxInkOutline.stroke(ink, run.points(), run.closed(), half, path.lineCap(), path.lineJoin());
                }
            }
        } else if (payload instanceof PolygonFragmentPayload polygon) {
            double half = halfOf(polygon.stroke());
            if (!polygon.points().isEmpty()) {
                List<double[]> ring = DocxInkOutline.ring(polygon.points(), fragment);
                if (polygon.fillColor() != null) {
                    DocxInkOutline.filled(ink, ring);
                }
                if (half > 0) {
                    DocxInkOutline.stroke(ink, ring, true, half, DocumentLineCap.BUTT, DocumentLineJoin.MITER);
                }
            }
        } else if (payload instanceof ParagraphFragmentPayload paragraph) {
            text(ink, fragment, paragraph, letters);
        } else if (payload instanceof ImageFragmentPayload && croppedToAnEllipse) {
            DocxInkOutline.ellipse(ink, fragment, 0);
        } else {
            // A picture, a barcode, a table's row, and anything else: its box.
            DocxInkOutline.roundedBox(ink, fragment, 0, DocumentCornerRadius.ZERO);
        }
        return ink;
    }

    /**
     * Each line of a paragraph, across the width it was set at and from its letters' tops to
     * their feet on the baseline the page sets it on — over its whole line where its letters'
     * reach is not known, or is said to run past the line the layout measured for them: an
     * outline read in other units than the layout's, as a face the PDF stands another in for
     * gives, is not to be trusted.
     */
    private static void text(List<double[]> ink, PlacedFragment fragment, ParagraphFragmentPayload paragraph,
                             LetterReach letters) {
        boolean padded = paragraph.padding() != null;
        double innerX = fragment.x() + (padded ? paragraph.padding().left() : 0);
        double innerWidth = Math.max(0,
                fragment.width() - (padded ? paragraph.padding().left() + paragraph.padding().right() : 0));
        double lineTop = ParagraphLineGeometry.contentTop(fragment.y(), fragment.height(),
                padded ? paragraph.padding().top() : 0);
        for (ParagraphLine line : paragraph.lines()) {
            if (line.width() > 0) {
                double start = ParagraphLineGeometry.lineStartX(paragraph.align(), innerX, innerWidth, line.width());
                double lineBottom = lineTop - line.lineHeight();
                double baseline = ParagraphLineGeometry.baselineY(lineTop, line.lineHeight(),
                        line.baselineOffsetFromBottom());
                double[] reach = letters.of(paragraph, line);
                if (reach == null || baseline + reach[0] > lineTop + TOLERANCE
                    || baseline - reach[1] < lineBottom - TOLERANCE) {
                    DocxInkOutline.box(ink, start, lineBottom, start + line.width(), lineTop);
                } else if (reach[0] + reach[1] > 0) {
                    DocxInkOutline.box(ink, start, baseline - reach[1], start + line.width(), baseline + reach[0]);
                }
            }
            lineTop = ParagraphLineGeometry.nextLineTop(lineTop, line.lineHeight(), paragraph.lineGap());
        }
    }

    /** One side of a box, drawn as a line of its own. */
    private static void side(List<double[]> ink, Stroke stroke, double x1, double y1, double x2, double y2) {
        double half = halfOf(stroke);
        if (half > 0) {
            DocxInkOutline.stroke(ink, List.of(new double[]{x1, y1}, new double[]{x2, y2}), false, half,
                    DocumentLineCap.BUTT, DocumentLineJoin.MITER);
        }
    }

    /** The non-zero winding number of the rings round a point. */
    private static int winding(List<List<double[]>> rings, double x, double y) {
        int winding = 0;
        for (List<double[]> ring : rings) {
            for (int index = 0; index < ring.size(); index++) {
                double[] from = ring.get(index);
                double[] to = ring.get((index + 1) % ring.size());
                if (from[1] <= y) {
                    if (to[1] > y && cross(from, to, x, y) > 0) {
                        winding++;
                    }
                } else if (to[1] <= y && cross(from, to, x, y) < 0) {
                    winding--;
                }
            }
        }
        return winding;
    }

    /** Which side of the line from one point to another a point stands, by sign. */
    private static double cross(double[] from, double[] to, double x, double y) {
        return (to[0] - from[0]) * (y - from[1]) - (x - from[0]) * (to[1] - from[1]);
    }

    /** Whether a point is within the tolerance of an edge of the rings. */
    private static boolean nearAnEdge(List<List<double[]>> rings, double x, double y) {
        for (List<double[]> ring : rings) {
            for (int index = 0; index < ring.size(); index++) {
                double[] from = ring.get(index);
                double[] to = ring.get((index + 1) % ring.size());
                double dx = to[0] - from[0];
                double dy = to[1] - from[1];
                double length = dx * dx + dy * dy;
                double t = length == 0 ? 0 : Math.max(0, Math.min(1, ((x - from[0]) * dx + (y - from[1]) * dy) / length));
                if (Math.hypot(x - from[0] - t * dx, y - from[1] - t * dy) <= TOLERANCE) {
                    return true;
                }
            }
        }
        return false;
    }

    private static double halfOf(Stroke stroke) {
        return stroke == null ? 0 : Math.max(0, stroke.width()) / 2;
    }

    private static double square(double value) {
        return value * value;
    }
}
