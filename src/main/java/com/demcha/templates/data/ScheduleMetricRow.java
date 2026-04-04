package com.demcha.templates.data;

import java.util.List;
import java.util.Objects;

/**
 * Label + per-day values row shown above the roster matrix.
 */
public record ScheduleMetricRow(
        String label,
        List<String> dayValues
) {
    public ScheduleMetricRow {
        label = Objects.requireNonNullElse(label, "");
        dayValues = List.copyOf(Objects.requireNonNullElse(dayValues, List.of()));
    }
}
