package com.demcha.loyaut_core.components.layout;

import com.demcha.loyaut_core.components.core.Component;

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
public record Transform(double scaleX, double scaleY, double rotationDeg) implements Component {
    public Transform noScale() {
        return new Transform(1.0, 1.0, 0);
    }
}

