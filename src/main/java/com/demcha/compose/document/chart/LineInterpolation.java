package com.demcha.compose.document.chart;

/**
 * How a line-chart series connects its data points — the trade-off between a
 * pleasing curve and a faithful reading of the numbers.
 *
 * <p>All three modes pass <em>through</em> every data point and render with the
 * same native PDF operators (straight {@code lineTo} or cubic {@code cubicTo});
 * they differ only in the geometry <em>between</em> points.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public enum LineInterpolation {
    /**
     * Straight segments between consecutive points. Exact by construction — the
     * line is literally the data, nothing is interpolated or embellished — at
     * the cost of angular joints.
     */
    LINEAR,
    /**
     * Smooth Catmull-Rom curve (tension 0.5). The prettiest option, but an
     * interpolating spline may <b>overshoot</b> local extremes between points on
     * sharp value swings, so the curve can briefly leave the range of the data
     * it connects. Choose this when visual flow matters more than a precise
     * reading; choose {@link #MONOTONE} when it does not.
     */
    SMOOTH,
    /**
     * Smooth monotone cubic (Fritsch-Carlson) curve. Looks nearly as smooth as
     * {@link #SMOOTH} but is constrained to <b>never overshoot</b>: the curve
     * stays within the value range of the two points it spans and preserves the
     * data's rises and falls. The accurate-yet-smooth middle ground — smooth
     * presentation without misrepresenting the numbers.
     */
    MONOTONE
}
