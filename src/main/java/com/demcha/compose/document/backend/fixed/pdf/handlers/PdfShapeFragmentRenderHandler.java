package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.BuiltInNodeDefinitions.SideBorders;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.engine.components.content.shape.Stroke;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

/**
 * Renders fixed rectangle-like shape fragments.
 *
 * @author Artem Demchyshyn
 */
public final class PdfShapeFragmentRenderHandler
        implements PdfFragmentRenderHandler<BuiltInNodeDefinitions.ShapeFragmentPayload> {
    private static final float BEZIER_CIRCLE_CONSTANT = 0.552284749831f;

    /**
     * Creates the shape fragment renderer.
     */
    public PdfShapeFragmentRenderHandler() {
    }

    @Override
    public Class<BuiltInNodeDefinitions.ShapeFragmentPayload> payloadType() {
        return BuiltInNodeDefinitions.ShapeFragmentPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       BuiltInNodeDefinitions.ShapeFragmentPayload payload,
                       PdfRenderEnvironment environment) throws IOException {
        if (fragment.width() <= 0 || fragment.height() <= 0) {
            return;
        }

        boolean hasFill = payload.fillColor() != null;
        boolean hasStroke = payload.stroke() != null
                && payload.stroke().strokeColor() != null
                && payload.stroke().width() > 0;
        boolean hasSideBorders = payload.sideBorders() != null && payload.sideBorders().hasAny();
        if (!hasFill && !hasStroke && !hasSideBorders) {
            return;
        }

        PDPageContentStream stream = environment.pageSurface(fragment.pageIndex());
        stream.saveGraphicsState();
        try {
            float x = (float) fragment.x();
            float y = (float) fragment.y();
            float width = (float) fragment.width();
            float height = (float) fragment.height();
            // Per-corner radii. Each is independently clamped to half
            // the smaller side so an over-sized radius never overshoots
            // the box.
            DocumentCornerRadius radii = payload.cornerRadius();
            float maxRadius = (float) (Math.min(width, height) / 2.0);
            float topLeft = clampCornerRadius(radii.topLeft(), maxRadius);
            float topRight = clampCornerRadius(radii.topRight(), maxRadius);
            float bottomRight = clampCornerRadius(radii.bottomRight(), maxRadius);
            float bottomLeft = clampCornerRadius(radii.bottomLeft(), maxRadius);
            boolean anyRounded = topLeft > 0f || topRight > 0f || bottomRight > 0f || bottomLeft > 0f;

            if (hasFill) {
                stream.setNonStrokingColor(payload.fillColor());
                if (anyRounded) {
                    drawRoundedRectangle(stream, x, y, width, height,
                            topLeft, topRight, bottomRight, bottomLeft);
                } else {
                    stream.addRect(x, y, width, height);
                }
                stream.fill();
            }

            if (hasSideBorders) {
                // Per-side borders override the uniform rectangle stroke; rounded
                // corners are not combined with mixed-side borders in v1.3.
                drawSideBorder(stream, payload.sideBorders().top(),    x,         y + height, x + width, y + height);
                drawSideBorder(stream, payload.sideBorders().right(),  x + width, y + height, x + width, y);
                drawSideBorder(stream, payload.sideBorders().bottom(), x,         y,          x + width, y);
                drawSideBorder(stream, payload.sideBorders().left(),   x,         y + height, x,         y);
            } else if (hasStroke) {
                stream.setStrokingColor(payload.stroke().strokeColor().color());
                stream.setLineWidth((float) payload.stroke().width());
                if (anyRounded) {
                    drawRoundedRectangle(stream, x, y, width, height,
                            topLeft, topRight, bottomRight, bottomLeft);
                } else {
                    stream.addRect(x, y, width, height);
                }
                stream.stroke();
            }
        } finally {
            stream.restoreGraphicsState();
        }
    }

    private static void drawSideBorder(PDPageContentStream stream,
                                       Stroke side,
                                       float x1, float y1, float x2, float y2) throws IOException {
        if (side == null || side.strokeColor() == null || side.width() <= 0) {
            return;
        }
        stream.saveGraphicsState();
        try {
            stream.setStrokingColor(side.strokeColor().color());
            stream.setLineWidth((float) side.width());
            stream.moveTo(x1, y1);
            stream.lineTo(x2, y2);
            stream.stroke();
        } finally {
            stream.restoreGraphicsState();
        }
    }

    private static float clampCornerRadius(double raw, float maxAllowed) {
        if (raw <= 0.0 || Double.isNaN(raw)) {
            return 0f;
        }
        float capped = (float) raw;
        return capped > maxAllowed ? maxAllowed : capped;
    }

    /**
     * Draws a rectangle outline whose four corners may have independent
     * radii. Each radius gets its own Bezier arc; a zero radius keeps a
     * sharp 90° corner (no arc, just a line into the corner). PDF y
     * grows up, so {@code (x, y)} is the bottom-left of the rectangle.
     */
    private static void drawRoundedRectangle(PDPageContentStream stream,
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
        // Start at the right end of the top edge (one radius left of the
        // top-right corner) and walk clockwise around the box. Each
        // corner emits either a curveTo (for radius > 0) or just lets
        // the next lineTo land on the corner point (for radius == 0).
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
}
