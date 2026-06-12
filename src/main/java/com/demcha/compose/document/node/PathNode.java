package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentDashPattern;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentPathSegment;
import com.demcha.compose.document.style.DocumentStroke;

import java.util.List;
import java.util.Objects;

/**
 * Atomic filled/stroked vector path inside a fixed-size box. Segments are
 * normalized {@link DocumentPathSegment}s scaled to the node's
 * {@code width × height} at render time — the open-path, curve-capable
 * sibling of {@link PolygonNode}.
 *
 * <p>This is the leaf vehicle for arbitrary vector geometry with real cubic
 * Bézier curves: smooth chart lines compile into it, decorative design
 * shapes can be authored against it, and imported SVG paths land here
 * tomorrow. The PDF backend emits native {@code curveTo} operators, so
 * curves stay perfectly smooth at any zoom level instead of being
 * tessellated into straight pieces.</p>
 *
 * @param name      node name used in snapshots and layout graph paths
 * @param width     resolved box width
 * @param height    resolved box height
 * @param segments  normalized path segments; must start with a
 *                  {@link DocumentPathSegment.MoveTo}
 * @param fillColor optional fill colour (non-zero winding rule)
 * @param stroke    optional outline stroke
 * @param padding     inner padding
 * @param margin      outer margin
 * @param dashPattern dash pattern for the stroke; defaults to
 *                    {@link DocumentDashPattern#NONE} (solid)
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public record PathNode(
        String name,
        double width,
        double height,
        List<DocumentPathSegment> segments,
        DocumentColor fillColor,
        DocumentStroke stroke,
        DocumentInsets padding,
        DocumentInsets margin,
        DocumentDashPattern dashPattern
) implements DocumentNode {
    /**
     * Validates dimensions and the segment list; copy-protects the segments.
     */
    public PathNode {
        name = name == null ? "" : name;
        Objects.requireNonNull(segments, "segments");
        segments = List.copyOf(segments);
        if (segments.size() < 2) {
            throw new IllegalArgumentException(
                    "path needs at least a MoveTo and one drawing segment: " + segments.size());
        }
        if (!(segments.get(0) instanceof DocumentPathSegment.MoveTo)) {
            throw new IllegalArgumentException(
                    "path must start with a MoveTo segment, found: "
                    + segments.get(0).getClass().getSimpleName());
        }
        padding = padding == null ? DocumentInsets.zero() : padding;
        margin = margin == null ? DocumentInsets.zero() : margin;
        dashPattern = dashPattern == null ? DocumentDashPattern.NONE : dashPattern;
        if (width <= 0 || Double.isNaN(width) || Double.isInfinite(width)) {
            throw new IllegalArgumentException("width must be finite and positive: " + width);
        }
        if (height <= 0 || Double.isNaN(height) || Double.isInfinite(height)) {
            throw new IllegalArgumentException("height must be finite and positive: " + height);
        }
    }

    @Override
    public String nodeKind() {
        return "Path";
    }
}
