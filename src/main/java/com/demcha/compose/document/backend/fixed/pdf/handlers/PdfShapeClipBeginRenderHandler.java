package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ShapeClipBeginPayload;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentCornerRadius;
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
 * (ellipse for circle/ellipse, uniform or per-corner rounded rectangle for
 * rounded-rect, polygon for diamonds / arrows / stars).</p>
 *
 * @author Artem Demchyshyn
 */
public final class PdfShapeClipBeginRenderHandler
        implements PdfFragmentRenderHandler<ShapeClipBeginPayload> {
    private static final float BEZIER_CIRCLE_CONSTANT = 0.552284749831f;

    /**
     * Creates the clip-begin handler.
     */
    public PdfShapeClipBeginRenderHandler() {
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

    @Override
    public Class<ShapeClipBeginPayload> payloadType() {
        return ShapeClipBeginPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       ShapeClipBeginPayload payload,
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
            /*
             * This is code that the java 21 switch pattern matching was designed to avoid.
             */
            if (outline instanceof ShapeOutline.Ellipse) {
                addEllipsePath(stream, x, y, width, height);
            } else if (outline instanceof ShapeOutline.RoundedRectangle r) {
                float radius = (float) Math.min(r.cornerRadius(), Math.min(width, height) / 2.0f);
                PdfShapeGeometry.roundedRectPath(stream, x, y, width, height,
                        radius, radius, radius, radius);
            } else if (outline instanceof ShapeOutline.RoundedRectanglePerCorner rp) {
                float maxRadius = Math.min(width, height) / 2.0f;
                DocumentCornerRadius c = rp.corners();
                PdfShapeGeometry.roundedRectPath(stream, x, y, width, height,
                        (float) Math.min(c.topLeft(), maxRadius),
                        (float) Math.min(c.topRight(), maxRadius),
                        (float) Math.min(c.bottomRight(), maxRadius),
                        (float) Math.min(c.bottomLeft(), maxRadius));
            } else if (outline instanceof ShapeOutline.Rectangle) {
                stream.addRect(x, y, width, height);
            } else if (outline instanceof ShapeOutline.Polygon p) {
                PdfShapeGeometry.addPolygonPath(stream, x, y, width, height, p.points());
            } else {
                throw new IllegalStateException("Unknown outline: " + outline);
            }
        }
        // clip() pushes the current path onto the clipping path; the path
        // itself is not painted (we follow up immediately with stroke/fill
        // ops on subsequent fragments, so we use the no-op path-painter).
        stream.clip();
    }
}
