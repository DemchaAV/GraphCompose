package com.demcha.templates.data;

import java.util.Objects;

/**
 * Display-first shift segment rendered inside a schedule cell.
 */
public record ScheduleSlot(
        String start,
        String end
) {
    public ScheduleSlot {
        start = Objects.requireNonNullElse(start, "");
        end = Objects.requireNonNullElse(end, "");
    }

    public String displayText() {
        if (start.isBlank() && end.isBlank()) {
            return "";
        }
        if (start.isBlank()) {
            return end;
        }
        if (end.isBlank()) {
            return start;
        }
        return start + " " + end;
    }
}
