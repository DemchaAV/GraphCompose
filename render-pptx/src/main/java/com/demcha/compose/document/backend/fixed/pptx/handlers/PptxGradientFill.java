package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.style.DocumentPaint;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGradientFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGradientStop;
import org.openxmlformats.schemas.drawingml.x2006.main.CTLineProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTRelativeRect;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSRgbColor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.STPathShadeType;
import org.openxmlformats.schemas.presentationml.x2006.main.CTConnector;
import org.openxmlformats.schemas.presentationml.x2006.main.CTShape;

import java.awt.Color;
import java.util.List;

/**
 * Stamps DrawingML gradient fills and gradient strokes from the engine's
 * {@link DocumentPaint} values.
 *
 * <p>Linear gradients are native ({@code lin} with the angle converted from
 * the engine's counter-clockwise 0&nbsp;=&nbsp;left→right convention to
 * DrawingML's clockwise degrees). An explicit {@code LinearAxis} keeps its
 * direction but DrawingML cannot place exact endpoints, so the axis extent is
 * approximate. Radial gradients map to the {@code circle} path shade — the
 * closest DrawingML has to PDF's radius-to-farthest-corner shading — with the
 * centre expressed through the fill-to rectangle. Both approximations warn
 * once per JVM.</p>
 */
final class PptxGradientFill {

    private PptxGradientFill() {
    }

    static void applyGradientFill(XSLFShape shape, DocumentPaint paint) {
        CTShapeProperties properties = shapeProperties(shape);
        if (properties == null) {
            return;
        }
        if (properties.isSetSolidFill()) {
            properties.unsetSolidFill();
        }
        if (properties.isSetNoFill()) {
            properties.unsetNoFill();
        }
        gradient(properties.isSetGradFill() ? clearGradient(properties) : properties.addNewGradFill(), paint);
    }

    /**
     * Applies a gradient stroking colour — DrawingML lines accept
     * {@code gradFill} natively, unlike PDF's pattern workaround.
     */
    static void applyGradientStroke(XSLFShape shape, DocumentPaint paint, double width) {
        CTShapeProperties properties = shapeProperties(shape);
        if (properties == null) {
            return;
        }
        CTLineProperties line = properties.isSetLn() ? properties.getLn() : properties.addNewLn();
        line.setW((int) Math.round(width * 12700.0));
        if (line.isSetSolidFill()) {
            line.unsetSolidFill();
        }
        // POI's setLineColor(null) writes an explicit noFill; leaving it next
        // to gradFill makes PowerPoint honour noFill and drop the stroke.
        if (line.isSetNoFill()) {
            line.unsetNoFill();
        }
        if (line.isSetPattFill()) {
            line.unsetPattFill();
        }
        gradient(line.isSetGradFill() ? line.getGradFill() : line.addNewGradFill(), paint);
    }

    private static CTGradientFillProperties clearGradient(CTShapeProperties properties) {
        properties.unsetGradFill();
        return properties.addNewGradFill();
    }

    private static void gradient(CTGradientFillProperties fill, DocumentPaint paint) {
        if (paint instanceof DocumentPaint.Solid solid) {
            // Contractually unreachable (solid paints normalize to fillColor);
            // guard so a slipped-through Solid still paints something.
            stops(fill, List.of(new DocumentPaint.Stop(0.0, solid.primaryColor()),
                    new DocumentPaint.Stop(1.0, solid.primaryColor())));
            fill.addNewLin().setAng(0);
        } else if (paint instanceof DocumentPaint.Linear linear) {
            stops(fill, linear.stops());
            fill.addNewLin().setAng(drawingMlAngle(linear.angleDegrees()));
        } else if (paint instanceof DocumentPaint.LinearAxis axis) {
            PptxCapabilityNotes.gradientGeometryApproximated();
            stops(fill, axis.stops());
            double degrees = Math.toDegrees(Math.atan2(axis.y1() - axis.y0(), axis.x1() - axis.x0()));
            fill.addNewLin().setAng(drawingMlAngle(degrees));
        } else if (paint instanceof DocumentPaint.Radial radial) {
            PptxCapabilityNotes.gradientGeometryApproximated();
            stops(fill, radial.stops());
            radialPath(fill, radial.cx(), radial.cy());
        } else if (paint instanceof DocumentPaint.RadialCircle circle) {
            PptxCapabilityNotes.gradientGeometryApproximated();
            stops(fill, circle.stops());
            radialPath(fill, circle.cx(), circle.cy());
        }
    }

    private static void radialPath(CTGradientFillProperties fill, double cx, double cy) {
        // fillToRect pins the gradient origin: all four edges collapse onto the
        // centre point, expressed in thousandths of a percent of the box. The
        // engine's normalized cy is y-up, DrawingML's is top-down.
        CTRelativeRect fillTo = fill.addNewPath().addNewFillToRect();
        int left = (int) Math.round(cx * 100000.0);
        int top = (int) Math.round((1.0 - cy) * 100000.0);
        fillTo.setL(left);
        fillTo.setT(top);
        fillTo.setR(100000 - left);
        fillTo.setB(100000 - top);
        fill.getPath().setPath(STPathShadeType.CIRCLE);
    }

    private static void stops(CTGradientFillProperties fill, List<DocumentPaint.Stop> stops) {
        var stopList = fill.isSetGsLst() ? fill.getGsLst() : fill.addNewGsLst();
        for (int index = 0; index < stops.size(); index++) {
            DocumentPaint.Stop stop = stops.get(index);
            CTGradientStop gradientStop = stopList.addNewGs();
            // The PDF backend's shading functions span the full domain — the
            // first and last stop offsets are effectively 0 and 1, only the
            // interior offsets survive as bounds. Pin the boundary stops the
            // same way so a partial-offset ramp shades identically in both
            // formats.
            double offset = index == 0 ? 0.0
                    : index == stops.size() - 1 ? 1.0
                    : stop.offset();
            gradientStop.setPos((int) Math.round(offset * 100000.0));
            // Stop colors are opaque by contract (DocumentPaint.Stop rejects
            // alpha), so only the RGB channels travel.
            Color color = stop.color().color();
            CTSRgbColor rgb = gradientStop.addNewSrgbClr();
            rgb.setVal(new byte[]{
                    (byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue()});
        }
    }

    /**
     * Engine angles are counter-clockwise with 0 = left→right; DrawingML
     * angles are clockwise sixtieths of a degree from the same axis.
     */
    private static int drawingMlAngle(double engineDegrees) {
        double clockwise = ((360.0 - engineDegrees) % 360.0 + 360.0) % 360.0;
        return (int) Math.round(clockwise * 60000.0);
    }

    private static CTShapeProperties shapeProperties(XSLFShape shape) {
        if (shape.getXmlObject() instanceof CTShape ctShape) {
            return ctShape.getSpPr();
        }
        if (shape.getXmlObject() instanceof CTConnector ctConnector) {
            return ctConnector.getSpPr();
        }
        return null;
    }
}
