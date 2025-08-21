package com.demcha.components.geometry;

import com.demcha.components.core.Component;

/**
 * This is size of Box Component which including a Component size + margins
 * @param width
 * @param height
 */
public record BoxSize(double width, double height) implements Component {
}
