package com.demcha.compose.document.templates.cv.v2.data;

import java.util.Objects;

/**
 * Timeline-style entry used inside an {@link EntriesSection}. Covers
 * both Education and Professional Experience — they share the same
 * four fields so authors don't have to learn two record types.
 *
 * <p>Blank fields are honoured: a blank {@code date} omits the date
 * column, a blank {@code subtitle} drops the italic line, a blank
 * {@code body} drops the description paragraph.</p>
 *
 * @param title    bold heading on the left (job title, degree)
 * @param subtitle italic subtitle on the line below (employer,
 *                 institution); blank collapses the subtitle line
 * @param date     right-aligned date column next to title
 *                 (e.g. {@code "2024-Present"}, {@code "2021"});
 *                 blank removes the date column
 * @param body     full-width prose paragraph beneath the subtitle;
 *                 may contain inline markdown
 */
public record CvEntry(String title, String subtitle, String date, String body) {

    /** Validates that every field is non-null and that {@code title} is non-blank. */
    public CvEntry {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(subtitle, "subtitle");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(body, "body");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
    }
}
