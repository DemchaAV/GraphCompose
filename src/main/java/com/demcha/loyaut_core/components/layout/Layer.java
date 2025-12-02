package com.demcha.loyaut_core.components.layout;

import com.demcha.loyaut_core.components.core.Component;

/**
 * Represents an object that exists on a specific layer.
 *
 * <p>In graphical systems, rendering engines, or UI frameworks,
 * a <b>layer</b> is often used to determine the drawing order or
 * stacking position of elements. Lower layer numbers are typically
 * drawn first (behind others), while higher numbers appear on top.</p>
 */
public record Layer(int value) implements Component {}


