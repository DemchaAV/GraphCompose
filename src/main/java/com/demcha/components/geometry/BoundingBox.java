package com.demcha.components.geometry;

import com.demcha.components.core.Component;

/**
 * Represents the final size and position of a component, including any calculated margins.
 * <p>
 * This component stores the absolute coordinates and dimensions (the "bounding box")
 * for an entity after all layout calculations, such as margins and parent container
 * constraints, have been applied.
 *
 * @param x      The final horizontal position (X-coordinate), including margins.
 * @param y      The final vertical position (Y-coordinate), including margins.
 * @param width  The final width, including any horizontal margins.
 * @param height The final height, including any vertical margins.
 */
public record BoundingBox(double x, double y, double width, double height) implements Component {}
