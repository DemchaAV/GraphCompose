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
 * on {@code y = 1}; a flat run centres on {@code y = 0.5}. Points are evenly
 * spaced across {@code x = 0..1}.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
final class SparklineGeometry {

    private SparklineGeometry() {
    }

    /**
     * Area silhouette: the value polyline closed down to the baseline.
     *
     * @param values at least two finite values
     * @return closed ring of {@code n + 2} normalized vertices
     */
    static List<ShapePoint> areaPoints(double[] values) {
        double[] ys = normalize(values);
        List<ShapePoint> points = new ArrayList<>(ys.length + 2);
        for (int i = 0; i < ys.length; i++) {
            points.add(new ShapePoint(x(i, ys.length), ys[i]));
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
     * @param values at least two finite values
     * @param thicknessFraction band thickness as a fraction of the box height, in (0, 1)
     * @return closed ring of {@code 2n} normalized vertices
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
        List<ShapePoint> points = new ArrayList<>(ys.length * 2);
        for (int i = 0; i < ys.length; i++) {
            points.add(new ShapePoint(x(i, ys.length), ys[i] + half));
        }
        for (int i = ys.length - 1; i >= 0; i--) {
            points.add(new ShapePoint(x(i, ys.length), ys[i] - half));
        }
        return points;
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

    private static double x(int index, int count) {
        return (double) index / (count - 1);
    }
}
