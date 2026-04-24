package com.demcha.compose.document.templates.data.schedule;

import java.util.Objects;

/**
 * Person row in the weekly roster matrix.
 *
 * @param id stable person identifier
 * @param displayName display name
 * @param sortOrder ordering token for roster display
 */
public record SchedulePerson(
        String id,
        String displayName,
        int sortOrder
) {
    /**
     * Normalizes null person fields to empty strings.
     */
    public SchedulePerson {
        id = Objects.requireNonNullElse(id, "");
        displayName = Objects.requireNonNullElse(displayName, "");
    }

    /**
     * Creates a person row.
     *
     * @param id stable person identifier
     * @param displayName display name
     * @param sortOrder ordering token
     * @return schedule person
     */
    public static SchedulePerson of(String id, String displayName, int sortOrder) {
        return new SchedulePerson(id, displayName, sortOrder);
    }

    /**
     * Starts a fluent schedule person builder.
     *
     * @return schedule person builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for schedule people.
     */
    public static final class Builder {
        private String id;
        private String displayName;
        private int sortOrder;

        private Builder() {
        }

        /**
         * Sets the stable person identifier.
         *
         * @param id person identifier
         * @return this builder
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the display name.
         *
         * @param displayName display name
         * @return this builder
         */
        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        /**
         * Sets the display order token.
         *
         * @param sortOrder sort order
         * @return this builder
         */
        public Builder sortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        /**
         * Builds an immutable person row.
         *
         * @return schedule person
         */
        public SchedulePerson build() {
            return new SchedulePerson(id, displayName, sortOrder);
        }
    }
}
