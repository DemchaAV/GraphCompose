package com.demcha.compose.document.templates.data.schedule;

import java.util.List;
import java.util.Objects;

/**
 * Display-oriented weekly schedule document input.
 */
public record WeeklyScheduleData(
        String title,
        String weekLabel,
        List<ScheduleDay> days,
        List<ScheduleCategory> categories,
        List<ScheduleMetricRow> headerMetrics,
        List<SchedulePerson> people,
        List<ScheduleAssignment> assignments,
        List<String> footerNotes
) {
    public WeeklyScheduleData {
        title = Objects.requireNonNullElse(title, "Weekly Schedule");
        weekLabel = Objects.requireNonNullElse(weekLabel, "");
        days = List.copyOf(Objects.requireNonNullElse(days, List.of()));
        categories = List.copyOf(Objects.requireNonNullElse(categories, List.of()));
        headerMetrics = List.copyOf(Objects.requireNonNullElse(headerMetrics, List.of()));
        people = List.copyOf(Objects.requireNonNullElse(people, List.of()));
        assignments = List.copyOf(Objects.requireNonNullElse(assignments, List.of()));
        footerNotes = List.copyOf(Objects.requireNonNullElse(footerNotes, List.of()));
    }
}
