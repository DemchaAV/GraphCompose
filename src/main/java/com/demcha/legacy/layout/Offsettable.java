package com.demcha.legacy.layout;

/**
 * Represents an object that can be positioned using an X and Y offset.
 *
 * <p>Offsets are typically applied relative to an object's default or
 * anchored position. This allows shifting the element horizontally
 * and vertically without changing its base coordinates.</p>
 *
 * <ul>
 *   <li><b>Offset X</b> – Horizontal shift (positive values move right, negative left).</li>
 *   <li><b>Offset Y</b> – Vertical shift (positive values move down, negative up).</li>
 * </ul>
 */
public interface Offsettable {

    /**
     * Returns the horizontal offset.
     *
     * @return the X offset value.
     */
    double getOffsetX();

    /**
     * Returns the vertical offset.
     *
     * @return the Y offset value.
     */
    double getOffsetY();

    /**
     * Sets both horizontal and vertical offsets at once.
     *
     * @param dx the horizontal offset (positive values move right, negative left).
     * @param dy the vertical offset (positive values move down, negative up).
     */
    void setOffset(double dx, double dy);
}
