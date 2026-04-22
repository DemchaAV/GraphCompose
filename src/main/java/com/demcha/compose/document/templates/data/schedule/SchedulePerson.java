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
}
