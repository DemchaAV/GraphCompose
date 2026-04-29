package com.demcha.compose.document.style;

/**
 * Canonical-surface value object that carries a 2-D affine transformation
 * (rotation around the placement centre plus uniform or non-uniform
 * scaling) for a node. The transform is a render-time concern: the
 * canonical layout compiler still measures and places nodes against
 * their natural bbox, and backends apply the transform when drawing.
 *
 * <p>Mirrors {@code com.demcha.compose.engine.components.layout.Transform}
 * one-to-one but lives on the canonical side so the public DSL never
 * leaks engine.* types (see {@code PublicApiNoEngineLeakTest}).</p>
 *
 * <p>Conventions:</p>
 * <ul>
 *   <li>{@link #rotationDegrees()} — clockwise rotation in degrees,
 *       {@code 0.0} means no rotation. Backends rotate around the
 *       placement centre, not the page origin.</li>
 *   <li>{@link #scaleX()} / {@link #scaleY()} — multiplicative scaling
 *       factors, {@code 1.0} means no scaling, {@code 0.5} halves,
 *       {@code 2.0} doubles. Negative values are allowed (mirror).</li>
 * </ul>
 *
 * @param rotationDegrees clockwise rotation in degrees
 * @param scaleX horizontal scale factor (must be finite, non-zero)
 * @param scaleY vertical scale factor (must be finite, non-zero)
 *
 * @author Artem Demchyshyn
 */
public record DocumentTransform(double rotationDegrees, double scaleX, double scaleY) {

    /** Identity transform — no rotation, no scaling. */
    public static final DocumentTransform NONE = new DocumentTransform(0.0, 1.0, 1.0);

    /**
     * Validates that the rotation is finite and the scale factors are
     * finite and non-zero. Zero scale would collapse the geometry to a
     * point and is almost always a caller mistake.
     */
    public DocumentTransform {
        requireFinite("rotationDegrees", rotationDegrees);
        requireFiniteNonZero("scaleX", scaleX);
        requireFiniteNonZero("scaleY", scaleY);
    }

    /**
     * @return the identity transform (alias for {@link #NONE})
     */
    public static DocumentTransform none() {
        return NONE;
    }

    /**
     * @param degrees clockwise rotation in degrees
     * @return a transform that only rotates, with identity scaling
     */
    public static DocumentTransform rotate(double degrees) {
        return new DocumentTransform(degrees, 1.0, 1.0);
    }

    /**
     * Uniform scaling — both axes share the same factor.
     *
     * @param uniformFactor scale factor for both axes
     * @return a transform that only scales, with no rotation
     */
    public static DocumentTransform scale(double uniformFactor) {
        return new DocumentTransform(0.0, uniformFactor, uniformFactor);
    }

    /**
     * Non-uniform scaling.
     *
     * @param scaleX horizontal scale factor
     * @param scaleY vertical scale factor
     * @return a transform that only scales, with no rotation
     */
    public static DocumentTransform scale(double scaleX, double scaleY) {
        return new DocumentTransform(0.0, scaleX, scaleY);
    }

    /**
     * @param degrees clockwise rotation in degrees
     * @return a copy of this transform with the rotation replaced
     */
    public DocumentTransform withRotation(double degrees) {
        return new DocumentTransform(degrees, scaleX, scaleY);
    }

    /**
     * @param scaleX horizontal scale factor
     * @param scaleY vertical scale factor
     * @return a copy of this transform with the scale factors replaced
     */
    public DocumentTransform withScale(double scaleX, double scaleY) {
        return new DocumentTransform(rotationDegrees, scaleX, scaleY);
    }

    /**
     * @return {@code true} when the transform is the identity (no
     *         rotation, no scaling); backends may skip CTM push/pop
     *         when this is the case
     */
    public boolean isIdentity() {
        return rotationDegrees == 0.0 && scaleX == 1.0 && scaleY == 1.0;
    }

    private static void requireFinite(String label, double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(label + " must be finite: " + value);
        }
    }

    private static void requireFiniteNonZero(String label, double value) {
        requireFinite(label, value);
        if (value == 0.0) {
            throw new IllegalArgumentException(label + " must be non-zero (a zero scale collapses the geometry)");
        }
    }
}
