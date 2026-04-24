package com.demcha.compose.document.templates.data.schedule;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Display-oriented assignment for one person/day cell.
 *
 * @param personId person identifier
 * @param dayId day identifier
 * @param categoryId category identifier
 * @param slots shift/time slots
 * @param note optional assignment note
 */
public record ScheduleAssignment(
        String personId,
        String dayId,
        String categoryId,
        List<ScheduleSlot> slots,
        String note
) {
    /**
     * Normalizes null assignment fields and freezes slot order.
     */
    public ScheduleAssignment {
        personId = Objects.requireNonNullElse(personId, "");
        dayId = Objects.requireNonNullElse(dayId, "");
        categoryId = Objects.requireNonNullElse(categoryId, "");
        slots = List.copyOf(Objects.requireNonNullElse(slots, List.of()));
        note = Objects.requireNonNullElse(note, "");
    }

    /**
     * Starts a fluent schedule assignment builder.
     *
     * @return assignment builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for one person/day assignment cell.
     */
    public static final class Builder {
        private String personId;
        private String dayId;
        private String categoryId;
        private final List<ScheduleSlot> slots = new ArrayList<>();
        private String note;

        private Builder() {
        }

        /**
         * Sets the person identifier.
         *
         * @param personId person identifier
         * @return this builder
         */
        public Builder personId(String personId) {
            this.personId = personId;
            return this;
        }

        /**
         * Sets the day identifier.
         *
         * @param dayId day identifier
         * @return this builder
         */
        public Builder dayId(String dayId) {
            this.dayId = dayId;
            return this;
        }

        /**
         * Sets the category identifier.
         *
         * @param categoryId category identifier
         * @return this builder
         */
        public Builder categoryId(String categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        /**
         * Replaces all slot rows.
         *
         * @param slots shift/time slots
         * @return this builder
         */
        public Builder slots(List<ScheduleSlot> slots) {
            this.slots.clear();
            if (slots != null) {
                this.slots.addAll(slots);
            }
            return this;
        }

        /**
         * Replaces all slot rows.
         *
         * @param slots shift/time slots
         * @return this builder
         */
        public Builder slots(ScheduleSlot... slots) {
            this.slots.clear();
            if (slots != null) {
                for (ScheduleSlot slot : slots) {
                    this.slots.add(slot);
                }
            }
            return this;
        }

        /**
         * Appends a slot row.
         *
         * @param slot schedule slot
         * @return this builder
         */
        public Builder addSlot(ScheduleSlot slot) {
            this.slots.add(slot);
            return this;
        }

        /**
         * Appends a slot row from start and end labels.
         *
         * @param start start label
         * @param end end label
         * @return this builder
         */
        public Builder slot(String start, String end) {
            return addSlot(new ScheduleSlot(start, end));
        }

        /**
         * Sets the optional assignment note.
         *
         * @param note note text
         * @return this builder
         */
        public Builder note(String note) {
            this.note = note;
            return this;
        }

        /**
         * Builds an immutable assignment.
         *
         * @return schedule assignment
         */
        public ScheduleAssignment build() {
            return new ScheduleAssignment(personId, dayId, categoryId, slots, note);
        }
    }
}
