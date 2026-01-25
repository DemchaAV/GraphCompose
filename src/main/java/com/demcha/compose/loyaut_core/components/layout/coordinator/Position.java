package com.demcha.compose.loyaut_core.components.layout.coordinator;

import com.demcha.compose.loyaut_core.components.core.Component;

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
public record Position(double x, double y) implements Component {
    public static Position zero(){
        return new Position(0, 0);
    }
}

