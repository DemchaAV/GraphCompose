package com.demcha.templates.data;

import java.util.Objects;

/**
 * Weekly day header information.
 */
public record ScheduleDay(
        String id,
        String label,
        String headerNote,
        String headerCategoryId
) {
    public ScheduleDay {
        id = Objects.requireNonNullElse(id, "");
        label = Objects.requireNonNullElse(label, "");
        headerNote = Objects.requireNonNullElse(headerNote, "");
        headerCategoryId = Objects.requireNonNullElse(headerCategoryId, "");
    }
}
