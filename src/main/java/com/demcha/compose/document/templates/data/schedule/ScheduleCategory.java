package com.demcha.compose.document.templates.data.schedule;

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

    public static ScheduleCategory of(String id, String label, Color fillColor, Color borderColor) {
        return new ScheduleCategory(id, label, fillColor, Color.BLACK, borderColor);
    }

    public static ScheduleCategory of(String id, String label, Color fillColor, Color textColor, Color borderColor) {
        return new ScheduleCategory(id, label, fillColor, textColor, borderColor);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private String label;
        private Color fillColor;
        private Color textColor;
        private Color borderColor;

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

        public Builder fillColor(Color fillColor) {
            this.fillColor = fillColor;
            return this;
        }

        public Builder textColor(Color textColor) {
            this.textColor = textColor;
            return this;
        }

        public Builder borderColor(Color borderColor) {
            this.borderColor = borderColor;
            return this;
        }

        public ScheduleCategory build() {
            return new ScheduleCategory(id, label, fillColor, textColor, borderColor);
        }
    }
}
