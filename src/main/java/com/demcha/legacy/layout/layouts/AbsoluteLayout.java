package com.demcha.legacy.layout.layouts;

import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.layout.Position;
import com.demcha.legacy.core.Element;
import com.demcha.legacy.layout.ArrangeCtx;
import com.demcha.legacy.layout.Container;
import com.demcha.legacy.layout.Layout;
import com.demcha.legacy.layout.MeasureCtx;

/**
 * Layout that arranges child elements at absolute positions within the container.
 * Each child element can be placed anywhere in the container based on explicit X and Y coordinates.
 */
public record AbsoluteLayout() implements Layout {

    /**
     * Measures the size of the container based on its child elements' positions.
     * The container size is calculated based on the farthest child element in both X and Y directions.
     *
     * @param c   The container whose child elements are being measured.
     * @param ctx The context used for measurement, containing available space and other info.
     */
    @Override
    public void measure(Container c, MeasureCtx ctx) {
        double maxWidth = 0;
        double maxHeight = 0;

        // Iterate through all child elements to determine the maximum width and height
        for (Element child : c.getChildren()) {
            Position position = child.get(Position.class).orElse(Position.zero());
            OuterBoxSize outerBoxSize = child.get(OuterBoxSize.class).orElse(new OuterBoxSize(0, 0));

            // Calculate the maximum width and height based on the child position and outerBoxSize
            double childMaxX = position.x() + outerBoxSize.width();
            double childMaxY = position.y() + outerBoxSize.height();

            maxWidth = Math.max(maxWidth, childMaxX);
            maxHeight = Math.max(maxHeight, childMaxY);
        }
        final var finalMaxWidth = maxWidth;
        final var finalMaxHeight = maxHeight;

        // Set the container's size based on the maximum width and height found
        c.getElement().getOrAdd(OuterBoxSize.class, () -> new OuterBoxSize(finalMaxWidth, finalMaxHeight));
    }

    /**
     * Arranges the child elements at their absolute positions.
     * Each child is placed based on its Position (x, y) and its size.
     *
     * @param c   The container whose child elements are being arranged.
     * @param ctx The context used for arranging, containing the starting position and allocated space.
     */
    @Override
    public void arrange(Container c, ArrangeCtx ctx) {
        // Iterate through all child elements and set their absolute positions
        for (Element child : c.getChildren()) {
            Position position = child.get(Position.class).orElse(Position.zero());

            // Set the child’s position explicitly
            child.add(new Position(position.x(), position.y()));
        }
    }
}
