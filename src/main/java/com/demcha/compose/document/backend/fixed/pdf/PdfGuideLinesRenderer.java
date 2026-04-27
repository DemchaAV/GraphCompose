package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.engine.components.style.Padding;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal guide renderer for the canonical semantic PDF backend.
 */
final class PdfGuideLinesRenderer {
    private static final Color BOX_COLOR = new Color(150, 150, 150);
    private static final Color MARGIN_COLOR = new Color(0, 110, 255);
    private static final Color PADDING_COLOR = new Color(255, 140, 0);
    private static final float BOX_WIDTH = 1.0f;
    private static final float GUIDE_WIDTH = 0.5f;
    private static final float MARKER_RADIUS = 3.5f;
    private static final float[] DASH_PATTERN = new float[]{3f, 2f};

    private PdfGuideLinesRenderer() {
    }

    /**
     * Builds the union bounds (per page) for each owner path so the guide
     * renderer can paint margin and padding once around the entire owner
     * instead of stacking dashed rectangles inside every sub-fragment.
     *
     * @param fragments resolved fragments for the rendered document
     * @return path → page → bounds map; never {@code null}
     */
    static Map<String, Map<Integer, Bounds>> computeOwnerBounds(List<PlacedFragment> fragments) {
        Map<String, Map<Integer, Bounds>> result = new HashMap<>();
        for (PlacedFragment fragment : fragments) {
            String path = fragment.path();
            if (path == null) {
                continue;
            }
            Map<Integer, Bounds> byPage = result.computeIfAbsent(path, k -> new HashMap<>());
            byPage.merge(fragment.pageIndex(), Bounds.from(fragment), Bounds::union);
        }
        return result;
    }

    static void draw(PlacedFragment fragment,
                     Object payload,
                     PdfRenderEnvironment environment,
                     Map<String, Map<Integer, Bounds>> ownerBoundsByPath) throws IOException {
        PDPageContentStream stream = environment.pageSurface(fragment.pageIndex());
        stream.saveGraphicsState();
        try {
            drawBox(stream, fragment);
            // Margin and padding belong to the owning semantic node, not to
            // each sub-fragment. Table rows inherit the outer table's
            // margin/padding through PlacedFragment; rendering them per-row
            // produces stacked dashed rectangles that visually cut through
            // row text. We render those guides once per owner path on the
            // first sub-fragment, using the union bounds of all sibling
            // sub-fragments so the rectangle wraps the whole table.
            if (fragment.fragmentIndex() == 0) {
                Bounds ownerBounds = lookupOwnerBounds(fragment, ownerBoundsByPath);
                drawMargin(stream, ownerBounds, fragment.margin());
                drawPadding(stream, ownerBounds, guidePadding(fragment, payload));
            }
        } finally {
            stream.restoreGraphicsState();
        }
    }

    private static Bounds lookupOwnerBounds(PlacedFragment fragment,
                                            Map<String, Map<Integer, Bounds>> ownerBoundsByPath) {
        if (ownerBoundsByPath == null || fragment.path() == null) {
            return Bounds.from(fragment);
        }
        Map<Integer, Bounds> byPage = ownerBoundsByPath.get(fragment.path());
        if (byPage == null) {
            return Bounds.from(fragment);
        }
        Bounds bounds = byPage.get(fragment.pageIndex());
        return bounds == null ? Bounds.from(fragment) : bounds;
    }

    /**
     * Lightweight axis-aligned bounding box used to compute owner unions.
     */
    record Bounds(double x, double y, double width, double height) {
        static Bounds from(PlacedFragment fragment) {
            return new Bounds(fragment.x(), fragment.y(), fragment.width(), fragment.height());
        }

        Bounds union(Bounds other) {
            double minX = Math.min(this.x, other.x);
            double minY = Math.min(this.y, other.y);
            double maxX = Math.max(this.x + this.width, other.x + other.width);
            double maxY = Math.max(this.y + this.height, other.y + other.height);
            return new Bounds(minX, minY, maxX - minX, maxY - minY);
        }
    }

    private static void drawBox(PDPageContentStream stream, PlacedFragment fragment) throws IOException {
        stream.setLineDashPattern(new float[0], 0);
        stream.setStrokingColor(BOX_COLOR);
        stream.setLineWidth(BOX_WIDTH);
        stream.addRect((float) fragment.x(), (float) fragment.y(), (float) fragment.width(), (float) fragment.height());
        stream.stroke();
    }

    private static void drawMargin(PDPageContentStream stream,
                                   Bounds bounds,
                                   com.demcha.compose.engine.components.style.Margin margin) throws IOException {
        if (bounds == null
                || margin == null
                || (margin.horizontal() <= 0.0 && margin.vertical() <= 0.0)) {
            return;
        }

        double x = bounds.x() - margin.left();
        double y = bounds.y() - margin.bottom();
        double width = bounds.width() + margin.horizontal();
        double height = bounds.height() + margin.vertical();
        if (width <= 0.0 || height <= 0.0) {
            return;
        }

        stream.setLineDashPattern(DASH_PATTERN, 0);
        stream.setStrokingColor(MARGIN_COLOR);
        stream.setLineWidth(GUIDE_WIDTH);
        stream.addRect((float) x, (float) y, (float) width, (float) height);
        stream.stroke();
        drawCornerMarkers(stream, x, y, width, height, MARGIN_COLOR);
    }

    private static void drawPadding(PDPageContentStream stream,
                                    Bounds bounds,
                                    Padding padding) throws IOException {
        if (bounds == null
                || padding == null
                || (padding.horizontal() <= 0.0 && padding.vertical() <= 0.0)) {
            return;
        }

        double x = bounds.x() + padding.left();
        double y = bounds.y() + padding.bottom();
        double width = bounds.width() - padding.horizontal();
        double height = bounds.height() - padding.vertical();
        if (width <= 0.0 || height <= 0.0) {
            return;
        }

        stream.setLineDashPattern(DASH_PATTERN, 0);
        stream.setStrokingColor(PADDING_COLOR);
        stream.setLineWidth(GUIDE_WIDTH);
        stream.addRect((float) x, (float) y, (float) width, (float) height);
        stream.stroke();
        drawCornerMarkers(stream, x, y, width, height, PADDING_COLOR);
    }

    private static Padding guidePadding(PlacedFragment fragment, Object payload) {
        if (payload instanceof BuiltInNodeDefinitions.ParagraphFragmentPayload paragraphPayload) {
            return paragraphPayload.padding();
        }
        return fragment.padding();
    }

    private static void drawCornerMarkers(PDPageContentStream stream,
                                          double x,
                                          double y,
                                          double width,
                                          double height,
                                          Color color) throws IOException {
        fillCircle(stream, x, y, MARKER_RADIUS, color);
        fillCircle(stream, x + width, y, MARKER_RADIUS, color);
        fillCircle(stream, x, y + height, MARKER_RADIUS, color);
        fillCircle(stream, x + width, y + height, MARKER_RADIUS, color);
    }

    private static void fillCircle(PDPageContentStream stream,
                                   double centerX,
                                   double centerY,
                                   double radius,
                                   Color color) throws IOException {
        double control = radius * 0.552284749831d;
        stream.setLineDashPattern(new float[0], 0);
        stream.setNonStrokingColor(color);
        stream.moveTo((float) (centerX + radius), (float) centerY);
        stream.curveTo(
                (float) (centerX + radius), (float) (centerY + control),
                (float) (centerX + control), (float) (centerY + radius),
                (float) centerX, (float) (centerY + radius));
        stream.curveTo(
                (float) (centerX - control), (float) (centerY + radius),
                (float) (centerX - radius), (float) (centerY + control),
                (float) (centerX - radius), (float) centerY);
        stream.curveTo(
                (float) (centerX - radius), (float) (centerY - control),
                (float) (centerX - control), (float) (centerY - radius),
                (float) centerX, (float) (centerY - radius));
        stream.curveTo(
                (float) (centerX + control), (float) (centerY - radius),
                (float) (centerX + radius), (float) (centerY - control),
                (float) (centerX + radius), (float) centerY);
        stream.fill();
    }
}
