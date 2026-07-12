package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.ShapePoint;

import java.util.List;
import java.util.Objects;

/**
 * Atomic filled/stroked polygon inside a fixed-size box. Vertices are
 * normalized {@link ShapePoint}s in the unit square, scaled to the node's
 * {@code width × height} at render time — the same convention
 * {@link com.demcha.compose.document.style.ShapeOutline.Polygon} uses.
 *
 * <p>This is the leaf vehicle for arbitrary closed vector geometry: chart
 * pie/donut sectors (arc-tessellated rings) today, imported icon paths
 * tomorrow. It renders through the existing polygon fragment pipeline — no
 * backend gains any new handler for it.</p>
 *
 * @param name      node name used in snapshots and layout graph paths
 * @param width     resolved box width
 * @param height    resolved box height
 * @param points    normalized vertices in draw order; at least three
 * @param fillColor optional fill colour
 * @param stroke    optional outline stroke
 * @param padding   inner padding
 * @param margin    outer margin
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public record PolygonNode(
        String name,
        double width,
        double height,
        List<ShapePoint> points,
        DocumentColor fillColor,
        DocumentStroke stroke,
        DocumentInsets padding,
        DocumentInsets margin
) implements DocumentNode {
    /**
     * Validates dimensions and the vertex list; copy-protects the vertices.
     */
    public PolygonNode {
        name = name == null ? "" : name;
        Objects.requireNonNull(points, "points");
        points = List.copyOf(points);
        if (points.size() < 3) {
            throw new IllegalArgumentException(
                    "polygon needs at least three points: " + points.size());
        }
        padding = padding == null ? DocumentInsets.zero() : padding;
        margin = margin == null ? DocumentInsets.zero() : margin;
        if (width <= 0 || Double.isNaN(width) || Double.isInfinite(width)) {
            throw new IllegalArgumentException("width must be finite and positive: " + width);
        }
        if (height <= 0 || Double.isNaN(height) || Double.isInfinite(height)) {
            throw new IllegalArgumentException("height must be finite and positive: " + height);
        }
    }

    @Override
    public String nodeKind() {
        return "Polygon";
    }
}
