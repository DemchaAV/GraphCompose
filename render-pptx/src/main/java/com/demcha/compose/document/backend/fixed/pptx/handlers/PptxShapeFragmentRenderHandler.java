package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.backend.fixed.pptx.PptxCoordinates;
import com.demcha.compose.document.backend.fixed.pptx.PptxFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.PptxRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ShapeFragmentPayload;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.engine.components.content.shape.Stroke;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFConnectorShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGeomGuide;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGeomGuideList;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPresetGeometry2D;
import org.openxmlformats.schemas.presentationml.x2006.main.CTShape;

import java.awt.geom.Rectangle2D;

/**
 * Renders fixed rectangle-like shape fragments as native PPTX shapes.
 *
 * <p>Sharp and uniformly-rounded rectangles use the {@code rect} /
 * {@code roundRect} presets (the roundRect adjust value expresses the radius
 * as a fraction of the smaller side, matching the PDF handler's clamp to half
 * the smaller side). Distinct per-corner radii cannot be expressed by the
 * single-adjust preset and render with the top-left radius on all corners
 * until the custom-geometry path support lands. Per-side borders draw as four
 * separate line shapes, exactly like the PDF handler draws four separate
 * strokes. Gradient fills are not implemented yet and fall back to the
 * gradient's primary color via {@link ShapeFragmentPayload}'s documented
 * fallback.</p>
 *
 * @since 2.1.0
 */
public final class PptxShapeFragmentRenderHandler
        implements PptxFragmentRenderHandler<ShapeFragmentPayload> {

    /**
     * Creates the shape fragment renderer.
     */
    public PptxShapeFragmentRenderHandler() {
    }

    @Override
    public Class<ShapeFragmentPayload> payloadType() {
        return ShapeFragmentPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       ShapeFragmentPayload payload,
                       PptxRenderEnvironment environment) {
        if (fragment.width() <= 0 || fragment.height() <= 0) {
            return;
        }

        boolean hasFill = payload.fillColor() != null || payload.fillPaint() != null;
        boolean hasStroke = PptxShapeStyle.drawable(payload.stroke());
        boolean hasSideBorders = payload.sideBorders() != null && payload.sideBorders().hasAny();
        if (!hasFill && !hasStroke && !hasSideBorders) {
            return;
        }

        XSLFSlide slide = environment.slide(fragment.pageIndex());
        Rectangle2D.Double anchor = PptxCoordinates.anchorOf(environment.canvasHeight(), fragment);

        if (hasFill || (hasStroke && !hasSideBorders)) {
            XSLFAutoShape shape = slide.createAutoShape();
            double radius = uniformRadius(payload.cornerRadius(), anchor.width, anchor.height);
            if (radius > 0) {
                shape.setShapeType(ShapeType.ROUND_RECT);
                applyRoundRectAdjust(shape, radius, Math.min(anchor.width, anchor.height));
            } else {
                shape.setShapeType(ShapeType.RECT);
            }
            shape.setAnchor(anchor);
            // Solid paints are pre-normalised to fillColor; a non-null fillPaint is
            // always a gradient, drawn as its primary color until gradient fills land.
            if (payload.fillPaint() != null) {
                PptxCapabilityNotes.gradientApproximated();
            }
            java.awt.Color fill = payload.fillColor() != null
                    ? payload.fillColor()
                    : (payload.fillPaint() != null ? payload.fillPaint().primaryColor().color() : null);
            PptxShapeStyle.applyFill(shape, fill);
            PptxShapeStyle.applyStroke(shape, hasSideBorders ? null : payload.stroke());
        }

        if (hasSideBorders) {
            double top = anchor.y;
            double bottom = anchor.y + anchor.height;
            double left = anchor.x;
            double right = anchor.x + anchor.width;
            drawSideBorder(slide, payload.sideBorders().top(), left, top, right, top);
            drawSideBorder(slide, payload.sideBorders().right(), right, top, right, bottom);
            drawSideBorder(slide, payload.sideBorders().bottom(), left, bottom, right, bottom);
            drawSideBorder(slide, payload.sideBorders().left(), left, top, left, bottom);
        }
    }

    private static double uniformRadius(DocumentCornerRadius radii, double width, double height) {
        double maxRadius = Math.min(width, height) / 2.0;
        double topLeft = clamp(radii.topLeft(), maxRadius);
        double topRight = clamp(radii.topRight(), maxRadius);
        double bottomRight = clamp(radii.bottomRight(), maxRadius);
        double bottomLeft = clamp(radii.bottomLeft(), maxRadius);
        boolean uniform = topLeft == topRight && topRight == bottomRight && bottomRight == bottomLeft;
        if (!uniform) {
            PptxCapabilityNotes.perCornerRadiiApproximated();
        }
        return topLeft;
    }

    private static double clamp(double raw, double maxAllowed) {
        if (raw <= 0.0 || Double.isNaN(raw)) {
            return 0.0;
        }
        return Math.min(raw, maxAllowed);
    }

    /**
     * Stamps the roundRect {@code adj} guide: the radius as a fraction of the
     * smaller side, in DrawingML's 1/100000 units, capped at the preset's
     * half-side maximum of 50000.
     */
    private static void applyRoundRectAdjust(XSLFAutoShape shape, double radius, double smallerSide) {
        long adj = Math.round(radius / smallerSide * 100000.0);
        adj = Math.max(0, Math.min(adj, 50000));
        CTPresetGeometry2D geometry = ((CTShape) shape.getXmlObject()).getSpPr().getPrstGeom();
        CTGeomGuideList adjustValues = geometry.isSetAvLst() ? geometry.getAvLst() : geometry.addNewAvLst();
        CTGeomGuide guide = adjustValues.addNewGd();
        guide.setName("adj");
        guide.setFmla("val " + adj);
    }

    private static void drawSideBorder(XSLFSlide slide,
                                       Stroke side,
                                       double x1, double y1, double x2, double y2) {
        if (!PptxShapeStyle.drawable(side)) {
            return;
        }
        XSLFConnectorShape line = slide.createConnector();
        line.setShapeType(ShapeType.LINE);
        line.setAnchor(new Rectangle2D.Double(
                Math.min(x1, x2),
                Math.min(y1, y2),
                Math.abs(x2 - x1),
                Math.abs(y2 - y1)));
        line.setLineColor(side.strokeColor().color());
        line.setLineWidth(side.width());
    }
}
