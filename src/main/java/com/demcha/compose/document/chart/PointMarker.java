package com.demcha.compose.document.chart;

import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentStroke;

/**
 * Point-marker configuration for line charts: an ellipse drawn at every data
 * point. Markers make line joints and crossings legible and anchor the
 * per-point value labels.
 *
 * <p>The marker is an ellipse with independent axes, so both circular dots and
 * flattened ellipses are expressible. {@code fill} defaults to the series paint
 * from the colour cascade when {@code null}; {@code stroke} draws an outline
 * ring (a white ring over a coloured line is the classic way to keep
 * overlapping joints readable).</p>
 *
 * @param width  marker width (horizontal axis) in points
 * @param height marker height (vertical axis) in points
 * @param fill   explicit marker fill, or {@code null} to use the series paint
 * @param stroke outline ring, or {@code null} for none
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public record PointMarker(double width, double height, DocumentPaint fill, DocumentStroke stroke) {

    /**
     * Validates marker axes.
     */
    public PointMarker {
        requirePositiveFinite(width, "width");
        requirePositiveFinite(height, "height");
    }

    /**
     * Circular marker filled with the series paint.
     *
     * @param diameter circle diameter in points
     * @return marker
     */
    public static PointMarker circle(double diameter) {
        return new PointMarker(diameter, diameter, null, null);
    }

    /**
     * Elliptical marker filled with the series paint.
     *
     * @param width  horizontal axis in points
     * @param height vertical axis in points
     * @return marker
     */
    public static PointMarker ellipse(double width, double height) {
        return new PointMarker(width, height, null, null);
    }

    private static void requirePositiveFinite(double v, String name) {
        if (v <= 0 || Double.isNaN(v) || Double.isInfinite(v)) {
            throw new IllegalArgumentException(name + " must be finite and positive: " + v);
        }
    }

    /**
     * Returns a copy with an explicit fill (overrides the series paint).
     *
     * @param fill marker fill
     * @return updated marker
     */
    public PointMarker withFill(DocumentPaint fill) {
        return new PointMarker(width, height, fill, stroke);
    }

    /**
     * Returns a copy with an outline ring.
     *
     * @param stroke ring stroke
     * @return updated marker
     */
    public PointMarker withStroke(DocumentStroke stroke) {
        return new PointMarker(width, height, fill, stroke);
    }
}
