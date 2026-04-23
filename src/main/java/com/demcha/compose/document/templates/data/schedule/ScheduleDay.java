package com.demcha.compose.document.templates.data.schedule;

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

    public static ScheduleDay of(String id, String label, String headerNote, String headerCategoryId) {
        return new ScheduleDay(id, label, headerNote, headerCategoryId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private String label;
        private String headerNote;
        private String headerCategoryId;

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder headerNote(String headerNote) {
            this.headerNote = headerNote;
            return this;
        }

        public Builder headerCategoryId(String headerCategoryId) {
            this.headerCategoryId = headerCategoryId;
            return this;
        }

        public ScheduleDay build() {
            return new ScheduleDay(id, label, headerNote, headerCategoryId);
        }
    }
}
