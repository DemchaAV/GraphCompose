package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.style.DocumentDashPattern;
import com.demcha.compose.document.style.DocumentLineCap;
import com.demcha.compose.engine.components.content.shape.Stroke;
import org.apache.poi.sl.usermodel.StrokeStyle.LineCap;
import org.apache.poi.sl.usermodel.StrokeStyle.LineDash;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFSimpleShape;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGeomGuide;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGeomGuideList;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPresetGeometry2D;
import org.openxmlformats.schemas.presentationml.x2006.main.CTShape;

import java.awt.Color;

/**
 * Shared fill/stroke application for the PPTX fragment handlers, mirroring the
 * role {@code PdfShapeGeometry} plays for the PDF handlers: one place mapping
 * the engine's style values onto POI shape properties.
 */
final class PptxShapeStyle {

    private PptxShapeStyle() {
    }

    /**
     * Applies a solid fill; {@code null} writes an explicit {@code noFill}
     * (POI's default fill would otherwise paint theme blue).
     */
    static void applyFill(XSLFSimpleShape shape, Color fillColor) {
        shape.setFillColor(fillColor);
    }

    /**
     * Applies a uniform stroke, or removes the outline when the stroke carries
     * no drawable color/width.
     */
    static void applyStroke(XSLFSimpleShape shape, Stroke stroke) {
        if (!drawable(stroke)) {
            shape.setLineColor(null);
            return;
        }
        // POI's setLineColor writes the color's alpha as an <a:alpha> child
        // in the XML; only its getLineColor getter hides it on read-back.
        shape.setLineColor(stroke.strokeColor().color());
        shape.setLineWidth(stroke.width());
    }

    static boolean drawable(Stroke stroke) {
        return stroke != null
                && stroke.strokeColor() != null
                && stroke.strokeColor().color() != null
                && stroke.width() > 0;
    }

    /**
     * Writes the {@code roundRect} adjustment that represents the requested
     * radius as a fraction of the smaller side. DrawingML clamps that preset at
     * half the smaller side, matching the PDF geometry contract.
     */
    static void applyRoundRectAdjust(XSLFAutoShape shape,
                                     double radius,
                                     double width,
                                     double height) {
        double smallerSide = Math.min(width, height);
        if (smallerSide <= 0) {
            return;
        }
        long adjust = Math.round(Math.min(Math.max(radius, 0), smallerSide / 2.0)
                / smallerSide * 100000.0);
        CTPresetGeometry2D geometry = ((CTShape) shape.getXmlObject()).getSpPr().getPrstGeom();
        CTGeomGuideList adjustValues = geometry.isSetAvLst()
                ? geometry.getAvLst() : geometry.addNewAvLst();
        CTGeomGuide guide = adjustValues.addNewGd();
        guide.setName("adj");
        guide.setFmla("val " + Math.max(0, Math.min(adjust, 50000)));
    }

    static void applyLineCap(XSLFSimpleShape shape, DocumentLineCap cap) {
        if (cap == null) {
            return;
        }
        shape.setLineCap(switch (cap) {
            case BUTT -> LineCap.FLAT;
            case ROUND -> LineCap.ROUND;
            case SQUARE -> LineCap.SQUARE;
        });
    }

    /** Applies a line join through the raw line properties (POI has no setter). */
    static void applyLineJoin(XSLFSimpleShape shape,
                              com.demcha.compose.document.style.DocumentLineJoin join) {
        if (join == null) {
            return;
        }
        org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties properties;
        if (shape.getXmlObject() instanceof org.openxmlformats.schemas.presentationml.x2006.main.CTShape ctShape) {
            properties = ctShape.getSpPr();
        } else if (shape.getXmlObject() instanceof org.openxmlformats.schemas.presentationml.x2006.main.CTConnector ctConnector) {
            properties = ctConnector.getSpPr();
        } else {
            return;
        }
        org.openxmlformats.schemas.drawingml.x2006.main.CTLineProperties line =
                properties.isSetLn() ? properties.getLn() : properties.addNewLn();
        switch (join) {
            case MITER -> line.addNewMiter();
            case ROUND -> line.addNewRound();
            case BEVEL -> line.addNewBevel();
        }
    }

    /**
     * Applies a dash pattern. DrawingML dashes are percent-of-line-width
     * presets rather than point arrays, so a non-solid pattern maps to the
     * generic dashed preset for now; the exact numeric mapping belongs to the
     * custom-geometry work that also brings free paths.
     */
    static void applyDashPattern(XSLFSimpleShape shape, DocumentDashPattern pattern) {
        if (pattern == null || pattern.isSolid()) {
            return;
        }
        PptxCapabilityNotes.numericDashApproximated();
        shape.setLineDash(LineDash.DASH);
    }
}
