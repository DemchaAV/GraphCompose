package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.backend.fixed.pptx.PptxCoordinates;
import com.demcha.compose.document.backend.fixed.pptx.PptxFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.PptxRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.TransformBeginPayload;
import com.demcha.compose.document.style.DocumentTransform;
import org.apache.poi.xslf.usermodel.XSLFGroupShape;

import java.awt.geom.Rectangle2D;

/**
 * Opens a transform region as a PPTX group shape. PPTX has no graphics-state
 * stack, so the PDF backend's ambient {@code cm} matrix becomes containment:
 * every fragment between the begin and end markers is created inside the
 * group, whose frame then rotates and scales about the fragment centre —
 * PowerPoint rotates a shape about its own centre, and scaling is the ratio
 * between the group's exterior frame and its interior coordinate space, so
 * the composite transforms exactly like the PDF's
 * {@code T(c)·R(θ)·S(sx,sy)·T(−c)} matrix. The engine's clockwise rotation
 * convention matches PowerPoint's, so the angle passes through unchanged.
 *
 * @since 2.1.0
 */
public final class PptxTransformBeginRenderHandler
        implements PptxFragmentRenderHandler<TransformBeginPayload> {

    /**
     * Creates the transform-begin handler.
     */
    public PptxTransformBeginRenderHandler() {
    }

    @Override
    public Class<TransformBeginPayload> payloadType() {
        return TransformBeginPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       TransformBeginPayload payload,
                       PptxRenderEnvironment environment) {
        XSLFGroupShape group = environment.surface(fragment.pageIndex()).createGroup();
        Rectangle2D.Double box = PptxCoordinates.anchorOf(environment.canvasHeight(), fragment);
        // Children keep drawing in absolute slide coordinates: the interior
        // space equals the untransformed fragment box.
        group.setInteriorAnchor(box);
        DocumentTransform transform = payload.transform();
        if (transform.isIdentity()) {
            group.setAnchor(box);
        } else {
            double scaledWidth = box.width * Math.abs(transform.scaleX());
            double scaledHeight = box.height * Math.abs(transform.scaleY());
            group.setAnchor(new Rectangle2D.Double(
                    box.x + (box.width - scaledWidth) / 2.0,
                    box.y + (box.height - scaledHeight) / 2.0,
                    scaledWidth,
                    scaledHeight));
            group.setFlipHorizontal(transform.scaleX() < 0);
            group.setFlipVertical(transform.scaleY() < 0);
            group.setRotation(transform.rotationDegrees());
        }
        environment.pushGroup(group);
    }
}
