package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentLineCap;
import com.demcha.compose.document.style.DocumentLineJoin;
import com.demcha.compose.document.style.DocumentPathSegment;
import com.demcha.compose.document.style.ShapePoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Where a drawing's paint runs: points along the outer edge of a fill or a stroke, as the PDF
 * paints it, measured on the page with y up (see {@link DocxClipInk}).
 *
 * <p>A fill runs to its outline. A stroke runs half its width either side of each of its lines;
 * round a round join, out to the point of a mitred one where the PDF's miter limit keeps it, and
 * no further than its lines' edges at a bevel; at an open run's ends, nothing past a butt cap,
 * half its width past a square one and round a round one. Points along an edge stand no further
 * apart than {@link #STEP}, and a curve is taken at {@link #CURVE_STEPS} points along it.</p>
 */
final class DocxInkOutline {

    /** The PDF's default miter limit, which the page paints every join with. */
    private static final double MITER_LIMIT = 10;

    /** The longest step between two points measured along an edge. */
    private static final double STEP = 2;

    /** The points a cubic curve is measured at. */
    private static final int CURVE_STEPS = 16;

    /** The points an ellipse or a round end is measured at. */
    private static final int ROUND_STEPS = 32;

    /** The cosine of the least turn a round join is measured round: ten degrees. */
    private static final double MIN_TURN_COSINE = Math.cos(Math.toRadians(10));

    private DocxInkOutline() {
    }

    /**
     * A sub-path, as the points it runs through.
     *
     * @param points the points, a curve's taken along its length
     * @param closed whether a close ends it
     */
    record Run(List<double[]> points, boolean closed) {
    }

    /** The edge of a filled run: the points along it, its closing edge with them. */
    static void filled(List<double[]> ink, List<double[]> points) {
        for (int index = 0; index < points.size(); index++) {
            edge(ink, points.get(index), points.get((index + 1) % points.size()));
        }
    }

    /**
     * The edge of a stroke along a run of points. A run of one point is a dot, which a round or
     * square cap draws.
     *
     * @param half the stroke's half width
     */
    static void stroke(List<double[]> ink, List<double[]> points, boolean closed, double half,
                       DocumentLineCap cap, DocumentLineJoin join) {
        List<double[]> run = new ArrayList<>();
        for (double[] point : points) {
            if (run.isEmpty() || !same(run.get(run.size() - 1), point)) {
                run.add(point);
            }
        }
        if (closed && run.size() > 1 && same(run.get(0), run.get(run.size() - 1))) {
            run.remove(run.size() - 1);
        }
        if (run.isEmpty()) {
            return;
        }
        if (run.size() == 1) {
            if (!closed) {
                end(ink, run.get(0), new double[]{1, 0}, half, cap);
                end(ink, run.get(0), new double[]{-1, 0}, half, cap);
            }
            return;
        }
        int count = run.size();
        int lines = closed ? count : count - 1;
        for (int index = 0; index < lines; index++) {
            double[] from = run.get(index);
            double[] to = run.get((index + 1) % count);
            double[] normal = normal(from, to);
            List<double[]> along = new ArrayList<>();
            edge(along, from, to);
            along.add(to);
            for (double[] point : along) {
                ink.add(new double[]{point[0] + normal[0] * half, point[1] + normal[1] * half});
                ink.add(new double[]{point[0] - normal[0] * half, point[1] - normal[1] * half});
            }
        }
        for (int index = closed ? 0 : 1; index < (closed ? count : count - 1); index++) {
            double[] before = run.get((index - 1 + count) % count);
            double[] corner = run.get(index);
            double[] after = run.get((index + 1) % count);
            if (join == DocumentLineJoin.ROUND) {
                // Where a curve's points turn by a few degrees, the two lines' edges already
                // meet its arc within a few hundredths of the stroke's half width.
                if (turns(before, corner, after)) {
                    round(ink, corner, half);
                }
            } else if (join == DocumentLineJoin.MITER) {
                miterTip(ink, before, corner, after, half);
            }
        }
        if (!closed) {
            end(ink, run.get(0), direction(run.get(1), run.get(0)), half, cap);
            end(ink, run.get(count - 1), direction(run.get(count - 2), run.get(count - 1)), half, cap);
        }
    }

    /**
     * A box grown by {@code reach}: a rounded corner round its arc, its radius grown the same, and
     * a square one to its point, as a mitred stroke draws it.
     */
    static void roundedBox(List<double[]> ink, PlacedFragment fragment, double reach, DocumentCornerRadius radius) {
        double left = fragment.x();
        double bottom = fragment.y();
        double right = left + fragment.width();
        double top = bottom + fragment.height();
        double half = Math.min(fragment.width(), fragment.height()) / 2;
        double[][] corners = {
                {right, top, 1, 1, Math.min(radius.topRight(), half)},
                {left, top, -1, 1, Math.min(radius.topLeft(), half)},
                {left, bottom, -1, -1, Math.min(radius.bottomLeft(), half)},
                {right, bottom, 1, -1, Math.min(radius.bottomRight(), half)}};
        List<double[]> ring = new ArrayList<>();
        for (double[] corner : corners) {
            double r = Math.max(0, corner[4]);
            if (r == 0) {
                ring.add(new double[]{corner[0] + corner[2] * reach, corner[1] + corner[3] * reach});
                continue;
            }
            double cx = corner[0] - corner[2] * r;
            double cy = corner[1] - corner[3] * r;
            for (int step = 0; step <= ROUND_STEPS / 4; step++) {
                double angle = Math.PI / 2 * step / (ROUND_STEPS / 4);
                ring.add(new double[]{cx + corner[2] * (r + reach) * Math.cos(angle),
                        cy + corner[3] * (r + reach) * Math.sin(angle)});
            }
        }
        filled(ink, ring);
    }

    static void box(List<double[]> ink, double left, double bottom, double right, double top) {
        filled(ink, List.of(new double[]{left, bottom}, new double[]{right, bottom},
                new double[]{right, top}, new double[]{left, top}));
    }

    /** The ellipse a box holds, its axes grown by {@code reach}. */
    static void ellipse(List<double[]> ink, PlacedFragment fragment, double reach) {
        double a = fragment.width() / 2 + reach;
        double b = fragment.height() / 2 + reach;
        double cx = fragment.x() + fragment.width() / 2;
        double cy = fragment.y() + fragment.height() / 2;
        int steps = ROUND_STEPS * 2;
        for (int step = 0; step < steps; step++) {
            double angle = 2 * Math.PI * step / steps;
            ink.add(new double[]{cx + a * Math.cos(angle), cy + b * Math.sin(angle)});
        }
    }

    /** A polygon's normalized points, set in a box. */
    static List<double[]> ring(List<ShapePoint> points, PlacedFragment box) {
        List<double[]> ring = new ArrayList<>(points.size());
        for (ShapePoint point : points) {
            ring.add(new double[]{box.x() + point.x() * box.width(), box.y() + point.y() * box.height()});
        }
        return ring;
    }

    /** A path's normalized segments, set in a box, each sub-path as the points it runs through. */
    static List<Run> flatten(List<DocumentPathSegment> segments, PlacedFragment box) {
        List<Run> runs = new ArrayList<>();
        List<double[]> current = null;
        double[] start = null;
        for (DocumentPathSegment segment : segments) {
            if (segment instanceof DocumentPathSegment.MoveTo move) {
                // A move alone draws nothing; a sub-path needs a segment.
                if (current != null && current.size() > 1) {
                    runs.add(new Run(current, false));
                }
                start = at(box, move.x(), move.y());
                current = new ArrayList<>();
                current.add(start);
            } else if (current == null) {
                // A path opens with a move; anything before one draws nothing.
                continue;
            } else if (segment instanceof DocumentPathSegment.LineTo line) {
                current.add(at(box, line.x(), line.y()));
            } else if (segment instanceof DocumentPathSegment.CubicTo cubic) {
                double[] from = current.get(current.size() - 1);
                double[] first = at(box, cubic.control1X(), cubic.control1Y());
                double[] second = at(box, cubic.control2X(), cubic.control2Y());
                double[] to = at(box, cubic.x(), cubic.y());
                for (int step = 1; step <= CURVE_STEPS; step++) {
                    double t = (double) step / CURVE_STEPS;
                    double u = 1 - t;
                    current.add(new double[]{
                            u * u * u * from[0] + 3 * u * u * t * first[0] + 3 * u * t * t * second[0] + t * t * t * to[0],
                            u * u * u * from[1] + 3 * u * u * t * first[1] + 3 * u * t * t * second[1] + t * t * t * to[1]});
                }
            } else if (segment instanceof DocumentPathSegment.Close) {
                current.add(start);
                runs.add(new Run(current, true));
                // What follows a close without a move starts again where the sub-path did.
                current = new ArrayList<>();
                current.add(start);
            }
        }
        if (current != null && current.size() > 1) {
            runs.add(new Run(current, false));
        }
        return runs;
    }

    /** Whether a run turns at a point by more than ten degrees. */
    private static boolean turns(double[] before, double[] corner, double[] after) {
        double[] in = direction(before, corner);
        double[] out = direction(corner, after);
        return in[0] * out[0] + in[1] * out[1] < MIN_TURN_COSINE;
    }

    /** The end of an open stroke, running out along {@code outward} from its last point. */
    private static void end(List<double[]> ink, double[] point, double[] outward, double half, DocumentLineCap cap) {
        if (cap == DocumentLineCap.ROUND) {
            round(ink, point, half);
        } else if (cap == DocumentLineCap.SQUARE) {
            double x = point[0] + outward[0] * half;
            double y = point[1] + outward[1] * half;
            ink.add(new double[]{x - outward[1] * half, y + outward[0] * half});
            ink.add(new double[]{x + outward[1] * half, y - outward[0] * half});
        }
    }

    /** The point a mitred join runs to, where the miter limit keeps it rather than bevelling it. */
    private static void miterTip(List<double[]> ink, double[] before, double[] corner, double[] after, double half) {
        double[] in = direction(before, corner);
        double[] out = direction(corner, after);
        // The tip lies along the difference of the two directions, past the outer side of the turn.
        double tipX = in[0] - out[0];
        double tipY = in[1] - out[1];
        double tipLength = Math.hypot(tipX, tipY);
        if (tipLength < 1e-9) {
            return;
        }
        // The miter runs 1 / sin(θ/2) half-widths past the corner, θ the angle between the lines;
        // sin(θ/2) is half the length of the sum of the two directions.
        double ratio = 2 / Math.hypot(in[0] + out[0], in[1] + out[1]);
        if (!(ratio <= MITER_LIMIT)) {
            return;
        }
        ink.add(new double[]{corner[0] + tipX / tipLength * half * ratio, corner[1] + tipY / tipLength * half * ratio});
    }

    /** Points round {@code centre} at {@code radius}. */
    private static void round(List<double[]> ink, double[] centre, double radius) {
        for (int step = 0; step < ROUND_STEPS; step++) {
            double angle = 2 * Math.PI * step / ROUND_STEPS;
            ink.add(new double[]{centre[0] + radius * Math.cos(angle), centre[1] + radius * Math.sin(angle)});
        }
    }

    /** The points from {@code from} towards {@code to}, no further apart than {@link #STEP}. */
    private static void edge(List<double[]> into, double[] from, double[] to) {
        int steps = Math.max(1, (int) Math.ceil(Math.hypot(to[0] - from[0], to[1] - from[1]) / STEP));
        for (int step = 0; step < steps; step++) {
            double t = (double) step / steps;
            into.add(new double[]{from[0] + (to[0] - from[0]) * t, from[1] + (to[1] - from[1]) * t});
        }
    }

    /** The unit direction from one point to another, or none where they meet. */
    private static double[] direction(double[] from, double[] to) {
        double dx = to[0] - from[0];
        double dy = to[1] - from[1];
        double length = Math.hypot(dx, dy);
        return length == 0 ? new double[]{0, 0} : new double[]{dx / length, dy / length};
    }

    private static double[] normal(double[] from, double[] to) {
        double[] along = direction(from, to);
        return new double[]{-along[1], along[0]};
    }

    private static double[] at(PlacedFragment box, double x, double y) {
        return new double[]{box.x() + x * box.width(), box.y() + y * box.height()};
    }

    private static boolean same(double[] first, double[] second) {
        return Math.abs(first[0] - second[0]) < 1e-9 && Math.abs(first[1] - second[1]) < 1e-9;
    }
}
