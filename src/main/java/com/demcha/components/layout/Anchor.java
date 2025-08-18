package com.demcha.components.layout;

import com.demcha.components.core.Component;

/**
 * Represents an object that can be anchored to a specific position.
 *
 * <p>Implementations of this interface provide access to an {@link Anchor}
 * that defines how the object is positioned or aligned within its container
 * or relative to another element.</p>
 *
 * <p>The meaning of the anchor depends on the context — for example, in a
 * UI layout system it might define alignment (e.g., top-left, center, bottom-right),
 * while in a graphics system it might define a fixed reference point.</p>
 */
public record Anchor(HAnchor h, VAnchor v) implements Component {
    public static Anchor topLeft()   { return new Anchor(HAnchor.LEFT,   VAnchor.TOP); }
    public static Anchor center()    { return new Anchor(HAnchor.CENTER, VAnchor.MIDDLE); }
    public static Anchor topRight()  { return new Anchor(HAnchor.RIGHT,  VAnchor.TOP); }
    public static Anchor bottomLeft(){ return new Anchor(HAnchor.LEFT,   VAnchor.BOTTOM); }
    public static Anchor bottomRight(){return new Anchor(HAnchor.RIGHT,  VAnchor.BOTTOM); }

}

