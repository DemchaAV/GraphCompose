package com.demcha.compose.document.templates.data.schedule;

import java.util.Objects;

/**
 * Person row in the weekly roster matrix.
 */
public record SchedulePerson(
        String id,
        String displayName,
        int sortOrder
) {
    public SchedulePerson {
        id = Objects.requireNonNullElse(id, "");
        displayName = Objects.requireNonNullElse(displayName, "");
    }

    public static SchedulePerson of(String id, String displayName, int sortOrder) {
        return new SchedulePerson(id, displayName, sortOrder);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private String displayName;
        private int sortOrder;

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder sortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public SchedulePerson build() {
            return new SchedulePerson(id, displayName, sortOrder);
        }
    }
}
