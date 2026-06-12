package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.style.DocumentDashPattern;
import com.demcha.compose.document.style.DocumentPathSegment;
import com.demcha.compose.document.style.ShapePoint;
import com.demcha.compose.engine.components.content.shape.Stroke;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.awt.*;
import java.io.IOException;
import java.util.List;

/**
 * Shared PDF path helpers for shape geometry, so block render, clip masking and
 * inline shape render emit identical paths from one source.
 */
final class PdfShapeGeometry {
    private static final float BEZIER_CIRCLE_CONSTANT = 0.552284749831f;

    private PdfShapeGeometry() {
    }

    /**
     * Paints a path with optional fill and/or stroke, sharing the
     * save/restore + colour setup + fill/stroke selection across every shape
     * render handler. No-op when neither a fill nor a visible stroke is present.
     */
    static void fillAndStrokePath(PDPageContentStream stream,
                                  Color fillColor,
                                  Stroke stroke,
                                  PathEmitter path) throws IOException {
        fillAndStrokePath(stream, fillColor, stroke, null, path);
    }

    /**
     * Variant of {@link #fillAndStrokePath(PDPageContentStream, Color, Stroke, PathEmitter)}
     * with an optional dash pattern applied to the stroke inside the saved
     * graphics state ({@code null} or {@link DocumentDashPattern#NONE} keeps
     * the stroke solid).
     */
    static void fillAndStrokePath(PDPageContentStream stream,
                                  Color fillColor,
                                  Stroke stroke,
                                  DocumentDashPattern dashPattern,
                                  PathEmitter path) throws IOException {
        boolean hasFill = fillColor != null;
        boolean hasStroke = stroke != null
                            && stroke.strokeColor() != null
                            && stroke.strokeColor().color() != null
                            && stroke.width() > 0;
        if (!hasFill && !hasStroke) {
            return;
        }
        stream.saveGraphicsState();
        try {
            if (hasStroke) {
                PdfAlphaSupport.applyStrokeAlpha(stream, stroke.strokeColor().color());
                stream.setStrokingColor(stroke.strokeColor().color());
                stream.setLineWidth((float) stroke.width());
                applyDashPattern(stream, dashPattern);
            }
            if (hasFill) {
                PdfAlphaSupport.applyFillAlpha(stream, fillColor);
                stream.setNonStrokingColor(fillColor);
            }
            path.emit(stream);
            if (hasFill && hasStroke) {
                stream.fillAndStroke();
            } else if (hasFill) {
                stream.fill();
            } else {
                stream.stroke();
            }
        } finally {
            stream.restoreGraphicsState();
        }
    }

    /**
     * Appends a closed polygon path to the stream. Normalized vertices (see
     * {@link ShapePoint}) are scaled into the {@code [x, x+width] × [y, y+height]}
     * box; the caller fills/strokes/clips the resulting path.
     */
    static void addPolygonPath(PDPageContentStream stream,
                               float x,
                               float y,
                               float width,
                               float height,
                               List<ShapePoint> points) throws IOException {
        ShapePoint first = points.get(0);
        stream.moveTo(x + (float) (first.x() * width), y + (float) (first.y() * height));
        for (int i = 1; i < points.size(); i++) {
            ShapePoint point = points.get(i);
            stream.lineTo(x + (float) (point.x() * width), y + (float) (point.y() * height));
        }
        stream.closePath();
    }

    /**
     * Appends a normalized segment path scaled to the fragment box, emitting
     * native line and cubic-Bézier operators. Segments follow the
     * {@link DocumentPathSegment} contract: normalized unit-box coordinates,
     * PDF y-up orientation, {@code MoveTo} first, control points free to
     * overshoot the box.
     */
    static void addPathSegments(PDPageContentStream stream,
                                float x,
                                float y,
                                float width,
                                float height,
                                List<DocumentPathSegment> segments) throws IOException {
        for (DocumentPathSegment segment : segments) {
            if (segment instanceof DocumentPathSegment.MoveTo move) {
                stream.moveTo(x + (float) (move.x() * width), y + (float) (move.y() * height));
            } else if (segment instanceof DocumentPathSegment.LineTo line) {
                stream.lineTo(x + (float) (line.x() * width), y + (float) (line.y() * height));
            } else if (segment instanceof DocumentPathSegment.CubicTo cubic) {
                stream.curveTo(
                        x + (float) (cubic.control1X() * width), y + (float) (cubic.control1Y() * height),
                        x + (float) (cubic.control2X() * width), y + (float) (cubic.control2Y() * height),
                        x + (float) (cubic.x() * width), y + (float) (cubic.y() * height));
            } else if (segment instanceof DocumentPathSegment.Close) {
                stream.closePath();
            }
        }
    }

    /**
     * Appends a closed rounded-rectangle path whose four corners may have
     * independent radii. Each radius gets its own Bezier arc; a zero radius
     * keeps a sharp 90° corner. PDF y grows up, so {@code (x, y)} is the
     * bottom-left of the box. Callers clamp each radius to half the smaller
     * side first. Shared by block fill/stroke and clip masking so both emit an
     * identical path.
     */
    static void roundedRectPath(PDPageContentStream stream,
                                float x,
                                float y,
                                float width,
                                float height,
                                float topLeft,
                                float topRight,
                                float bottomRight,
                                float bottomLeft) throws IOException {
        float right = x + width;
        float top = y + height;
        stream.moveTo(x + topLeft, top);
        stream.lineTo(right - topRight, top);
        if (topRight > 0f) {
            float control = topRight * BEZIER_CIRCLE_CONSTANT;
            stream.curveTo(right - topRight + control, top,
                    right, top - topRight + control,
                    right, top - topRight);
        } else {
            stream.lineTo(right, top);
        }
        stream.lineTo(right, y + bottomRight);
        if (bottomRight > 0f) {
            float control = bottomRight * BEZIER_CIRCLE_CONSTANT;
            stream.curveTo(right, y + bottomRight - control,
                    right - bottomRight + control, y,
                    right - bottomRight, y);
        } else {
            stream.lineTo(right, y);
        }
        stream.lineTo(x + bottomLeft, y);
        if (bottomLeft > 0f) {
            float control = bottomLeft * BEZIER_CIRCLE_CONSTANT;
            stream.curveTo(x + bottomLeft - control, y,
                    x, y + bottomLeft - control,
                    x, y + bottomLeft);
        } else {
            stream.lineTo(x, y);
        }
        stream.lineTo(x, top - topLeft);
        if (topLeft > 0f) {
            float control = topLeft * BEZIER_CIRCLE_CONSTANT;
            stream.curveTo(x, top - topLeft + control,
                    x + topLeft - control, top,
                    x + topLeft, top);
        } else {
            stream.lineTo(x, top);
        }
        stream.closePath();
    }

    /**
     * Applies a dash pattern to the stream's stroking state. No-op for
     * {@code null} or solid patterns. Shared by the line and path renderers
     * so both emit identical dash arrays.
     */
    static void applyDashPattern(PDPageContentStream stream, DocumentDashPattern dash) throws IOException {
        if (dash == null || dash.isSolid()) {
            return;
        }
        List<Double> segments = dash.segments();
        float[] dashArray = new float[segments.size()];
        for (int i = 0; i < dashArray.length; i++) {
            dashArray[i] = segments.get(i).floatValue();
        }
        stream.setLineDashPattern(dashArray, 0f);
    }

    /**
     * A path contribution: the caller adds the geometry (ellipse, rectangle,
     * polygon, …) so the fill/stroke wrapper can be shared.
     */
    @FunctionalInterface
    interface PathEmitter {
        void emit(PDPageContentStream stream) throws IOException;
    }
}
