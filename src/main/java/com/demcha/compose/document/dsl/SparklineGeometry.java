package com.demcha.compose.document.dsl;

import com.demcha.compose.document.style.ShapePoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure vertex math for inline sparklines: normalizes a value run into the unit
 * box and produces closed polygon rings — a filled area silhouette, or a
 * constant-thickness ribbon that reads as a stroked polyline. Deterministic
 * arithmetic only, unit-tested in isolation.
 *
 * <p>Values map linearly: the run's minimum sits on {@code y = 0}, its maximum
 * on {@code y = 1}; a flat run centres on {@code y = 0.5}. Data points are
 * evenly spaced across {@code x = 0..1}, and the polyline between them is
 * smoothed with the same uniform Catmull-Rom curve the chart engine uses,
 * densified to {@value #SMOOTH_SUBDIVISIONS} sub-segments per span — at
 * sparkline sizes the facets are far below visual resolution, so the run
 * reads as a true curve while staying a deterministic polygon ring.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
final class SparklineGeometry {

    /**
     * Sub-segments per data span. Inline shapes stay polygon rings, so the
     * curve is densified instead of emitted as Béziers; 12 segments on a
     * ~40 pt sparkline puts every facet under half a point.
     */
    private static final int SMOOTH_SUBDIVISIONS = 12;

    private SparklineGeometry() {
    }

    /**
     * Area silhouette: the smoothed value curve closed down to the baseline.
     *
     * @param values at least two finite values
     * @return closed ring of smoothed normalized vertices
     */
    static List<ShapePoint> areaPoints(double[] values) {
        double[][] curve = smoothCurve(normalize(values));
        List<ShapePoint> points = new ArrayList<>(curve.length + 2);
        for (double[] p : curve) {
            points.add(new ShapePoint(p[0], p[1]));
        }
        points.add(new ShapePoint(1.0, 0.0));
        points.add(new ShapePoint(0.0, 0.0));
        return points;
    }

    /**
     * Ribbon polyline: the value run offset vertically by ± half the thickness
     * fraction, forming a closed band that renders as a stroked line. The
     * offset is vertical (not perpendicular), which is visually equivalent at
     * sparkline sizes and keeps the maths exact.
     *
     * @param values            at least two finite values
     * @param thicknessFraction band thickness as a fraction of the box height, in (0, 1)
     * @return closed ring of smoothed normalized vertices (top edge forward,
 *         bottom edge back)
     */
    static List<ShapePoint> ribbonPoints(double[] values, double thicknessFraction) {
        if (thicknessFraction <= 0 || thicknessFraction >= 1 || Double.isNaN(thicknessFraction)) {
            throw new IllegalArgumentException(
                    "thicknessFraction must be in (0,1): " + thicknessFraction);
        }
        double half = thicknessFraction / 2.0;
        double[] ys = normalize(values);
        // Compress the run into [half, 1 - half] so the band keeps its full
        // thickness at the extremes instead of being clamped at the box edge.
        for (int i = 0; i < ys.length; i++) {
            ys[i] = half + ys[i] * (1.0 - thicknessFraction);
        }
        double[][] curve = smoothCurve(ys);
        // Clamp the band CENTRE into [half, 1 - half] (spline overshoot may
        // poke past the compressed range) so the ±half offsets stay inside
        // the unit box without eating into the band thickness.
        List<ShapePoint> points = new ArrayList<>(curve.length * 2);
        for (double[] p : curve) {
            double centre = Math.max(half, Math.min(1.0 - half, p[1]));
            points.add(new ShapePoint(p[0], centre + half));
        }
        for (int i = curve.length - 1; i >= 0; i--) {
            double centre = Math.max(half, Math.min(1.0 - half, curve[i][1]));
            points.add(new ShapePoint(curve[i][0], centre - half));
        }
        return points;
    }

    /**
     * Densifies the evenly-spaced value run with a uniform Catmull-Rom curve
     * (tension 0.5, clamped endpoints) — the same spline the chart engine
     * draws as native Béziers. Returns {@code (x, y)} samples including every
     * original point; y is clamped to the unit box because the spline may
     * overshoot slightly around extremes.
     */
    private static double[][] smoothCurve(double[] ys) {
        int spans = ys.length - 1;
        double[][] out = new double[spans * SMOOTH_SUBDIVISIONS + 1][2];
        out[0] = new double[]{0.0, clamp01(ys[0])};
        int n = 1;
        for (int i = 0; i < spans; i++) {
            double p0 = ys[Math.max(0, i - 1)];
            double p1 = ys[i];
            double p2 = ys[i + 1];
            double p3 = ys[Math.min(ys.length - 1, i + 2)];
            for (int s = 1; s <= SMOOTH_SUBDIVISIONS; s++) {
                double t = (double) s / SMOOTH_SUBDIVISIONS;
                double t2 = t * t;
                double t3 = t2 * t;
                double y = 0.5 * ((2 * p1)
                                  + (-p0 + p2) * t
                                  + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2
                                  + (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
                double x = (i + t) / spans;
                out[n++] = new double[]{x, clamp01(y)};
            }
        }
        return out;
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private static double[] normalize(double[] values) {
        if (values == null || values.length < 2) {
            throw new IllegalArgumentException("sparkline needs at least two values");
        }
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double v : values) {
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                throw new IllegalArgumentException("sparkline values must be finite: " + v);
            }
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        double[] ys = new double[values.length];
        if (max - min < 1e-12) {
            java.util.Arrays.fill(ys, 0.5);
            return ys;
        }
        for (int i = 0; i < values.length; i++) {
            ys[i] = (values[i] - min) / (max - min);
        }
        return ys;
    }
}
