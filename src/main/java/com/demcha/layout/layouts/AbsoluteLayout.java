package com.demcha.layout.layouts;

import com.demcha.components.Position;
import com.demcha.components.Size;
import com.demcha.core.Element;
import com.demcha.layout.ArrangeCtx;
import com.demcha.layout.Container;
import com.demcha.layout.Layout;
import com.demcha.layout.MeasureCtx;

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
            Size size = child.get(Size.class).orElse(new Size(0, 0));

            // Calculate the maximum width and height based on the child position and size
            double childMaxX = position.x() + size.width();
            double childMaxY = position.y() + size.height();

            maxWidth = Math.max(maxWidth, childMaxX);
            maxHeight = Math.max(maxHeight, childMaxY);
        }
        final var finalMaxWidth = maxWidth;
        final var finalMaxHeight = maxHeight;

        // Set the container's size based on the maximum width and height found
        c.getElement().getOrAdd(Size.class, () -> new Size(finalMaxWidth, finalMaxHeight));
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
