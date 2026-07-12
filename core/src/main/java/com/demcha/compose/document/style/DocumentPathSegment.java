package com.demcha.compose.document.style;

/**
 * One segment of a {@link com.demcha.compose.document.node.PathNode} outline.
 *
 * <p>Coordinates are normalized to the node's unit box and follow the same
 * orientation as {@link ShapePoint}: {@code (0, 0)} is the bottom-left corner
 * and {@code y} grows upward (the PDF convention). The node scales them to
 * its resolved {@code width × height} at render time. Unlike
 * {@link ShapePoint}, values are <em>not</em> clamped to {@code [0, 1]} —
 * Bézier control points legitimately overshoot the box, and end points may
 * too; geometry outside the box simply draws outside the node's bounds.</p>
 *
 * <p>A path begins with {@link MoveTo}; {@link CubicTo} spans a cubic Bézier
 * curve with two control points; {@link Close} closes the current subpath
 * back to its last {@code MoveTo}. Multiple subpaths (further {@code MoveTo}
 * segments) are allowed. Filling uses the PDF non-zero winding rule, and an
 * unclosed subpath is closed implicitly by the fill while a stroke leaves it
 * open.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public sealed interface DocumentPathSegment
        permits DocumentPathSegment.MoveTo, DocumentPathSegment.LineTo,
        DocumentPathSegment.CubicTo, DocumentPathSegment.Close {

    /**
     * Starts a new subpath at the given normalized point.
     *
     * @param x normalized horizontal position (0 = left edge, 1 = right edge)
     * @param y normalized vertical position (0 = bottom edge, 1 = top edge)
     * @return a {@code MoveTo} segment
     */
    static MoveTo moveTo(double x, double y) {
        return new MoveTo(x, y);
    }

    /**
     * Draws a straight line from the current point.
     *
     * @param x normalized horizontal target
     * @param y normalized vertical target
     * @return a {@code LineTo} segment
     */
    static LineTo lineTo(double x, double y) {
        return new LineTo(x, y);
    }

    /**
     * Draws a cubic Bézier curve from the current point.
     *
     * @param control1X normalized horizontal position of the first control point
     * @param control1Y normalized vertical position of the first control point
     * @param control2X normalized horizontal position of the second control point
     * @param control2Y normalized vertical position of the second control point
     * @param x         normalized horizontal end point
     * @param y         normalized vertical end point
     * @return a {@code CubicTo} segment
     */
    static CubicTo cubicTo(double control1X, double control1Y,
                           double control2X, double control2Y,
                           double x, double y) {
        return new CubicTo(control1X, control1Y, control2X, control2Y, x, y);
    }

    /**
     * Closes the current subpath back to its last {@code MoveTo}.
     *
     * @return a {@code Close} segment
     */
    static Close close() {
        return new Close();
    }

    /**
     * Starts a new subpath.
     *
     * @param x normalized horizontal position
     * @param y normalized vertical position
     * @since 1.8.0
     */
    record MoveTo(double x, double y) implements DocumentPathSegment {
        /**
         * Validates that both coordinates are finite.
         */
        public MoveTo {
            requireFinite("x", x);
            requireFinite("y", y);
        }
    }

    /**
     * Straight line to the given point.
     *
     * @param x normalized horizontal target
     * @param y normalized vertical target
     * @since 1.8.0
     */
    record LineTo(double x, double y) implements DocumentPathSegment {
        /**
         * Validates that both coordinates are finite.
         */
        public LineTo {
            requireFinite("x", x);
            requireFinite("y", y);
        }
    }

    /**
     * Cubic Bézier curve to the given end point.
     *
     * @param control1X normalized first control point, horizontal
     * @param control1Y normalized first control point, vertical
     * @param control2X normalized second control point, horizontal
     * @param control2Y normalized second control point, vertical
     * @param x         normalized end point, horizontal
     * @param y         normalized end point, vertical
     * @since 1.8.0
     */
    record CubicTo(double control1X, double control1Y,
                   double control2X, double control2Y,
                   double x, double y) implements DocumentPathSegment {
        /**
         * Validates that every coordinate is finite.
         */
        public CubicTo {
            requireFinite("control1X", control1X);
            requireFinite("control1Y", control1Y);
            requireFinite("control2X", control2X);
            requireFinite("control2Y", control2Y);
            requireFinite("x", x);
            requireFinite("y", y);
        }
    }

    /**
     * Closes the current subpath.
     *
     * @since 1.8.0
     */
    record Close() implements DocumentPathSegment {
    }

    private static void requireFinite(String label, double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(label + " must be finite: " + value);
        }
    }
}
