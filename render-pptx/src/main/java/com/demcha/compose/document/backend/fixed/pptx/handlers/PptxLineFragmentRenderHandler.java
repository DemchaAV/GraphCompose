package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.backend.fixed.pptx.PptxCoordinates;
import com.demcha.compose.document.backend.fixed.pptx.PptxFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pptx.PptxRenderEnvironment;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.LineFragmentPayload;
import com.demcha.compose.engine.components.content.shape.Stroke;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.xslf.usermodel.XSLFConnectorShape;

import java.awt.geom.Rectangle2D;

/**
 * Renders fixed line fragments as native line connector shapes.
 *
 * <p>The payload carries start/end offsets inside the fragment box in the
 * engine's y-up space; both endpoints flip into slide space and the connector
 * anchors on their bounding box. The {@code line} preset draws from the
 * anchor's top-left to bottom-right, so an ascending segment sets the
 * vertical flip instead of relying on coordinate order.</p>
 *
 * @since 2.1.0
 */
public final class PptxLineFragmentRenderHandler
        implements PptxFragmentRenderHandler<LineFragmentPayload> {

    /**
     * Creates the line fragment renderer.
     */
    public PptxLineFragmentRenderHandler() {
    }

    @Override
    public Class<LineFragmentPayload> payloadType() {
        return LineFragmentPayload.class;
    }

    @Override
    public void render(PlacedFragment fragment,
                       LineFragmentPayload payload,
                       PptxRenderEnvironment environment) {
        Stroke stroke = payload.stroke();
        if (!PptxShapeStyle.drawable(stroke)) {
            return;
        }

        double canvasHeight = environment.canvasHeight();
        double x1 = fragment.x() + payload.startX();
        double y1 = PptxCoordinates.flipPoint(canvasHeight, fragment.y() + payload.startY());
        double x2 = fragment.x() + payload.endX();
        double y2 = PptxCoordinates.flipPoint(canvasHeight, fragment.y() + payload.endY());

        XSLFConnectorShape line = environment.surface(fragment.pageIndex()).createConnector();
        line.setShapeType(ShapeType.LINE);
        line.setAnchor(new Rectangle2D.Double(
                Math.min(x1, x2),
                Math.min(y1, y2),
                Math.abs(x2 - x1),
                Math.abs(y2 - y1)));
        // The preset always draws top-left → bottom-right inside the anchor;
        // flip vertically when the segment ascends left-to-right.
        line.setFlipVertical((x2 - x1) * (y2 - y1) < 0);
        line.setLineColor(stroke.strokeColor().color());
        line.setLineWidth(stroke.width());
        PptxShapeStyle.applyLineCap(line, payload.lineCap());
        PptxShapeStyle.applyDashPattern(line, payload.dashPattern());
    }
}
