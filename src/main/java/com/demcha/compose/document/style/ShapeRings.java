package com.demcha.compose.document.style;

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
