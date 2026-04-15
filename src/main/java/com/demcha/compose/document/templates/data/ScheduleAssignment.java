package com.demcha.compose.document.templates.data;

import java.util.List;
import java.util.Objects;

/**
 * Display-oriented assignment for one person/day cell.
 */
public record ScheduleAssignment(
        String personId,
        String dayId,
        String categoryId,
        List<ScheduleSlot> slots,
        String note
) {
    public ScheduleAssignment {
        personId = Objects.requireNonNullElse(personId, "");
        dayId = Objects.requireNonNullElse(dayId, "");
        categoryId = Objects.requireNonNullElse(categoryId, "");
        slots = List.copyOf(Objects.requireNonNullElse(slots, List.of()));
        note = Objects.requireNonNullElse(note, "");
    }
}
