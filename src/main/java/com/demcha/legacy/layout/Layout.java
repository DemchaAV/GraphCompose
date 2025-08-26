package com.demcha.legacy.layout;

import com.demcha.components.containers.Container;

/**
 * Defines a strategy for arranging elements inside a container.
 */
public interface Layout {

    /**
     * Measure phase — calculates required size for the container based on its children.
     * @param container the container being measured
     * @param ctx the measurement context (available space, units, etc.)
     */
    void measure(Container container, MeasureCtx ctx);

    /**
     * Arrange phase — sets positions of children inside the container.
     * @param container the container being arranged
     * @param ctx the arrangement context (start coordinates, allocated size, etc.)
     */
    void arrange(Container container, ArrangeCtx ctx);
}
