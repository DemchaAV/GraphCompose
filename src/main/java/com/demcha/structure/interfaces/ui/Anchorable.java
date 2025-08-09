package com.demcha.structure.interfaces.ui;

import com.demcha.structure.Anchor;

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
public interface Anchorable {

    /**
     * Returns the current {@link Anchor} of this object.
     *
     * @return the current anchor, or {@code null} if none is set.
     */
    Anchor getAnchor();

    /**
     * Sets the {@link Anchor} for this object.
     *
     * @param anchor the anchor to set; may be {@code null} to remove anchoring.
     */
    void setAnchor(Anchor anchor);
}
