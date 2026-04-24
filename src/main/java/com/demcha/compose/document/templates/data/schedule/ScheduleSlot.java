package com.demcha.compose.document.templates.data.schedule;

import java.util.Objects;

/**
 * Display-first shift segment rendered inside a schedule cell.
 *
 * @param start start time/label
 * @param end end time/label
 */
public record ScheduleSlot(
        String start,
        String end
) {
    /**
     * Normalizes null slot labels to empty strings.
     */
    public ScheduleSlot {
        start = Objects.requireNonNullElse(start, "");
        end = Objects.requireNonNullElse(end, "");
    }

    /**
     * Creates a schedule slot from start and end labels.
     *
     * @param start start label
     * @param end end label
     * @return schedule slot
     */
    public static ScheduleSlot of(String start, String end) {
        return new ScheduleSlot(start, end);
    }

    /**
     * Starts a fluent schedule slot builder.
     *
     * @return schedule slot builder
     */
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

    /**
     * Fluent builder for schedule slots.
     */
    public static final class Builder {
        private String start;
        private String end;

        private Builder() {
        }

        /**
         * Sets the start label.
         *
         * @param start start label
         * @return this builder
         */
        public Builder start(String start) {
            this.start = start;
            return this;
        }

        /**
         * Sets the end label.
         *
         * @param end end label
         * @return this builder
         */
        public Builder end(String end) {
            this.end = end;
            return this;
        }

        /**
         * Builds an immutable schedule slot.
         *
         * @return schedule slot
         */
        public ScheduleSlot build() {
            return new ScheduleSlot(start, end);
        }
    }
}
