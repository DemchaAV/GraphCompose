package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.PathNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentPathSegment;
import com.demcha.compose.document.style.DocumentStroke;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder for semantic vector-path nodes — free-form design shapes with
 * native cubic Bézier curves.
 *
 * <p>Segments use normalized unit-box coordinates in the PDF orientation
 * ({@code (0, 0)} = bottom-left, {@code y} grows upward) and are scaled to
 * the node's {@code width × height} at render time; Bézier control points
 * may overshoot the box. Start with {@link #moveTo}, then chain
 * {@link #lineTo} / {@link #curveTo}, and optionally {@link #closePath()}
 * before {@link #build()}:</p>
 *
 * <pre>{@code
 * flow.addPath(path -> path
 *         .name("Wave")
 *         .size(320, 60)
 *         .moveTo(0.0, 0.5)
 *         .curveTo(0.25, 1.0, 0.25, 0.0, 0.5, 0.5)
 *         .curveTo(0.75, 1.0, 0.75, 0.0, 1.0, 0.5)
 *         .stroke(DocumentStroke.of(accent, 2)));
 * }</pre>
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public final class PathBuilder {
    private final List<DocumentPathSegment> segments = new ArrayList<>();
    private String name = "";
    private double width;
    private double height;
    private DocumentColor fillColor;
    private DocumentStroke stroke;
    private DocumentInsets padding = DocumentInsets.zero();
    private DocumentInsets margin = DocumentInsets.zero();

    /**
     * Creates a path builder.
     */
    public PathBuilder() {
    }

    /**
     * Sets the path node name.
     *
     * @param name name used in snapshots and layout graph paths
     * @return this builder
     */
    public PathBuilder name(String name) {
        this.name = name == null ? "" : name;
        return this;
    }

    /**
     * Sets the path box width.
     *
     * @param width width in points
     * @return this builder
     */
    public PathBuilder width(double width) {
        this.width = width;
        return this;
    }

    /**
     * Sets the path box height.
     *
     * @param height height in points
     * @return this builder
     */
    public PathBuilder height(double height) {
        this.height = height;
        return this;
    }

    /**
     * Sets the path box width and height.
     *
     * @param width  width in points
     * @param height height in points
     * @return this builder
     */
    public PathBuilder size(double width, double height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /**
     * Starts a new subpath at the given normalized point.
     *
     * @param x normalized horizontal position (0 = left edge, 1 = right edge)
     * @param y normalized vertical position (0 = bottom edge, 1 = top edge)
     * @return this builder
     */
    public PathBuilder moveTo(double x, double y) {
        segments.add(DocumentPathSegment.moveTo(x, y));
        return this;
    }

    /**
     * Draws a straight line from the current point.
     *
     * @param x normalized horizontal target
     * @param y normalized vertical target
     * @return this builder
     */
    public PathBuilder lineTo(double x, double y) {
        segments.add(DocumentPathSegment.lineTo(x, y));
        return this;
    }

    /**
     * Draws a cubic Bézier curve from the current point. Control points may
     * overshoot the unit box.
     *
     * @param control1X first control point, horizontal
     * @param control1Y first control point, vertical
     * @param control2X second control point, horizontal
     * @param control2Y second control point, vertical
     * @param x         end point, horizontal
     * @param y         end point, vertical
     * @return this builder
     */
    public PathBuilder curveTo(double control1X, double control1Y,
                               double control2X, double control2Y,
                               double x, double y) {
        segments.add(DocumentPathSegment.cubicTo(control1X, control1Y, control2X, control2Y, x, y));
        return this;
    }

    /**
     * Closes the current subpath back to its last {@link #moveTo}.
     *
     * @return this builder
     */
    public PathBuilder closePath() {
        segments.add(DocumentPathSegment.close());
        return this;
    }

    /**
     * Sets the fill color (non-zero winding rule).
     *
     * @param fillColor fill color
     * @return this builder
     */
    public PathBuilder fillColor(DocumentColor fillColor) {
        this.fillColor = fillColor;
        return this;
    }

    /**
     * Sets the fill color (non-zero winding rule).
     *
     * @param fillColor fill color
     * @return this builder
     */
    public PathBuilder fillColor(Color fillColor) {
        this.fillColor = fillColor == null ? null : DocumentColor.of(fillColor);
        return this;
    }

    /**
     * Sets the outline stroke.
     *
     * @param stroke stroke descriptor, or {@code null} for no stroke
     * @return this builder
     */
    public PathBuilder stroke(DocumentStroke stroke) {
        this.stroke = stroke;
        return this;
    }

    /**
     * Sets the path padding.
     *
     * @param padding padding in points
     * @return this builder
     */
    public PathBuilder padding(DocumentInsets padding) {
        this.padding = padding == null ? DocumentInsets.zero() : padding;
        return this;
    }

    /**
     * Sets the path margin.
     *
     * @param margin margin in points
     * @return this builder
     */
    public PathBuilder margin(DocumentInsets margin) {
        this.margin = margin == null ? DocumentInsets.zero() : margin;
        return this;
    }

    /**
     * Builds the path node. The built node copies the segment list, so the
     * builder may keep accumulating segments afterwards — each {@code build()}
     * snapshots the configuration at that moment.
     *
     * @return path node
     * @throws IllegalArgumentException if the segments do not start with a
     *                                  move-to, fewer than two segments were
     *                                  added, or the box is not positive
     */
    public PathNode build() {
        return new PathNode(name, width, height, segments, fillColor, stroke, padding, margin);
    }
}
