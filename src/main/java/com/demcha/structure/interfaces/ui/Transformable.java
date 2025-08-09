package com.demcha.structure.interfaces.ui;

/**
 * Represents an object that supports geometric transformations such as
 * scaling and rotation.
 *
 * <p>Transformations affect the rendering or logical representation of
 * an object, without necessarily changing its underlying data.</p>
 *
 * <ul>
 *   <li><b>Scale X / Scale Y</b> – Horizontal and vertical scaling factors
 *       (1.0 means no scaling, values &gt; 1 enlarge, values between 0 and 1 shrink).</li>
 *   <li><b>Rotation</b> – Rotation of the object in <b>degrees</b>, measured
 *       clockwise, where 0° represents the original orientation.</li>
 * </ul>
 */
public interface Transformable {

    /**
     * Returns the horizontal scaling factor.
     *
     * @return the X scale factor.
     */
    double getScaleX();

    /**
     * Returns the vertical scaling factor.
     *
     * @return the Y scale factor.
     */
    double getScaleY();

    /**
     * Returns the current rotation angle of this object.
     *
     * @return the rotation in degrees, measured clockwise.
     */
    double getRotation();

    /**
     * Sets the horizontal and vertical scaling factors.
     *
     * @param sx the horizontal scale factor.
     * @param sy the vertical scale factor.
     */
    void setScale(double sx, double sy);

    /**
     * Sets the rotation of this object.
     *
     * @param angle the rotation in degrees, measured clockwise.
     */
    void setRotation(double angle);
}

