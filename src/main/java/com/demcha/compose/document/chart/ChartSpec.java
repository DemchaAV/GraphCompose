package com.demcha.compose.document.chart;

import java.util.Objects;

/**
 * The structural description of a chart — <em>what</em> is plotted and how it is
 * configured, independent of <em>colour</em> ({@link ChartStyle}) and of the raw
 * numbers ({@link ChartData}). A sealed hierarchy: each chart kind is its own
 * record carrying only the knobs that make sense for it, so an unsupported
 * combination (a stacked line, a pie axis) is simply unrepresentable rather than
 * validated away at runtime.
 *
 * <p>The resolver pattern-matches on the permitted subtypes; adding a new chart
 * kind means a new record here plus a new branch in {@code ChartLayoutResolver}
 * — additive, and the compiler enforces exhaustive handling at the switch.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public sealed interface ChartSpec permits ChartSpec.Bar, ChartSpec.Line {

    /**
     * Tabular data backing this chart.
     *
     * @return chart data
     */
    ChartData data();

    /**
     * Numeric-axis configuration.
     *
     * @return value axis spec
     */
    AxisSpec valueAxis();

    /**
     * Legend placement.
     *
     * @return legend position
     */
    LegendPosition legend();

    /**
     * Width-responsive sizing policy.
     *
     * @return chart size policy
     */
    ChartSize size();

    /**
     * Starts a bar-chart builder.
     *
     * @return bar builder
     */
    static Bar.Builder bar() {
        return new Bar.Builder();
    }

    /**
     * Starts a line-chart builder.
     *
     * @return line builder
     */
    static Line.Builder line() {
        return new Line.Builder();
    }

    /**
     * Bar chart: vertical or horizontal, grouped or stacked.
     *
     * @param data tabular data
     * @param horizontal true = bars run horizontally (categories on y)
     * @param grouping side-by-side vs stacked when multiple series
     * @param valueAxis numeric-axis configuration
     * @param legend legend placement
     * @param valueLabels per-bar value label mode
     * @param size width-responsive sizing policy
     * @param showCategoryLabels emit the category (x-axis) label under each slot
     */
    record Bar(
            ChartData data,
            boolean horizontal,
            BarGrouping grouping,
            AxisSpec valueAxis,
            LegendPosition legend,
            ValueLabelMode valueLabels,
            ChartSize size,
            boolean showCategoryLabels
    ) implements ChartSpec {
        /** Normalizes structural defaults. */
        public Bar {
            Objects.requireNonNull(data, "data");
            grouping = grouping == null ? BarGrouping.GROUPED : grouping;
            valueAxis = valueAxis == null ? AxisSpec.defaults() : valueAxis;
            legend = legend == null ? LegendPosition.NONE : legend;
            valueLabels = valueLabels == null ? ValueLabelMode.NONE : valueLabels;
            size = size == null ? ChartSize.aspectRatio(16, 9) : size;
        }

        /**
         * Backward-compatible constructor without the category-label toggle
         * (defaults to showing category labels).
         *
         * @param data tabular data
         * @param horizontal horizontal orientation
         * @param grouping multi-series grouping
         * @param valueAxis numeric-axis configuration
         * @param legend legend placement
         * @param valueLabels per-bar value label mode
         * @param size sizing policy
         */
        public Bar(ChartData data, boolean horizontal, BarGrouping grouping, AxisSpec valueAxis,
                   LegendPosition legend, ValueLabelMode valueLabels, ChartSize size) {
            this(data, horizontal, grouping, valueAxis, legend, valueLabels, size, true);
        }

        /** Fluent builder for a {@link Bar} spec. */
        public static final class Builder {
            private ChartData data;
            private boolean horizontal = false;
            private BarGrouping grouping = BarGrouping.GROUPED;
            private AxisSpec valueAxis = AxisSpec.defaults();
            private LegendPosition legend = LegendPosition.NONE;
            private ValueLabelMode valueLabels = ValueLabelMode.NONE;
            private ChartSize size = ChartSize.aspectRatio(16, 9);
            private boolean showCategoryLabels = true;

            /**
             * Sets the data.
             *
             * @param d chart data
             * @return this builder
             */
            public Builder data(ChartData d) {
                this.data = d;
                return this;
            }

            /**
             * Sets horizontal orientation.
             *
             * @param v true for horizontal bars
             * @return this builder
             */
            public Builder horizontal(boolean v) {
                this.horizontal = v;
                return this;
            }

            /**
             * Toggles the category (x-axis) labels under each slot.
             *
             * @param v true to draw category labels
             * @return this builder
             */
            public Builder showCategoryLabels(boolean v) {
                this.showCategoryLabels = v;
                return this;
            }

            /**
             * Sets multi-series grouping.
             *
             * @param g grouping mode
             * @return this builder
             */
            public Builder grouping(BarGrouping g) {
                this.grouping = g;
                return this;
            }

            /**
             * Sets the value axis.
             *
             * @param a axis spec
             * @return this builder
             */
            public Builder valueAxis(AxisSpec a) {
                this.valueAxis = a;
                return this;
            }

            /**
             * Sets the legend placement.
             *
             * @param l legend position
             * @return this builder
             */
            public Builder legend(LegendPosition l) {
                this.legend = l;
                return this;
            }

            /**
             * Sets the value-label mode.
             *
             * @param m value label mode
             * @return this builder
             */
            public Builder valueLabels(ValueLabelMode m) {
                this.valueLabels = m;
                return this;
            }

            /**
             * Sets the size policy.
             *
             * @param s chart size
             * @return this builder
             */
            public Builder size(ChartSize s) {
                this.size = s;
                return this;
            }

            /**
             * Builds the bar spec.
             *
             * @return bar spec
             */
            public Bar build() {
                return new Bar(data, horizontal, grouping, valueAxis, legend, valueLabels, size,
                        showCategoryLabels);
            }
        }
    }

    /**
     * Line chart: one polyline per series, optional point markers (markers come
     * from {@link ChartStyle#pointMarker()} so the geometry is reused).
     *
     * @param data tabular data
     * @param smooth true = curved (Catmull-Rom) segments; false = straight
     * @param valueAxis numeric-axis configuration
     * @param legend legend placement
     * @param valueLabels per-point value label mode
     * @param size width-responsive sizing policy
     * @param showCategoryLabels emit the category (x-axis) label under each slot
     */
    record Line(
            ChartData data,
            boolean smooth,
            AxisSpec valueAxis,
            LegendPosition legend,
            ValueLabelMode valueLabels,
            ChartSize size,
            boolean showCategoryLabels
    ) implements ChartSpec {
        /** Normalizes structural defaults. */
        public Line {
            Objects.requireNonNull(data, "data");
            valueAxis = valueAxis == null ? AxisSpec.defaults() : valueAxis;
            legend = legend == null ? LegendPosition.NONE : legend;
            valueLabels = valueLabels == null ? ValueLabelMode.NONE : valueLabels;
            size = size == null ? ChartSize.aspectRatio(16, 9) : size;
        }

        /**
         * Backward-compatible constructor without the category-label toggle
         * (defaults to showing category labels).
         *
         * @param data tabular data
         * @param smooth curved segments
         * @param valueAxis numeric-axis configuration
         * @param legend legend placement
         * @param valueLabels per-point value label mode
         * @param size sizing policy
         */
        public Line(ChartData data, boolean smooth, AxisSpec valueAxis, LegendPosition legend,
                    ValueLabelMode valueLabels, ChartSize size) {
            this(data, smooth, valueAxis, legend, valueLabels, size, true);
        }

        /** Fluent builder for a {@link Line} spec. */
        public static final class Builder {
            private ChartData data;
            private boolean smooth = false;
            private AxisSpec valueAxis = AxisSpec.defaults();
            private LegendPosition legend = LegendPosition.NONE;
            private ValueLabelMode valueLabels = ValueLabelMode.NONE;
            private ChartSize size = ChartSize.aspectRatio(16, 9);
            private boolean showCategoryLabels = true;

            /**
             * Sets the data.
             *
             * @param d chart data
             * @return this builder
             */
            public Builder data(ChartData d) {
                this.data = d;
                return this;
            }

            /**
             * Sets smoothing.
             *
             * @param v true for curved segments
             * @return this builder
             */
            public Builder smooth(boolean v) {
                this.smooth = v;
                return this;
            }

            /**
             * Toggles the category (x-axis) labels under each slot.
             *
             * @param v true to draw category labels
             * @return this builder
             */
            public Builder showCategoryLabels(boolean v) {
                this.showCategoryLabels = v;
                return this;
            }

            /**
             * Sets the value axis.
             *
             * @param a axis spec
             * @return this builder
             */
            public Builder valueAxis(AxisSpec a) {
                this.valueAxis = a;
                return this;
            }

            /**
             * Sets the legend placement.
             *
             * @param l legend position
             * @return this builder
             */
            public Builder legend(LegendPosition l) {
                this.legend = l;
                return this;
            }

            /**
             * Sets the value-label mode.
             *
             * @param m value label mode
             * @return this builder
             */
            public Builder valueLabels(ValueLabelMode m) {
                this.valueLabels = m;
                return this;
            }

            /**
             * Sets the size policy.
             *
             * @param s chart size
             * @return this builder
             */
            public Builder size(ChartSize s) {
                this.size = s;
                return this;
            }

            /**
             * Builds the line spec.
             *
             * @return line spec
             */
            public Line build() {
                return new Line(data, smooth, valueAxis, legend, valueLabels, size,
                        showCategoryLabels);
            }
        }
    }
}
