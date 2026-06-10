package com.demcha.compose.document.chart;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pure tabular chart data — categories plus one or more value series. Holds no
 * type, no colour, no axis configuration: the same {@code ChartData} feeds a
 * bar, line, or sparkline spec unchanged, and it is the unit that serializes
 * cleanly into the JSON document model and arrives from external pipelines.
 *
 * <p>Every series must have exactly {@code categories.size()} values. This
 * invariant is checked at construction so a ragged dataset fails fast at
 * authoring time rather than producing a skewed plot.</p>
 *
 * @param categories ordered category (x-axis) labels; at least one
 * @param series ordered value series; at least one, each aligned to categories
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public record ChartData(List<String> categories, List<Series> series) {

    /** Defensively copies both lists and enforces the alignment invariant. */
    public ChartData {
        Objects.requireNonNull(categories, "categories");
        Objects.requireNonNull(series, "series");
        categories = List.copyOf(categories);
        series = List.copyOf(series);
        if (categories.isEmpty()) {
            throw new IllegalArgumentException("chart needs at least one category");
        }
        if (series.isEmpty()) {
            throw new IllegalArgumentException("chart needs at least one series");
        }
        for (Series s : series) {
            if (s.values().size() != categories.size()) {
                throw new IllegalArgumentException(
                        "series '" + s.name() + "' has " + s.values().size()
                        + " values but there are " + categories.size() + " categories");
            }
        }
    }

    /**
     * Number of value series.
     *
     * @return series count
     */
    public int seriesCount() {
        return series.size();
    }

    /**
     * Number of categories (points per series).
     *
     * @return category count
     */
    public int categoryCount() {
        return categories.size();
    }

    /**
     * Starts a mutable builder.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * One named value series. {@code null} entries are allowed and mean a
     * missing point — a gap in a line, a skipped bar — distinct from {@code 0}.
     *
     * @param name legend label for this series
     * @param values value per category, aligned by index; entries may be null
     */
    public record Series(String name, List<Double> values) {
        /** Normalizes the name and tolerates {@code null} value entries. */
        public Series {
            name = name == null ? "" : name;
            Objects.requireNonNull(values, "values");
            values = java.util.Collections.unmodifiableList(new ArrayList<>(values));
        }

        /**
         * Convenience: build a series from a name and a primitive run.
         *
         * @param name series name
         * @param values value run
         * @return series
         */
        public static Series of(String name, double... values) {
            List<Double> boxed = new ArrayList<>(values.length);
            for (double v : values) {
                boxed.add(v);
            }
            return new Series(name, boxed);
        }
    }

    /** Mutable builder; produces an immutable {@link ChartData}. */
    public static final class Builder {
        private final List<String> categories = new ArrayList<>();
        private final List<Series> series = new ArrayList<>();

        /**
         * Adds category labels.
         *
         * @param labels category labels
         * @return this builder
         */
        public Builder categories(String... labels) {
            categories.addAll(List.of(labels));
            return this;
        }

        /**
         * Adds one category label.
         *
         * @param label category label
         * @return this builder
         */
        public Builder category(String label) {
            categories.add(label);
            return this;
        }

        /**
         * Adds a value series.
         *
         * @param s series
         * @return this builder
         */
        public Builder series(Series s) {
            series.add(Objects.requireNonNull(s, "series"));
            return this;
        }

        /**
         * Adds a value series from a name and a primitive run.
         *
         * @param name series name
         * @param values value run
         * @return this builder
         */
        public Builder series(String name, double... values) {
            return series(Series.of(name, values));
        }

        /**
         * Builds the immutable data.
         *
         * @return chart data
         */
        public ChartData build() {
            return new ChartData(categories, series);
        }
    }
}
