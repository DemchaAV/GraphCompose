package com.demcha.compose.document.chart;

import com.demcha.compose.document.style.DocumentTextStyle;

/**
 * Narrow text-measurement seam consumed by {@link ChartLayoutResolver}.
 *
 * <p>The resolver lives in the public {@code document.chart} package and must
 * not reach into the engine measurement system or the package-private style
 * adapters directly. It receives this interface instead, so it stays a pure,
 * deterministic function of its inputs while still sizing labels with real font
 * metrics. The layout-side implementation bridges to the engine
 * {@code TextMeasurementSystem}.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public interface ChartTextMetrics {

    /**
     * Measures the rendered width of {@code text} in the supplied style.
     *
     * @param style text style
     * @param text text to measure
     * @return width in points
     */
    double width(DocumentTextStyle style, String text);

    /**
     * Returns the line height for the supplied style.
     *
     * @param style text style
     * @return line height in points
     */
    double lineHeight(DocumentTextStyle style);

    /**
     * Returns the baseline offset from the bottom of the line box (the font
     * descent) for the supplied style. Used to align graphics — legend swatches,
     * tick marks — to the optical centre of the glyph ink rather than the centre
     * of the line box, which would otherwise sit high for ascender-only text such
     * as digits.
     *
     * @param style text style
     * @return descent in points
     */
    double descent(DocumentTextStyle style);
}
