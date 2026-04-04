package com.demcha.templates.data;

import java.awt.Color;
import java.util.Objects;

/**
 * Shared category catalog entry referenced by days and assignments.
 */
public record ScheduleCategory(
        String id,
        String label,
        Color fillColor,
        Color textColor,
        Color borderColor
) {
    public ScheduleCategory {
        id = Objects.requireNonNullElse(id, "");
        label = Objects.requireNonNullElse(label, "");
        fillColor = fillColor == null ? Color.WHITE : fillColor;
        textColor = textColor == null ? Color.BLACK : textColor;
        borderColor = borderColor == null ? Color.BLACK : borderColor;
    }
}
