package com.demcha.compose.document.chart;

/**
 * Pure, deterministic axis-scale computation: given a raw data range and a
 * target tick count, produce "nice" rounded bounds and a tick step using the
 * classic 1 / 2 / 5 / 10 progression. No floating-point surprises that depend on
 * machine or locale — this is the keystone of chart determinism, and it is
 * unit-tested in isolation with golden tables before any rendering is involved.
 *
 * @param niceMin   rounded lower bound
 * @param niceMax   rounded upper bound
 * @param tickStep  distance between successive ticks
 * @param tickCount number of tick marks ({@code niceMin .. niceMax} inclusive)
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public record NiceScale(double niceMin, double niceMax, double tickStep, int tickCount) {

    /**
     * Computes a nice scale.
     *
     * @param dataMin     smallest value to cover
     * @param dataMax     largest value to cover
     * @param includeZero pull the range to include zero (bars)
     * @param targetTicks desired number of intervals (e.g. 5); a hint, not exact
     * @return rounded bounds and step
     */
    public static NiceScale compute(double dataMin, double dataMax,
                                    boolean includeZero, int targetTicks) {
        if (targetTicks < 1) {
            throw new IllegalArgumentException("targetTicks must be >= 1: " + targetTicks);
        }
        if (includeZero) {
            dataMin = Math.min(dataMin, 0.0);
            dataMax = Math.max(dataMax, 0.0);
        }
        if (dataMin == dataMax) {
            // Degenerate range: pad upward so a flat series still plots.
            dataMax = dataMin + 1.0;
        }
        double range = niceNum(dataMax - dataMin, false);
        double step = niceNum(range / (targetTicks - 1 == 0 ? 1 : (targetTicks - 1)), true);
        double niceMin = Math.floor(dataMin / step) * step;
        double niceMax = Math.ceil(dataMax / step) * step;
        int ticks = (int) Math.round((niceMax - niceMin) / step) + 1;
        return new NiceScale(niceMin, niceMax, step, ticks);
    }

    /**
     * Rounds a positive number to a "nice" 1/2/5×10ⁿ value.
     *
     * @param value positive magnitude to round
     * @param round true = round to nearest nice number; false = round up
     * @return nice value
     */
    private static double niceNum(double value, boolean round) {
        double exp = Math.floor(Math.log10(value));
        double fraction = value / Math.pow(10, exp);
        double niceFraction;
        if (round) {
            if (fraction < 1.5) {
                niceFraction = 1;
            } else if (fraction < 3) {
                niceFraction = 2;
            } else if (fraction < 7) {
                niceFraction = 5;
            } else {
                niceFraction = 10;
            }
        } else {
            if (fraction <= 1) {
                niceFraction = 1;
            } else if (fraction <= 2) {
                niceFraction = 2;
            } else if (fraction <= 5) {
                niceFraction = 5;
            } else {
                niceFraction = 10;
            }
        }
        return niceFraction * Math.pow(10, exp);
    }

    /**
     * Maps a data value to a fraction in [0,1] across the nice range.
     *
     * @param value data value
     * @return position fraction along the axis
     */
    public double fractionOf(double value) {
        return (value - niceMin) / (niceMax - niceMin);
    }
}
