package com.demcha.compose.document.chart;

import com.demcha.compose.document.node.DocumentNode;

import java.util.Objects;

/**
 * One positioned primitive emitted by {@link ChartLayoutResolver}: an existing
 * document node placed in an explicit box inside the chart's inner content area.
 *
 * <p>Coordinates use the engine's <b>bottom-up</b> convention — {@code (0, 0)}
 * is the bottom-left corner of the chart's inner content box, {@code x} grows
 * right, {@code y} grows up — so {@link com.demcha.compose.document.layout.definitions.ChartDefinition}
 * can translate them into child fragment offsets with no axis flip. The
 * {@code width}/{@code height} are the box the child is measured and emitted
 * within (matching the node's intrinsic size for shapes and lines, and the
 * label box for paragraphs).</p>
 *
 * @param node   the primitive node (bar, line segment, grid line, label, swatch)
 * @param x      left edge of the box, points from the inner box's left edge
 * @param y      bottom edge of the box, points from the inner box's bottom edge
 * @param width  box width in points (positive)
 * @param height box height in points (positive)
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public record ChartPrimitive(DocumentNode node, double x, double y, double width, double height) {
    /**
     * Validates the node reference, finite coordinates, and positive box.
     */
    public ChartPrimitive {
        Objects.requireNonNull(node, "node");
        requireFinite(x, "x");
        requireFinite(y, "y");
        if (width <= 0 || Double.isNaN(width) || Double.isInfinite(width)) {
            throw new IllegalArgumentException("width must be finite and positive: " + width);
        }
        if (height <= 0 || Double.isNaN(height) || Double.isInfinite(height)) {
            throw new IllegalArgumentException("height must be finite and positive: " + height);
        }
    }

    private static void requireFinite(double v, String name) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            throw new IllegalArgumentException(name + " must be finite: " + v);
        }
    }
}
