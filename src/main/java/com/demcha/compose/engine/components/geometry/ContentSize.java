package com.demcha.compose.engine.components.geometry;

import com.demcha.compose.engine.components.core.Component;

/**
 * Declared content box size for an entity.
 * <p>
 * Builders usually write this component when they know or can measure the size
 * of the entity's content. Layout and pagination later read it together with
 * margin and padding to derive outer-box size and final placement.
 * </p>
 */
public record ContentSize(double width, double height) implements Component {
}
