package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.ShapeOutline;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

/**
 * Opens a graphics-state clip region for a {@code ShapeContainerNode}'s
 * layers. Paired with {@link PdfShapeClipEndRenderHandler}: every begin
 * fragment must be followed by an end fragment with the same owner path on
 * the same page so the saved graphics state is balanced.
 *
 * <p>The handler issues:
 * {@code saveGraphicsState() -> add outline path -> clip()}. Subsequent
 * fragment handlers draw on a page surface that is already restricted to
 * the outline; the matching end handler issues
 * {@code restoreGraphicsState()} to lift the clip.</p>
 *
 * <p>{@link ClipPolicy#OVERFLOW_VISIBLE} never reaches this handler — the
 * layout layer skips emitting both markers when the policy is visible. For
 * {@link ClipPolicy#CLIP_BOUNDS} the clip path is the axis-aligned outline
 * rectangle; for {@link ClipPolicy#CLIP_PATH} it is the geometric outline
 * (ellipse for circle/ellipse, rounded rectangle for rounded-rect).</p>
 *
 * @author Artem Demchyshyn
 */
public final class PdfShapeClipBeginRenderHandler
        implements PdfFragmentRenderHandler<BuiltInNodeDefinitions.ShapeClipBeginPayload> {
    private static final float BEZIER_CIRCLE_CONSTANT = 0.552284749831f;

    /**
     * Creates the clip-begin handler.
     */
    public PdfShapeClipBeginRenderHandler() {
    }

    @Override
    public Class<BuiltInNodeDefinitions.ShapeClipBeginPayload> payloadType() {
        return BuiltInNodeDefinitions.ShapeClipBeginPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       BuiltInNodeDefinitions.ShapeClipBeginPayload payload,
                       PdfRenderEnvironment environment) throws IOException {
        if (fragment.width() <= 0 || fragment.height() <= 0) {
            return;
        }

        PDPageContentStream stream = environment.pageSurface(fragment.pageIndex());
        // saveGraphicsState here is paired with restoreGraphicsState in
        // the end-marker handler. The pairing invariant is enforced by the
        // architecture-guard test ShapeContainerInvariantsTest.
        stream.saveGraphicsState();

        float x = (float) fragment.x();
        float y = (float) fragment.y();
        float width = (float) fragment.width();
        float height = (float) fragment.height();

        ClipPolicy policy = payload.policy();
        ShapeOutline outline = payload.outline();
        if (policy == ClipPolicy.CLIP_BOUNDS) {
            stream.addRect(x, y, width, height);
        } else { // CLIP_PATH
            switch (outline) {
                case ShapeOutline.Ellipse ignored -> addEllipsePath(stream, x, y, width, height);
                case ShapeOutline.RoundedRectangle r -> addRoundedRectanglePath(
                        stream, x, y, width, height,
                        (float) Math.min(r.cornerRadius(), Math.min(width, height) / 2.0f));
                case ShapeOutline.Rectangle ignored -> stream.addRect(x, y, width, height);
            }
        }
        // clip() pushes the current path onto the clipping path; the path
        // itself is not painted (we follow up immediately with stroke/fill
        // ops on subsequent fragments, so we use the no-op path-painter).
        stream.clip();
    }

    private static void addEllipsePath(PDPageContentStream stream,
                                       float x,
                                       float y,
                                       float width,
                                       float height) throws IOException {
        float centerX = x + width / 2.0f;
        float centerY = y + height / 2.0f;
        float radiusX = width / 2.0f;
        float radiusY = height / 2.0f;
        float controlX = radiusX * BEZIER_CIRCLE_CONSTANT;
        float controlY = radiusY * BEZIER_CIRCLE_CONSTANT;

        stream.moveTo(centerX + radiusX, centerY);
        stream.curveTo(centerX + radiusX, centerY + controlY,
                centerX + controlX, centerY + radiusY,
                centerX, centerY + radiusY);
        stream.curveTo(centerX - controlX, centerY + radiusY,
                centerX - radiusX, centerY + controlY,
                centerX - radiusX, centerY);
        stream.curveTo(centerX - radiusX, centerY - controlY,
                centerX - controlX, centerY - radiusY,
                centerX, centerY - radiusY);
        stream.curveTo(centerX + controlX, centerY - radiusY,
                centerX + radiusX, centerY - controlY,
                centerX + radiusX, centerY);
        stream.closePath();
    }

    private static void addRoundedRectanglePath(PDPageContentStream stream,
                                                float x,
                                                float y,
                                                float width,
                                                float height,
                                                float radius) throws IOException {
        if (radius <= 0f) {
            stream.addRect(x, y, width, height);
            return;
        }
        float control = radius * BEZIER_CIRCLE_CONSTANT;
        stream.moveTo(x + radius, y + height);
        stream.lineTo(x + width - radius, y + height);
        stream.curveTo(x + width - radius + control, y + height,
                x + width, y + height - radius + control,
                x + width, y + height - radius);
        stream.lineTo(x + width, y + radius);
        stream.curveTo(x + width, y + radius - control,
                x + width - radius + control, y,
                x + width - radius, y);
        stream.lineTo(x + radius, y);
        stream.curveTo(x + radius - control, y,
                x, y + radius - control,
                x, y + radius);
        stream.lineTo(x, y + height - radius);
        stream.curveTo(x, y + height - radius + control,
                x + radius - control, y + height,
                x + radius, y + height);
        stream.closePath();
    }
}
