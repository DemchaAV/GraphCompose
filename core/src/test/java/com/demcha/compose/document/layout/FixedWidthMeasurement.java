package com.demcha.compose.document.layout;

import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;

/**
 * Measurement where each character is exactly {@code unit} wide, so a token's width
 * equals its length.
 *
 * <p>Lets the wrapping tests assert on structure and order without a font: the numbers
 * are whatever the test says they are, and a change in font metrics can never move them.</p>
 */
record FixedWidthMeasurement(double unit) implements TextMeasurementSystem {

    @Override
    public ContentSize measure(TextStyle style, String text) {
        return new ContentSize(text.length() * unit, unit);
    }

    @Override
    public LineMetrics lineMetrics(TextStyle style) {
        return new LineMetrics(unit, 0.0, 0.0);
    }
}
