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

    public static ScheduleSlot of(String start, String end) {
        return new ScheduleSlot(start, end);
    }

    public static Builder builder() {
        return new Builder();
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

    public static final class Builder {
        private String start;
        private String end;

        private Builder() {
        }

        public Builder start(String start) {
            this.start = start;
            return this;
        }

        public Builder end(String end) {
            this.end = end;
            return this;
        }

        public ScheduleSlot build() {
            return new ScheduleSlot(start, end);
        }
    }
}
