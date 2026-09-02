package com.demcha.compose.document.templates.data.rota;

import java.util.Objects;

/**
 * Which week the rota covers.
 *
 * @param title       the sheet's own title; blank when the design carries none
 * @param rangeLabel  the span the week runs, as printed
 * @since 2.4.0
 */
public record RotaWeek(String title, String rangeLabel) {

    /**
     * Normalizes optional fields.
     */
    public RotaWeek {
        title = Objects.requireNonNullElse(title, "");
        rangeLabel = Objects.requireNonNullElse(rangeLabel, "");
    }

    /**
     * Whether there is anything to draw.
     *
     * @return {@code true} when the week states a title or a range
     */
    public boolean isPresent() {
        return !title.isBlank() || !rangeLabel.isBlank();
    }
}
