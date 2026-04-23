package com.demcha.compose.document.templates.data.schedule;

import java.util.ArrayList;
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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String personId;
        private String dayId;
        private String categoryId;
        private final List<ScheduleSlot> slots = new ArrayList<>();
        private String note;

        private Builder() {
        }

        public Builder personId(String personId) {
            this.personId = personId;
            return this;
        }

        public Builder dayId(String dayId) {
            this.dayId = dayId;
            return this;
        }

        public Builder categoryId(String categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public Builder slots(List<ScheduleSlot> slots) {
            this.slots.clear();
            if (slots != null) {
                this.slots.addAll(slots);
            }
            return this;
        }

        public Builder slots(ScheduleSlot... slots) {
            this.slots.clear();
            if (slots != null) {
                for (ScheduleSlot slot : slots) {
                    this.slots.add(slot);
                }
            }
            return this;
        }

        public Builder addSlot(ScheduleSlot slot) {
            this.slots.add(slot);
            return this;
        }

        public Builder slot(String start, String end) {
            return addSlot(new ScheduleSlot(start, end));
        }

        public Builder note(String note) {
            this.note = note;
            return this;
        }

        public ScheduleAssignment build() {
            return new ScheduleAssignment(personId, dayId, categoryId, slots, note);
        }
    }
}
