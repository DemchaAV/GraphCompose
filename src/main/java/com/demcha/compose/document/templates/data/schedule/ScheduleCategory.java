package com.demcha.compose.document.templates.data.schedule;

import java.awt.Color;
import java.util.Objects;

/**
 * Shared category catalog entry referenced by days and assignments.
 *
 * @param id stable category identifier
 * @param label display label
 * @param fillColor category fill color
 * @param textColor category text color
 * @param borderColor category border color
 */
public record ScheduleCategory(
        String id,
        String label,
        Color fillColor,
        Color textColor,
        Color borderColor
) {
    /**
     * Normalizes null category fields and colors.
     */
    public ScheduleCategory {
        id = Objects.requireNonNullElse(id, "");
        label = Objects.requireNonNullElse(label, "");
        fillColor = fillColor == null ? Color.WHITE : fillColor;
        textColor = textColor == null ? Color.BLACK : textColor;
        borderColor = borderColor == null ? Color.BLACK : borderColor;
    }

    /**
     * Creates a category using black text.
     *
     * @param id stable category identifier
     * @param label display label
     * @param fillColor fill color
     * @param borderColor border color
     * @return schedule category
     */
    public static ScheduleCategory of(String id, String label, Color fillColor, Color borderColor) {
        return new ScheduleCategory(id, label, fillColor, Color.BLACK, borderColor);
    }

    /**
     * Creates a category with explicit fill, text, and border colors.
     *
     * @param id stable category identifier
     * @param label display label
     * @param fillColor fill color
     * @param textColor text color
     * @param borderColor border color
     * @return schedule category
     */
    public static ScheduleCategory of(String id, String label, Color fillColor, Color textColor, Color borderColor) {
        return new ScheduleCategory(id, label, fillColor, textColor, borderColor);
    }

    /**
     * Starts a fluent schedule category builder.
     *
     * @return schedule category builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for schedule categories.
     */
    public static final class Builder {
        private String id;
        private String label;
        private Color fillColor;
        private Color textColor;
        private Color borderColor;

        private Builder() {
        }

        /**
         * Sets the stable category identifier.
         *
         * @param id category identifier
         * @return this builder
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the visible category label.
         *
         * @param label category label
         * @return this builder
         */
        public Builder label(String label) {
            this.label = label;
            return this;
        }

        /**
         * Sets the fill color.
         *
         * @param fillColor fill color
         * @return this builder
         */
        public Builder fillColor(Color fillColor) {
            this.fillColor = fillColor;
            return this;
        }

        /**
         * Sets the text color.
         *
         * @param textColor text color
         * @return this builder
         */
        public Builder textColor(Color textColor) {
            this.textColor = textColor;
            return this;
        }

        /**
         * Sets the border color.
         *
         * @param borderColor border color
         * @return this builder
         */
        public Builder borderColor(Color borderColor) {
            this.borderColor = borderColor;
            return this;
        }

        /**
         * Builds an immutable category entry.
         *
         * @return schedule category
         */
        public ScheduleCategory build() {
            return new ScheduleCategory(id, label, fillColor, textColor, borderColor);
        }
    }
}
