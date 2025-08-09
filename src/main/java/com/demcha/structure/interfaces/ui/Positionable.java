package com.demcha.structure.interfaces.ui;

/**
 * Represents an object that has a two-dimensional position.
 *
 * <p>Coordinates are typically defined in a Cartesian coordinate system,
 * where <b>X</b> represents the horizontal axis and <b>Y</b> represents
 * the vertical axis.</p>
 *
 * <ul>
 *   <li><b>X</b> – Horizontal position (increasing values move right).</li>
 *   <li><b>Y</b> – Vertical position (increasing values move down, unless using
 *       a mathematical coordinate system where values increase upward).</li>
 * </ul>
 */
public interface Positionable {

    /**
     * Returns the X coordinate of this object.
     *
     * @return the horizontal position.
     */
    double getX();

    /**
     * Returns the Y coordinate of this object.
     *
     * @return the vertical position.
     */
    double getY();

    /**
     * Sets both X and Y coordinates of this object.
     *
     * @param x the horizontal position.
     * @param y the vertical position.
     */
    void setPosition(double x, double y);
}

