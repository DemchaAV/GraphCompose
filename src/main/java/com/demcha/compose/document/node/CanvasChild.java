package com.demcha.compose.document.node;

import java.util.Objects;

/**
 * One child placed at explicit pixel coordinates inside a
 * {@link CanvasLayerNode}.
 *
 * <p>Coordinates use the on-screen convention:
 * {@code (0, 0)} is the canvas's <b>top-left</b> corner, positive
 * {@code x} extends to the right, and positive {@code y} extends
 * downward. The child's own bounding box is anchored at
 * {@code (x, y)} via the canvas's top-left origin — exactly the
 * mental model authors use when sketching badges, diagrams, or
 * pixel-perfect cover-page elements.</p>
 *
 * @param node child node placed inside the canvas
 * @param x offset from the canvas's left edge (points)
 * @param y offset from the canvas's top edge (points, positive = down)
 *
 * @author Artem Demchyshyn
 */
public record CanvasChild(DocumentNode node, double x, double y) {
    /**
     * Validates the child reference and the finite-coordinate
     * invariant.
     */
    public CanvasChild {
        Objects.requireNonNull(node, "node");
        if (Double.isNaN(x) || Double.isInfinite(x)) {
            throw new IllegalArgumentException("x must be finite: " + x);
        }
        if (Double.isNaN(y) || Double.isInfinite(y)) {
            throw new IllegalArgumentException("y must be finite: " + y);
        }
    }
}
