package com.demcha.compose.document.style;

import java.util.ArrayList;
import java.util.List;

/**
 * Package-private geometry helpers that compute raw vertex rings for the more
 * involved {@link ShapeOutline} figures, keeping the vector math out of the
 * public factory surface.
 *
 * <p>Coordinates are in the unit box (x right, y up) and may land slightly
 * outside {@code [0, 1]}; {@code ShapeOutline} clamps and wraps them into
 * {@link ShapePoint}s. Each method here is a candidate home for future design
 * variants (a thinner tick, a rounded tick, …).</p>
 */
final class ShapeRings {

    private ShapeRings() {
    }

    /**
     * Wraps a raw vertex ring into {@link ShapePoint}s, clamping each
     * coordinate into the unit box.
     *
     * @param raw vertex ring in unit-box coordinates (may stray slightly out)
     * @return the clamped points in the same order
     */
    static List<ShapePoint> toPoints(double[][] raw) {
        List<ShapePoint> points = new ArrayList<>(raw.length);
        for (double[] vertex : raw) {
            points.add(new ShapePoint(clampUnit(vertex[0]), clampUnit(vertex[1])));
        }
        return points;
    }

    private static double clampUnit(double value) {
        return value < 0.0 ? 0.0 : (value > 1.0 ? 1.0 : value);
    }

    /**
     * Builds the vertex ring of an {@code n}-pointed star inscribed in the unit
     * box, first outer point facing up. Outer points sit on radius 0.5; the
     * inner ring uses the true-star ratio (the inner vertices land on the
     * chords between outer points) for five-plus points, falling back to a
     * fixed spiky ratio below five where that formula degenerates.
     *
     * @param points number of outer points (caller validates {@code >= 3})
     * @return {@code 2 * points} ring vertices in draw order
     */
    static double[][] star(int points) {
        double innerRatio = points >= 5
                ? Math.cos(2 * Math.PI / points) / Math.cos(Math.PI / points)
                : 0.38;
        double innerRadius = 0.5 * innerRatio;
        double start = Math.PI / 2.0;     // first outer vertex faces up
        double[][] ring = new double[points * 2][2];
        for (int i = 0; i < points * 2; i++) {
            double radius = (i % 2 == 0) ? 0.5 : innerRadius;
            double angle = start + i * Math.PI / points;
            ring[i][0] = 0.5 + radius * Math.cos(angle);
            ring[i][1] = 0.5 + radius * Math.sin(angle);
        }
        return ring;
    }

    /**
     * Builds the vertex ring of a regular {@code sides}-gon inscribed in the
     * unit box, first vertex facing up.
     *
     * @param sides number of sides (caller validates {@code >= 3})
     * @return {@code sides} ring vertices in draw order
     */
    static double[][] regularPolygon(int sides) {
        double start = Math.PI / 2.0;
        double[][] ring = new double[sides][2];
        for (int i = 0; i < sides; i++) {
            double angle = start + i * 2.0 * Math.PI / sides;
            ring[i][0] = 0.5 + 0.5 * Math.cos(angle);
            ring[i][1] = 0.5 + 0.5 * Math.sin(angle);
        }
        return ring;
    }

    /**
     * Reorients a right-pointing base ring toward {@code direction} by the
     * axis-aligned remap SVG figures use (mirror for LEFT, transpose for
     * UP/DOWN). Coordinates stay in the unit box.
     *
     * @param base      right-pointing vertex ring
     * @param direction target direction; {@code null} keeps RIGHT
     * @return the reoriented ring
     */
    static double[][] directional(double[][] base, ShapeOutline.Direction direction) {
        ShapeOutline.Direction resolved = direction == null ? ShapeOutline.Direction.RIGHT : direction;
        double[][] ring = new double[base.length][2];
        for (int i = 0; i < base.length; i++) {
            double x = base[i][0];
            double y = base[i][1];
            switch (resolved) {
                case LEFT -> {
                    ring[i][0] = 1.0 - x;
                    ring[i][1] = y;
                }
                case UP -> {
                    ring[i][0] = y;
                    ring[i][1] = x;
                }
                case DOWN -> {
                    ring[i][0] = y;
                    ring[i][1] = 1.0 - x;
                }
                default -> {
                    ring[i][0] = x;
                    ring[i][1] = y;
                }
            }
        }
        return ring;
    }

    /**
     * Builds a constant-width checkmark band of perpendicular half-thickness
     * {@code half} around a fixed left-tip → elbow → right-tip centreline, with a
     * mitred elbow and flat-cut tips. Larger {@code half} reads as a bolder tick.
     *
     * @param half perpendicular half-thickness of the band, in unit-box units
     * @return six ring vertices in draw order: outer elbow, right tip (outer then
     * inner), inner elbow, left tip (inner then outer)
     */
    static double[][] checkmarkBand(double half) {
        double[] left = {0.12, 0.50};
        double[] elbow = {0.42, 0.18};
        double[] right = {0.92, 0.84};
        double[] toRight = unit(right[0] - elbow[0], right[1] - elbow[1]);
        double[] toLeft = unit(left[0] - elbow[0], left[1] - elbow[1]);
        double[] normalRight = outwardNormal(toRight);
        double[] normalLeft = outwardNormal(toLeft);
        double[] bisector = unit(normalRight[0] + normalLeft[0], normalRight[1] + normalLeft[1]);
        double miter = half / (bisector[0] * normalRight[0] + bisector[1] * normalRight[1]);
        return new double[][]{
                {elbow[0] + bisector[0] * miter, elbow[1] + bisector[1] * miter},     // outer elbow
                {right[0] + normalRight[0] * half, right[1] + normalRight[1] * half},  // right tip, outer
                {right[0] - normalRight[0] * half, right[1] - normalRight[1] * half},  // right tip, inner
                {elbow[0] - bisector[0] * miter, elbow[1] - bisector[1] * miter},      // inner elbow
                {left[0] - normalLeft[0] * half, left[1] - normalLeft[1] * half},      // left tip, inner
                {left[0] + normalLeft[0] * half, left[1] + normalLeft[1] * half}       // left tip, outer
        };
    }

    /**
     * Returns the unit vector along {@code (x, y)}, or the zero vector for zero length.
     */
    private static double[] unit(double x, double y) {
        double length = Math.hypot(x, y);
        return length == 0 ? new double[]{0.0, 0.0} : new double[]{x / length, y / length};
    }

    /**
     * Returns the normal of {@code u} flipped to point toward the bottom (convex) side.
     */
    private static double[] outwardNormal(double[] u) {
        double nx = u[1];
        double ny = -u[0];
        if (ny > 0) {
            return new double[]{-nx, -ny};
        }
        return new double[]{nx, ny};
    }
}
