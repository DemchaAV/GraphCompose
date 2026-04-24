package com.demcha.compose.document.templates.data.schedule;

import java.util.Objects;

/**
 * Weekly day header information.
 *
 * @param id stable day identifier
 * @param label display day label
 * @param headerNote optional header note
 * @param headerCategoryId optional category shown in the header
 */
public record ScheduleDay(
        String id,
        String label,
        String headerNote,
        String headerCategoryId
) {
    /**
     * Normalizes null day fields to empty strings.
     */
    public ScheduleDay {
        id = Objects.requireNonNullElse(id, "");
        label = Objects.requireNonNullElse(label, "");
        headerNote = Objects.requireNonNullElse(headerNote, "");
        headerCategoryId = Objects.requireNonNullElse(headerCategoryId, "");
    }

    /**
     * Creates a day header entry.
     *
     * @param id stable day identifier
     * @param label display day label
     * @param headerNote optional header note
     * @param headerCategoryId optional header category identifier
     * @return schedule day
     */
    public static ScheduleDay of(String id, String label, String headerNote, String headerCategoryId) {
        return new ScheduleDay(id, label, headerNote, headerCategoryId);
    }

    /**
     * Starts a fluent schedule day builder.
     *
     * @return schedule day builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for day header entries.
     */
    public static final class Builder {
        private String id;
        private String label;
        private String headerNote;
        private String headerCategoryId;

        private Builder() {
        }

        /**
         * Sets the stable day identifier.
         *
         * @param id day identifier
         * @return this builder
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the visible day label.
         *
         * @param label day label
         * @return this builder
         */
        public Builder label(String label) {
            this.label = label;
            return this;
        }

        /**
         * Sets the optional header note.
         *
         * @param headerNote header note
         * @return this builder
         */
        public Builder headerNote(String headerNote) {
            this.headerNote = headerNote;
            return this;
        }

        /**
         * Sets the optional header category identifier.
         *
         * @param headerCategoryId header category identifier
         * @return this builder
         */
        public Builder headerCategoryId(String headerCategoryId) {
            this.headerCategoryId = headerCategoryId;
            return this;
        }

        /**
         * Builds an immutable day entry.
         *
         * @return schedule day
         */
        public ScheduleDay build() {
            return new ScheduleDay(id, label, headerNote, headerCategoryId);
        }
    }
}
