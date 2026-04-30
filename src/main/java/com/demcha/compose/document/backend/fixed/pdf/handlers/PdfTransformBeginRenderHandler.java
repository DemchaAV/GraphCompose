package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.backend.fixed.pdf.PdfFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.PdfRenderEnvironment;
import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.style.DocumentTransform;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.util.Matrix;

import java.io.IOException;

/**
 * Opens a graphics-state transform region for a {@code ShapeContainerNode}'s
 * outline + layers. Paired with {@link PdfTransformEndRenderHandler}: every
 * begin fragment must be followed by an end fragment with the same owner
 * path on the same page so the saved graphics state stays balanced.
 *
 * <p>The handler issues
 * {@code saveGraphicsState() -> cm(matrix-rotating-and-scaling-about-the-placement-centre)}.
 * Subsequent fragment handlers (the outline draw, optional clip-path
 * marker, every layer fragment, the matching clip-end) draw on a page
 * surface where the CTM already rotates and scales around the
 * container's centre, so the whole composite transforms as one unit.</p>
 *
 * <p>Conventions:</p>
 * <ul>
 *   <li>{@link DocumentTransform#rotationDegrees()} is interpreted as
 *       <em>clockwise</em> (matches the engine convention). PDF native
 *       rotation is counter-clockwise, so the handler negates the angle
 *       when building the {@code cm} matrix.</li>
 *   <li>Rotation centre is {@code (fragment.x + width/2,
 *       fragment.y + height/2)} — the geometric centre of the outline
 *       in PDF page coordinates (y grows up).</li>
 *   <li>An identity transform never reaches this handler because
 *       {@code ShapeContainerDefinition} skips emitting both begin and
 *       end markers when {@code transform.isIdentity()} is {@code true}.</li>
 * </ul>
 *
 * @author Artem Demchyshyn
 */
public final class PdfTransformBeginRenderHandler
        implements PdfFragmentRenderHandler<BuiltInNodeDefinitions.TransformBeginPayload> {

    /**
     * Creates the transform-begin handler.
     */
    public PdfTransformBeginRenderHandler() {
    }

    @Override
    public Class<BuiltInNodeDefinitions.TransformBeginPayload> payloadType() {
        return BuiltInNodeDefinitions.TransformBeginPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       BuiltInNodeDefinitions.TransformBeginPayload payload,
                       PdfRenderEnvironment environment) throws IOException {
        DocumentTransform transform = payload.transform();
        // Defensive: layout layer already skips emitting begin/end for
        // identity transforms. If a future producer slips through, treat
        // identity as a no-op so the matched end still has a state to pop.
        PDPageContentStream stream = environment.pageSurface(fragment.pageIndex());
        stream.saveGraphicsState();
        if (transform.isIdentity()) {
            return;
        }

        double centerX = fragment.x() + fragment.width() / 2.0;
        double centerY = fragment.y() + fragment.height() / 2.0;
        // Canonical convention: positive rotationDegrees is CLOCKWISE.
        // PDF native cm rotation is CCW (mathematical), so negate.
        double thetaRad = Math.toRadians(-transform.rotationDegrees());
        double cos = Math.cos(thetaRad);
        double sin = Math.sin(thetaRad);
        double sx = transform.scaleX();
        double sy = transform.scaleY();

        // Affine: T(cx,cy) * R(theta) * S(sx,sy) * T(-cx,-cy).
        // PDF cm operator order: a b c d e f where the matrix is
        //   [a c e]
        //   [b d f]
        //   [0 0 1]
        float a = (float) (sx * cos);
        float b = (float) (sx * sin);
        float c = (float) (-sy * sin);
        float d = (float) (sy * cos);
        float e = (float) (centerX - sx * centerX * cos + sy * centerY * sin);
        float f = (float) (centerY - sx * centerX * sin - sy * centerY * cos);

        stream.transform(new Matrix(a, b, c, d, e, f));
    }
}
