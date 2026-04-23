package com.demcha.compose.document.templates.data.schedule;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Label-plus-values row rendered above the roster matrix.
 */
public record ScheduleMetricRow(
        String label,
        List<String> dayValues
) {
    public ScheduleMetricRow {
        label = Objects.requireNonNullElse(label, "");
        dayValues = List.copyOf(Objects.requireNonNullElse(dayValues, List.of()));
    }

    public static ScheduleMetricRow of(String label, String... dayValues) {
        return builder()
                .label(label)
                .dayValues(dayValues)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String label;
        private final List<String> dayValues = new ArrayList<>();

        private Builder() {
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder dayValues(List<String> dayValues) {
            this.dayValues.clear();
            if (dayValues != null) {
                this.dayValues.addAll(dayValues);
            }
            return this;
        }

        public Builder dayValues(String... dayValues) {
            this.dayValues.clear();
            if (dayValues != null) {
                for (String dayValue : dayValues) {
                    this.dayValues.add(dayValue);
                }
            }
            return this;
        }

        public Builder addDayValue(String dayValue) {
            this.dayValues.add(dayValue);
            return this;
        }

        public ScheduleMetricRow build() {
            return new ScheduleMetricRow(label, dayValues);
        }
    }
}
