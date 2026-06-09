package com.demcha.compose.document.chart;

/**
 * Value-axis configuration. Category-axis labelling comes from
 * {@link ChartData#categories()} directly (toggled per chart spec), so only the
 * numeric axis is configured here.
 *
 * <p>Grid lines and numeric tick labels are independent: a chart can show only
 * the bars/lines with value labels by turning both off
 * ({@code showGridLines(false).showTickLabels(false)}).</p>
 *
 * @param baselineAtZero force the axis to include zero; recommended for bars to
 *                       avoid misleading truncated baselines
 * @param min explicit lower bound, or {@code null} to derive from data
 * @param max explicit upper bound, or {@code null} to derive from data
 * @param format number format applied to axis tick labels
 * @param showGridLines emit a horizontal grid line per major tick
 * @param showTickLabels emit the numeric tick label for each major tick; when
 *                       {@code false} the value axis reserves no left gutter
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public record AxisSpec(
        boolean baselineAtZero,
        Double min,
        Double max,
        NumberFormatSpec format,
        boolean showGridLines,
        boolean showTickLabels
) {
    /** Validates explicit bounds and normalizes the format. */
    public AxisSpec {
        if (min != null && max != null && min >= max) {
            throw new IllegalArgumentException("axis min must be < max: " + min + " >= " + max);
        }
        format = format == null ? NumberFormatSpec.defaults() : format;
    }

    /**
     * Backward-compatible constructor without the tick-label toggle (defaults to
     * showing tick labels).
     *
     * @param baselineAtZero include zero
     * @param min explicit lower bound or {@code null}
     * @param max explicit upper bound or {@code null}
     * @param format tick-label number format
     * @param showGridLines draw grid lines
     */
    public AxisSpec(boolean baselineAtZero, Double min, Double max,
                    NumberFormatSpec format, boolean showGridLines) {
        this(baselineAtZero, min, max, format, showGridLines, true);
    }

    /**
     * Zero-based, data-derived bounds, default format, grid lines and tick labels on.
     *
     * @return default axis spec
     */
    public static AxisSpec defaults() {
        return new AxisSpec(true, null, null, NumberFormatSpec.defaults(), true, true);
    }

    /**
     * Starts a mutable builder.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder for {@link AxisSpec}. */
    public static final class Builder {
        private boolean baselineAtZero = true;
        private Double min;
        private Double max;
        private NumberFormatSpec format = NumberFormatSpec.defaults();
        private boolean showGridLines = true;
        private boolean showTickLabels = true;

        /**
         * Forces the axis to include zero.
         *
         * @param v true to include zero
         * @return this builder
         */
        public Builder baselineAtZero(boolean v) {
            this.baselineAtZero = v;
            return this;
        }

        /**
         * Sets an explicit lower bound.
         *
         * @param v lower bound
         * @return this builder
         */
        public Builder min(double v) {
            this.min = v;
            return this;
        }

        /**
         * Sets an explicit upper bound.
         *
         * @param v upper bound
         * @return this builder
         */
        public Builder max(double v) {
            this.max = v;
            return this;
        }

        /**
         * Sets the tick-label number format.
         *
         * @param f format spec
         * @return this builder
         */
        public Builder format(NumberFormatSpec f) {
            this.format = f;
            return this;
        }

        /**
         * Toggles grid lines.
         *
         * @param v true to draw grid lines
         * @return this builder
         */
        public Builder showGridLines(boolean v) {
            this.showGridLines = v;
            return this;
        }

        /**
         * Toggles the numeric tick labels on the value axis.
         *
         * @param v true to draw tick labels
         * @return this builder
         */
        public Builder showTickLabels(boolean v) {
            this.showTickLabels = v;
            return this;
        }

        /**
         * Builds the axis spec.
         *
         * @return axis spec
         */
        public AxisSpec build() {
            return new AxisSpec(baselineAtZero, min, max, format, showGridLines, showTickLabels);
        }
    }
}
