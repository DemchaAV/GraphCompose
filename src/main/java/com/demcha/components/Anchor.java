package com.demcha.components;

import com.demcha.core.Component;
import com.demcha.layout.AnchorType;

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
public record Anchor(AnchorType type) implements Component {}

