package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.layout_core.components.style.Padding;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.awt.Color;
import java.io.IOException;

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

    static void draw(PlacedFragment fragment, Object payload, PdfRenderEnvironment environment) throws IOException {
        PDPageContentStream stream = environment.pageSurface(fragment.pageIndex());
        stream.saveGraphicsState();
        try {
            drawBox(stream, fragment);
            drawMargin(stream, fragment);
            drawPadding(stream, fragment, guidePadding(fragment, payload));
        } finally {
            stream.restoreGraphicsState();
        }
    }

    private static void drawBox(PDPageContentStream stream, PlacedFragment fragment) throws IOException {
        stream.setLineDashPattern(new float[0], 0);
        stream.setStrokingColor(BOX_COLOR);
        stream.setLineWidth(BOX_WIDTH);
        stream.addRect((float) fragment.x(), (float) fragment.y(), (float) fragment.width(), (float) fragment.height());
        stream.stroke();
    }

    private static void drawMargin(PDPageContentStream stream, PlacedFragment fragment) throws IOException {
        if (fragment.margin() == null
                || (fragment.margin().horizontal() <= 0.0 && fragment.margin().vertical() <= 0.0)) {
            return;
        }

        double x = fragment.x() - fragment.margin().left();
        double y = fragment.y() - fragment.margin().bottom();
        double width = fragment.width() + fragment.margin().horizontal();
        double height = fragment.height() + fragment.margin().vertical();
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
                                    PlacedFragment fragment,
                                    Padding padding) throws IOException {
        if (padding == null
                || (padding.horizontal() <= 0.0 && padding.vertical() <= 0.0)) {
            return;
        }

        double x = fragment.x() + padding.left();
        double y = fragment.y() + padding.bottom();
        double width = fragment.width() - padding.horizontal();
        double height = fragment.height() - padding.vertical();
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
