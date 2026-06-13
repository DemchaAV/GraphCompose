package com.demcha.compose.document.node;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentDashPattern;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentLineCap;
import com.demcha.compose.document.style.DocumentLineJoin;
import com.demcha.compose.document.style.DocumentPaint;
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
 * <p>Fills and strokes each take an optional gradient: {@code fillPaint}
 * wins over {@code fillColor}, and {@code strokePaint} paints the outline
 * through a PDF shading pattern while {@code stroke} keeps supplying the
 * width (and the flat-colour fallback for backends without gradients).</p>
 *
 * @param name        node name used in snapshots and layout graph paths
 * @param width       resolved box width
 * @param height      resolved box height
 * @param segments    normalized path segments; must start with a
 *                    {@link DocumentPathSegment.MoveTo}
 * @param fillColor   optional fill colour (non-zero winding rule)
 * @param fillPaint   optional gradient fill; wins over {@code fillColor}
 * @param stroke      optional outline stroke
 * @param strokePaint optional gradient for the outline; requires
 *                    {@code stroke} for the width
 * @param padding     inner padding
 * @param margin      outer margin
 * @param dashPattern dash pattern for the stroke; defaults to
 *                    {@link DocumentDashPattern#NONE} (solid)
 * @param lineCap     stroke end-cap style; defaults to
 *                    {@link DocumentLineCap#BUTT} (the PDF default)
 * @param lineJoin    stroke corner style; defaults to
 *                    {@link DocumentLineJoin#MITER} (the PDF default)
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public record PathNode(
        String name,
        double width,
        double height,
        List<DocumentPathSegment> segments,
        DocumentColor fillColor,
        DocumentPaint fillPaint,
        DocumentStroke stroke,
        DocumentPaint strokePaint,
        DocumentInsets padding,
        DocumentInsets margin,
        DocumentDashPattern dashPattern,
        DocumentLineCap lineCap,
        DocumentLineJoin lineJoin
) implements DocumentNode {
    /**
     * Validates dimensions, the segment list, and the paint pairing;
     * copy-protects the segments.
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
        lineCap = lineCap == null ? DocumentLineCap.BUTT : lineCap;
        lineJoin = lineJoin == null ? DocumentLineJoin.MITER : lineJoin;
        if (width <= 0 || Double.isNaN(width) || Double.isInfinite(width)) {
            throw new IllegalArgumentException("width must be finite and positive: " + width);
        }
        if (height <= 0 || Double.isNaN(height) || Double.isInfinite(height)) {
            throw new IllegalArgumentException("height must be finite and positive: " + height);
        }
        if (strokePaint != null && stroke == null) {
            throw new IllegalArgumentException(
                    "strokePaint needs a stroke to define the outline width — set stroke(...) too");
        }
    }

    /**
     * Compatibility constructor without paints — flat fill colour and flat
     * stroke only, the common authoring case.
     *
     * @param name        node name
     * @param width       resolved box width
     * @param height      resolved box height
     * @param segments    normalized path segments
     * @param fillColor   optional fill colour
     * @param stroke      optional outline stroke
     * @param padding     inner padding
     * @param margin      outer margin
     * @param dashPattern dash pattern for the stroke
     */
    public PathNode(String name,
                    double width,
                    double height,
                    List<DocumentPathSegment> segments,
                    DocumentColor fillColor,
                    DocumentStroke stroke,
                    DocumentInsets padding,
                    DocumentInsets margin,
                    DocumentDashPattern dashPattern) {
        this(name, width, height, segments, fillColor, null, stroke, null,
                padding, margin, dashPattern, null, null);
    }

    /**
     * Compatibility constructor with paints but default caps and joins.
     *
     * @param name        node name
     * @param width       resolved box width
     * @param height      resolved box height
     * @param segments    normalized path segments
     * @param fillColor   optional fill colour
     * @param fillPaint   optional gradient fill
     * @param stroke      optional outline stroke
     * @param strokePaint optional gradient stroke paint
     * @param padding     inner padding
     * @param margin      outer margin
     * @param dashPattern dash pattern for the stroke
     */
    public PathNode(String name,
                    double width,
                    double height,
                    List<DocumentPathSegment> segments,
                    DocumentColor fillColor,
                    DocumentPaint fillPaint,
                    DocumentStroke stroke,
                    DocumentPaint strokePaint,
                    DocumentInsets padding,
                    DocumentInsets margin,
                    DocumentDashPattern dashPattern) {
        this(name, width, height, segments, fillColor, fillPaint, stroke, strokePaint,
                padding, margin, dashPattern, null, null);
    }

    @Override
    public String nodeKind() {
        return "Path";
    }
}
