package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.document.style.DocumentDashPattern;
import com.demcha.compose.document.style.DocumentLineCap;
import com.demcha.compose.engine.components.content.shape.Stroke;
import org.apache.poi.sl.usermodel.StrokeStyle.LineCap;
import org.apache.poi.sl.usermodel.StrokeStyle.LineDash;
import org.apache.poi.xslf.usermodel.XSLFSimpleShape;

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
        shape.setLineColor(stroke.strokeColor().color());
        shape.setLineWidth(stroke.width());
    }

    static boolean drawable(Stroke stroke) {
        return stroke != null
                && stroke.strokeColor() != null
                && stroke.strokeColor().color() != null
                && stroke.width() > 0;
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
