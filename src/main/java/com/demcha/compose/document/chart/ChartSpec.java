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
public sealed interface ChartSpec permits ChartSpec.Bar, ChartSpec.Line, ChartSpec.Pie {

    /**
     * Tabular data backing this chart.
     *
     * @return chart data
     */
    ChartData data();

    /**
     * Number format for the chart's data values. Axis-based kinds delegate to
     * their value axis; a pie has no axis and carries its own format. Used by
     * value labels and by semantic exports that render the chart's data table.
     *
     * @return value number format
     */
    NumberFormatSpec valueFormat();

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
     * Starts a pie/donut-chart builder.
     *
     * @return pie builder
     */
    static Pie.Builder pie() {
        return new Pie.Builder();
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

        @Override
        public NumberFormatSpec valueFormat() {
            return valueAxis.format();
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
     * @param area fill the region between each series and the axis baseline
     *             (translucent series colour; see {@code ChartStyle.areaOpacity})
     * @param valueAxis numeric-axis configuration
     * @param legend legend placement
     * @param valueLabels per-point value label mode
     * @param size width-responsive sizing policy
     * @param showCategoryLabels emit the category (x-axis) label under each slot
     */
    record Line(
            ChartData data,
            boolean smooth,
            boolean area,
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

        @Override
        public NumberFormatSpec valueFormat() {
            return valueAxis.format();
        }

        /**
         * Backward-compatible constructor without the area and category-label
         * toggles (no area fill; category labels shown).
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
            this(data, smooth, false, valueAxis, legend, valueLabels, size, true);
        }

        /** Fluent builder for a {@link Line} spec. */
        public static final class Builder {
            private ChartData data;
            private boolean smooth = false;
            private boolean area = false;
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
             * Fills the region between each series and the axis baseline with a
             * translucent series-colour area.
             *
             * @param v true to fill the area
             * @return this builder
             */
            public Builder area(boolean v) {
                this.area = v;
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
                return new Line(data, smooth, area, valueAxis, legend, valueLabels, size,
                        showCategoryLabels);
            }
        }
    }

    /**
     * Pie / donut chart: one slice per category from a single series. An axis is
     * unrepresentable here by design — the spec carries its own value/percent
     * formats instead.
     *
     * @param data tabular data; must contain exactly one series
     * @param donutRatio 0 for a solid pie; (0..0.9] = hole radius as a fraction
     *                   of the outer radius
     * @param startAngleDegrees angle of the first slice's leading edge
     *                          (90 = twelve o'clock)
     * @param clockwise slice layout direction
     * @param sliceLabels what each slice's outside label shows
     * @param valueFormat format for {@link SliceLabelMode#VALUE} labels
     * @param percentFormat format for percent labels (suffix included)
     * @param centerText optional KPI text in the donut hole; requires
     *                   {@code donutRatio > 0}
     * @param legend legend placement; lists category names
     * @param size width-responsive sizing policy (1:1 by default)
     */
    record Pie(
            ChartData data,
            double donutRatio,
            double startAngleDegrees,
            boolean clockwise,
            SliceLabelMode sliceLabels,
            NumberFormatSpec valueFormat,
            NumberFormatSpec percentFormat,
            String centerText,
            LegendPosition legend,
            ChartSize size
    ) implements ChartSpec {
        /** Validates the single-series invariant and the ratio/angle ranges. */
        public Pie {
            Objects.requireNonNull(data, "data");
            if (data.seriesCount() != 1) {
                throw new IllegalArgumentException(
                        "pie charts plot exactly one series; got " + data.seriesCount()
                        + " — pass a single Series or use a bar chart");
            }
            if (donutRatio < 0 || donutRatio > 0.9 || Double.isNaN(donutRatio)) {
                throw new IllegalArgumentException("donutRatio must be in [0, 0.9]: " + donutRatio);
            }
            if (Double.isNaN(startAngleDegrees) || Double.isInfinite(startAngleDegrees)) {
                throw new IllegalArgumentException("startAngleDegrees must be finite: " + startAngleDegrees);
            }
            sliceLabels = sliceLabels == null ? SliceLabelMode.NONE : sliceLabels;
            valueFormat = valueFormat == null ? NumberFormatSpec.defaults() : valueFormat;
            percentFormat = percentFormat == null
                    ? NumberFormatSpec.pattern("#,##0.#").withSuffix("%") : percentFormat;
            legend = legend == null ? LegendPosition.NONE : legend;
            size = size == null ? ChartSize.aspectRatio(1, 1) : size;
            if (centerText != null && donutRatio <= 0) {
                throw new IllegalArgumentException(
                        "centerText requires a donut hole — set donutRatio > 0");
            }
        }

        /** Fluent builder for a {@link Pie} spec. */
        public static final class Builder {
            private ChartData data;
            private double donutRatio = 0.0;
            private double startAngleDegrees = 90.0;
            private boolean clockwise = true;
            private SliceLabelMode sliceLabels = SliceLabelMode.NONE;
            private NumberFormatSpec valueFormat = NumberFormatSpec.defaults();
            private NumberFormatSpec percentFormat =
                    NumberFormatSpec.pattern("#,##0.#").withSuffix("%");
            private String centerText;
            private LegendPosition legend = LegendPosition.NONE;
            private ChartSize size = ChartSize.aspectRatio(1, 1);

            /**
             * Sets the data (exactly one series).
             *
             * @param d chart data
             * @return this builder
             */
            public Builder data(ChartData d) {
                this.data = d;
                return this;
            }

            /**
             * Sets the donut hole radius fraction.
             *
             * @param ratio 0 for a solid pie, up to 0.9
             * @return this builder
             */
            public Builder donutRatio(double ratio) {
                this.donutRatio = ratio;
                return this;
            }

            /**
             * Sets the first slice's leading-edge angle.
             *
             * @param degrees angle, 90 = twelve o'clock
             * @return this builder
             */
            public Builder startAngleDegrees(double degrees) {
                this.startAngleDegrees = degrees;
                return this;
            }

            /**
             * Sets the slice layout direction.
             *
             * @param v true for clockwise
             * @return this builder
             */
            public Builder clockwise(boolean v) {
                this.clockwise = v;
                return this;
            }

            /**
             * Sets what slice labels show.
             *
             * @param mode slice label mode
             * @return this builder
             */
            public Builder sliceLabels(SliceLabelMode mode) {
                this.sliceLabels = mode;
                return this;
            }

            /**
             * Sets the VALUE label format.
             *
             * @param f format spec
             * @return this builder
             */
            public Builder valueFormat(NumberFormatSpec f) {
                this.valueFormat = f;
                return this;
            }

            /**
             * Sets the percent label format.
             *
             * @param f format spec (suffix included)
             * @return this builder
             */
            public Builder percentFormat(NumberFormatSpec f) {
                this.percentFormat = f;
                return this;
            }

            /**
             * Sets the donut-centre KPI text.
             *
             * @param text centre text
             * @return this builder
             */
            public Builder centerText(String text) {
                this.centerText = text;
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
             * Builds the pie spec.
             *
             * @return pie spec
             */
            public Pie build() {
                return new Pie(data, donutRatio, startAngleDegrees, clockwise, sliceLabels,
                        valueFormat, percentFormat, centerText, legend, size);
            }
        }
    }
}
