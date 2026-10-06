package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.image.DocumentImageFitMode;
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
import com.demcha.compose.engine.components.content.ImageData;
import com.demcha.compose.engine.components.content.shape.Stroke;

import java.awt.Shape;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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
 * <p>What is painted is measured from the layout's fragments as the page paints it
 * ({@link DocxInkOutline}), upright, as the file writes it, a transform not being carried: a
 * fill to its outline, and a fill of a gradient alone not at all, the file drawing none; a
 * stroke with its cap and its join; a box's side borders each a line of its own, ended flat at
 * its corners; a picture to the box it is drawn in — fitted inside its own where it is
 * contained — or to the ellipse the file crops it to; and a line of text across the width it was
 * set at and from its letters' tops to their feet, read from the outlines of its glyphs (see
 * {@link DocxInk}), or over its whole line where those are not known. What a clip inside it cuts
 * away is that clip's loss, not this one's. Ink within half a point of the clip is not counted as
 * cut. A highlight's chip behind its run, a table row's border and a fill over a hole in a clip
 * are not measured.</p>
 *
 * <p>{@code PptxClipSafety} asks the stricter question of the same fragments — whether a clip
 * provably cuts nothing, so that a slide may keep its shapes — and so takes any stroked path, and
 * any text near an edge, as cut. A note in the report must name only what is lost, and this
 * measures what is.</p>
 */
final class DocxClipInk {

    /** How far ink may stand past a clip before the clip is taken to cut it. */
    private static final double TOLERANCE = 0.5;

    /** The most bands a clip's outline is indexed in, by height. */
    private static final int MAX_BANDS = 256;

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
        // The clips opened inside this one, innermost first: what they cut away the page never
        // paints, so only what stands inside all of them counts here.
        Deque<Inner> inner = new ArrayDeque<>();
        for (PlacedFragment fragment : painted) {
            if (fragment.payload() instanceof ShapeClipBeginPayload begin) {
                inner.push(new Inner(begin.ownerPath(), regionOf(begin, fragment)));
                continue;
            }
            if (fragment.payload() instanceof ShapeClipEndPayload end) {
                if (!inner.isEmpty() && inner.peek().ownerPath().equals(end.ownerPath())) {
                    inner.pop();
                }
                continue;
            }
            for (double[] point : inkOf(fragment, croppedToAnEllipse.test(fragment), letters)) {
                if (keptByEvery(inner, point) && !region.holds(point[0], point[1])) {
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

    /** A clip opened inside the one measured. */
    private record Inner(String ownerPath, Region region) {
    }

    private static boolean keptByEvery(Deque<Inner> inner, double[] point) {
        for (Inner clip : inner) {
            if (!clip.region().holds(point[0], point[1])) {
                return false;
            }
        }
        return true;
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
        Shape shape = outline instanceof ShapeOutline.Polygon polygon
                ? DocxInkOutline.polygon(polygon.points(), box)
                : DocxInkOutline.path(((ShapeOutline.Path) outline).segments(), box);
        return new Edges(DocxInkOutline.rings(shape))::holds;
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
        if (payload instanceof TransformBeginPayload || payload instanceof TransformEndPayload
            || payload instanceof AnchorMarkerPayload || payload instanceof BookmarkMarkerPayload
            || payload instanceof LayoutAnchorPayload) {
            return ink;
        }
        if (payload instanceof ShapeFragmentPayload shape) {
            box(ink, fragment, shape);
        } else if (payload instanceof EllipseFragmentPayload ellipse) {
            Shape outline = DocxInkOutline.ellipse(fragment.x(), fragment.y(), fragment.width(), fragment.height());
            if (ellipse.fillColor() != null) {
                DocxInkOutline.filled(ink, outline);
            }
            DocxInkOutline.stroked(ink, outline, widthOf(ellipse.stroke()), DocumentLineCap.BUTT, DocumentLineJoin.MITER);
        } else if (payload instanceof LineFragmentPayload line) {
            DocxInkOutline.stroked(ink, DocxInkOutline.line(fragment.x() + line.startX(), fragment.y() + line.startY(),
                            fragment.x() + line.endX(), fragment.y() + line.endY()),
                    widthOf(line.stroke()), line.lineCap(), DocumentLineJoin.MITER);
        } else if (payload instanceof PathFragmentPayload path) {
            Shape outline = DocxInkOutline.path(path.segments(), fragment);
            if (path.fillColor() != null) {
                DocxInkOutline.filled(ink, outline);
            }
            DocxInkOutline.stroked(ink, outline, widthOf(path.stroke()), path.lineCap(), path.lineJoin());
        } else if (payload instanceof PolygonFragmentPayload polygon) {
            Shape outline = DocxInkOutline.polygon(polygon.points(), fragment);
            if (polygon.fillColor() != null) {
                DocxInkOutline.filled(ink, outline);
            }
            DocxInkOutline.stroked(ink, outline, widthOf(polygon.stroke()), DocumentLineCap.BUTT, DocumentLineJoin.MITER);
        } else if (payload instanceof ParagraphFragmentPayload paragraph) {
            text(ink, fragment, paragraph, letters);
        } else if (payload instanceof ImageFragmentPayload image) {
            double[] drawn = drawn(fragment, image);
            DocxInkOutline.filled(ink, croppedToAnEllipse
                    ? DocxInkOutline.ellipse(drawn[0], drawn[1], drawn[2], drawn[3])
                    : DocxInkOutline.box(drawn[0], drawn[1], drawn[0] + drawn[2], drawn[1] + drawn[3]));
        } else {
            // A barcode, a table's row, and anything else: its box.
            DocxInkOutline.filled(ink, DocxInkOutline.box(fragment, DocumentCornerRadius.ZERO));
        }
        return ink;
    }

    /**
     * A box's paint, as the page paints it: none for a box of no size; its fill, a fill of a
     * gradient alone being none the file draws; and either each of its side borders as a line of
     * its own, ended flat at its corners, or its stroke round it.
     */
    private static void box(List<double[]> ink, PlacedFragment fragment, ShapeFragmentPayload shape) {
        if (!(fragment.width() > 0) || !(fragment.height() > 0)) {
            return;
        }
        DocumentCornerRadius radius = shape.cornerRadius() == null ? DocumentCornerRadius.ZERO : shape.cornerRadius();
        Shape outline = DocxInkOutline.box(fragment, radius);
        if (shape.fillColor() != null) {
            DocxInkOutline.filled(ink, outline);
        }
        SideBorders sides = shape.sideBorders();
        if (sides != null && sides.hasAny()) {
            double left = fragment.x();
            double bottom = fragment.y();
            double right = left + fragment.width();
            double top = bottom + fragment.height();
            side(ink, sides.top(), left, top, right, top);
            side(ink, sides.right(), right, top, right, bottom);
            side(ink, sides.bottom(), left, bottom, right, bottom);
            side(ink, sides.left(), left, top, left, bottom);
        } else {
            DocxInkOutline.stroked(ink, outline, widthOf(shape.stroke()), DocumentLineCap.BUTT, DocumentLineJoin.MITER);
        }
    }

    private static void side(List<double[]> ink, Stroke stroke, double x1, double y1, double x2, double y2) {
        DocxInkOutline.stroked(ink, DocxInkOutline.line(x1, y1, x2, y2), widthOf(stroke),
                DocumentLineCap.BUTT, DocumentLineJoin.MITER);
    }

    /**
     * Where the page draws a picture, {@code {x, y, width, height}}: fitted inside its box and
     * centred there where it is contained, as the file writes it at that size too; across its box
     * otherwise, covering it or stretched over it.
     */
    private static double[] drawn(PlacedFragment fragment, ImageFragmentPayload image) {
        double[] box = {fragment.x(), fragment.y(), fragment.width(), fragment.height()};
        ImageData data = image.imageData();
        if (image.fitMode() != DocumentImageFitMode.CONTAIN || data == null || data.getMetadata() == null) {
            return box;
        }
        double sourceWidth = Math.max(1, data.getMetadata().width());
        double sourceHeight = Math.max(1, data.getMetadata().height());
        double scale = Math.min(fragment.width() / sourceWidth, fragment.height() / sourceHeight);
        double width = sourceWidth * scale;
        double height = sourceHeight * scale;
        return new double[]{fragment.x() + (fragment.width() - width) / 2,
                fragment.y() + (fragment.height() - height) / 2, width, height};
    }

    /**
     * Each line of a paragraph, across the width it was set at and from its letters' tops to
     * their feet on the baseline the page sets it on, wherever that is — past the line, for a
     * title set tighter than its face or a line seated at its foot — or over its whole line where
     * its letters' reach is not known.
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
                double[] reach = letters.of(paragraph, line);
                if (reach == null) {
                    DocxInkOutline.filled(ink, DocxInkOutline.box(start, lineTop - line.lineHeight(),
                            start + line.width(), lineTop));
                } else if (reach[0] + reach[1] > 0) {
                    double baseline = ParagraphLineGeometry.baselineY(lineTop, line.lineHeight(),
                            line.baselineOffsetFromBottom());
                    DocxInkOutline.filled(ink, DocxInkOutline.box(start, baseline - reach[1],
                            start + line.width(), baseline + reach[0]));
                }
            }
            lineTop = ParagraphLineGeometry.nextLineTop(lineTop, line.lineHeight(), paragraph.lineGap());
        }
    }

    /**
     * The edges of a clip's outline, indexed by height so that a point is tested against the
     * edges at its own height rather than every edge of a long path.
     */
    private static final class Edges {
        private final double left;
        private final double bottom;
        private final double right;
        private final double top;
        private final double bandHeight;
        private final List<List<double[]>> bands = new ArrayList<>();

        Edges(List<List<double[]>> rings) {
            List<double[]> edges = new ArrayList<>();
            double minX = Double.POSITIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;
            for (List<double[]> ring : rings) {
                for (int index = 0; index < ring.size(); index++) {
                    double[] from = ring.get(index);
                    double[] to = ring.get((index + 1) % ring.size());
                    edges.add(new double[]{from[0], from[1], to[0], to[1]});
                    minX = Math.min(minX, from[0]);
                    minY = Math.min(minY, from[1]);
                    maxX = Math.max(maxX, from[0]);
                    maxY = Math.max(maxY, from[1]);
                }
            }
            left = minX;
            bottom = minY;
            right = maxX;
            top = maxY;
            int count = Math.max(1, Math.min(MAX_BANDS, edges.size() / 8));
            bandHeight = maxY > minY ? (maxY - minY) / count : 1;
            for (int band = 0; band < count; band++) {
                bands.add(new ArrayList<>());
            }
            for (double[] edge : edges) {
                int from = band(Math.min(edge[1], edge[3]) - TOLERANCE);
                int to = band(Math.max(edge[1], edge[3]) + TOLERANCE);
                for (int band = from; band <= to; band++) {
                    bands.get(band).add(edge);
                }
            }
        }

        private int band(double y) {
            return Math.max(0, Math.min(bands.size() - 1, (int) Math.floor((y - bottom) / bandHeight)));
        }

        /** Inside the outline by the non-zero rule, or within the tolerance of its edge. */
        boolean holds(double x, double y) {
            if (!(x >= left - TOLERANCE && x <= right + TOLERANCE && y >= bottom - TOLERANCE && y <= top + TOLERANCE)) {
                return false;
            }
            int winding = 0;
            for (double[] edge : bands.get(band(y))) {
                if (nearTheEdge(edge, x, y)) {
                    return true;
                }
                if (edge[1] <= y) {
                    if (edge[3] > y && cross(edge, x, y) > 0) {
                        winding++;
                    }
                } else if (edge[3] <= y && cross(edge, x, y) < 0) {
                    winding--;
                }
            }
            return winding != 0;
        }

        /** Which side of an edge a point stands, by sign. */
        private static double cross(double[] edge, double x, double y) {
            return (edge[2] - edge[0]) * (y - edge[1]) - (x - edge[0]) * (edge[3] - edge[1]);
        }

        private static boolean nearTheEdge(double[] edge, double x, double y) {
            double dx = edge[2] - edge[0];
            double dy = edge[3] - edge[1];
            double length = dx * dx + dy * dy;
            double t = length == 0 ? 0 : Math.max(0, Math.min(1, ((x - edge[0]) * dx + (y - edge[1]) * dy) / length));
            return Math.hypot(x - edge[0] - t * dx, y - edge[1] - t * dy) <= TOLERANCE;
        }
    }

    private static double widthOf(Stroke stroke) {
        return stroke == null ? 0 : Math.max(0, stroke.width());
    }

    private static double square(double value) {
        return value * value;
    }
}
