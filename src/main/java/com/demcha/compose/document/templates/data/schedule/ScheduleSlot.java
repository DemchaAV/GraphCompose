package com.demcha.compose.document.templates.data.schedule;

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

    /**
     * Returns the compact display text used inside rendered cells.
     *
     * @return human-readable slot text
     */
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
