package com.demcha.compose.document.templates.data.schedule;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Label-plus-values row rendered above the roster matrix.
 *
 * @param label metric label
 * @param dayValues values aligned to days in display order
 */
public record ScheduleMetricRow(
        String label,
        List<String> dayValues
) {
    /**
     * Normalizes null metric fields and freezes day values.
     */
    public ScheduleMetricRow {
        label = Objects.requireNonNullElse(label, "");
        dayValues = List.copyOf(Objects.requireNonNullElse(dayValues, List.of()));
    }

    /**
     * Creates a metric row from a label and day values.
     *
     * @param label metric label
     * @param dayValues values aligned to schedule days
     * @return schedule metric row
     */
    public static ScheduleMetricRow of(String label, String... dayValues) {
        return builder()
                .label(label)
                .dayValues(dayValues)
                .build();
    }

    /**
     * Starts a fluent schedule metric row builder.
     *
     * @return metric row builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for schedule metric rows.
     */
    public static final class Builder {
        private String label;
        private final List<String> dayValues = new ArrayList<>();

        private Builder() {
        }

        /**
         * Sets the metric label.
         *
         * @param label metric label
         * @return this builder
         */
        public Builder label(String label) {
            this.label = label;
            return this;
        }

        /**
         * Replaces all day values.
         *
         * @param dayValues day values in display order
         * @return this builder
         */
        public Builder dayValues(List<String> dayValues) {
            this.dayValues.clear();
            if (dayValues != null) {
                this.dayValues.addAll(dayValues);
            }
            return this;
        }

        /**
         * Replaces all day values.
         *
         * @param dayValues day values in display order
         * @return this builder
         */
        public Builder dayValues(String... dayValues) {
            this.dayValues.clear();
            if (dayValues != null) {
                for (String dayValue : dayValues) {
                    this.dayValues.add(dayValue);
                }
            }
            return this;
        }

        /**
         * Appends one day value.
         *
         * @param dayValue day value
         * @return this builder
         */
        public Builder addDayValue(String dayValue) {
            this.dayValues.add(dayValue);
            return this;
        }

        /**
         * Builds an immutable metric row.
         *
         * @return schedule metric row
         */
        public ScheduleMetricRow build() {
            return new ScheduleMetricRow(label, dayValues);
        }
    }
}
