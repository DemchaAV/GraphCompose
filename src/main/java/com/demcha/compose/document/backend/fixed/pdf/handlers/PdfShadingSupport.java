package com.demcha.compose.document.backend.fixed.pdf.handlers;

import com.demcha.compose.document.style.DocumentPaint;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBoolean;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.common.function.PDFunction;
import org.apache.pdfbox.pdmodel.common.function.PDFunctionType2;
import org.apache.pdfbox.pdmodel.common.function.PDFunctionType3;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType2;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType3;

import java.awt.Color;
import java.util.List;

/**
 * Builds PDF axial / radial shadings from the backend-neutral
 * {@link DocumentPaint} gradient types.
 *
 * <p>The translation is deterministic: a {@link DocumentPaint.Linear} maps to
 * a {@code /ShadingType 2} whose axis crosses the target box's centre along
 * the paint's angle (0° = left→right, 90° = bottom→top), long enough to cover
 * the whole box; a {@link DocumentPaint.Radial} maps to {@code /ShadingType 3}
 * centred at the paint's normalized centre with a radius reaching the farthest
 * box corner. Two stops become one exponential function; more stops become a
 * stitching function over evenly encoded sub-intervals.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
final class PdfShadingSupport {

    private PdfShadingSupport() {
    }

    /**
     * Builds the shading for a gradient paint over the given box.
     *
     * @param paint gradient paint ({@link DocumentPaint.Linear} or {@link DocumentPaint.Radial})
     * @param x box left, page coordinates
     * @param y box bottom, page coordinates
     * @param width box width
     * @param height box height
     * @return configured shading
     * @throws IllegalArgumentException for a {@link DocumentPaint.Solid} (solid
     *         fills never reach the shading path)
     */
    static PDShading build(DocumentPaint paint, float x, float y, float width, float height) {
        if (paint instanceof DocumentPaint.Linear linear) {
            return axial(linear, x, y, width, height);
        }
        if (paint instanceof DocumentPaint.Radial radial) {
            return radial(radial, x, y, width, height);
        }
        throw new IllegalArgumentException(
                "solid paints are normalised before emission and never reach the shading path");
    }

    private static PDShading axial(DocumentPaint.Linear linear,
                                   float x, float y, float width, float height) {
        double radians = Math.toRadians(linear.angleDegrees());
        double dx = Math.cos(radians);
        double dy = Math.sin(radians);
        double cx = x + width / 2.0;
        double cy = y + height / 2.0;
        // Half the box's extent projected onto the gradient axis, so the axis
        // always spans the box regardless of angle.
        double halfLen = (Math.abs(width * dx) + Math.abs(height * dy)) / 2.0;

        PDShadingType2 shading = new PDShadingType2(new COSDictionary());
        shading.setShadingType(PDShading.SHADING_TYPE2);
        shading.setColorSpace(PDDeviceRGB.INSTANCE);
        COSArray coords = new COSArray();
        coords.add(new COSFloat((float) (cx - dx * halfLen)));
        coords.add(new COSFloat((float) (cy - dy * halfLen)));
        coords.add(new COSFloat((float) (cx + dx * halfLen)));
        coords.add(new COSFloat((float) (cy + dy * halfLen)));
        shading.setCoords(coords);
        shading.setFunction(stopsFunction(linear.stops()));
        shading.setExtend(bothExtend());
        return shading;
    }

    private static PDShading radial(DocumentPaint.Radial radial,
                                    float x, float y, float width, float height) {
        double cx = x + radial.cx() * width;
        double cy = y + radial.cy() * height;
        // Radius to the farthest corner so the last stop always reaches it.
        double r = 0.0;
        for (double[] corner : new double[][] {{x, y}, {x + width, y}, {x, y + height},
                {x + width, y + height}}) {
            r = Math.max(r, Math.hypot(corner[0] - cx, corner[1] - cy));
        }

        PDShadingType3 shading = new PDShadingType3(new COSDictionary());
        shading.setShadingType(PDShading.SHADING_TYPE3);
        shading.setColorSpace(PDDeviceRGB.INSTANCE);
        COSArray coords = new COSArray();
        coords.add(new COSFloat((float) cx));
        coords.add(new COSFloat((float) cy));
        coords.add(new COSFloat(0f));
        coords.add(new COSFloat((float) cx));
        coords.add(new COSFloat((float) cy));
        coords.add(new COSFloat((float) r));
        shading.setCoords(coords);
        shading.setFunction(stopsFunction(radial.stops()));
        shading.setExtend(bothExtend());
        return shading;
    }

    private static COSArray bothExtend() {
        COSArray extend = new COSArray();
        extend.add(COSBoolean.TRUE);
        extend.add(COSBoolean.TRUE);
        return extend;
    }

    /** Two stops → one exponential function; more → a stitching function. */
    private static PDFunction stopsFunction(List<DocumentPaint.Stop> stops) {
        if (stops.size() == 2) {
            return segment(stops.get(0).color().color(), stops.get(1).color().color());
        }
        COSDictionary dict = new COSDictionary();
        dict.setInt(COSName.FUNCTION_TYPE, 3);
        dict.setItem(COSName.DOMAIN, domain01());

        COSArray functions = new COSArray();
        COSArray bounds = new COSArray();
        COSArray encode = new COSArray();
        for (int i = 0; i < stops.size() - 1; i++) {
            functions.add(segment(
                    stops.get(i).color().color(),
                    stops.get(i + 1).color().color()));
            if (i > 0) {
                bounds.add(new COSFloat((float) stops.get(i).offset()));
            }
            encode.add(COSInteger.ZERO);
            encode.add(COSInteger.ONE);
        }
        dict.setItem(COSName.FUNCTIONS, functions);
        dict.setItem(COSName.BOUNDS, bounds);
        dict.setItem(COSName.ENCODE, encode);
        return new PDFunctionType3(dict);
    }

    private static PDFunctionType2 segment(Color from, Color to) {
        COSDictionary dict = new COSDictionary();
        dict.setInt(COSName.FUNCTION_TYPE, 2);
        dict.setItem(COSName.DOMAIN, domain01());
        dict.setItem(COSName.C0, rgb(from));
        dict.setItem(COSName.C1, rgb(to));
        dict.setInt(COSName.N, 1);
        return new PDFunctionType2(dict);
    }

    private static COSArray domain01() {
        COSArray domain = new COSArray();
        domain.add(COSInteger.ZERO);
        domain.add(COSInteger.ONE);
        return domain;
    }

    private static COSArray rgb(Color color) {
        COSArray array = new COSArray();
        array.add(new COSFloat(color.getRed() / 255f));
        array.add(new COSFloat(color.getGreen() / 255f));
        array.add(new COSFloat(color.getBlue() / 255f));
        return array;
    }
}
