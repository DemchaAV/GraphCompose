package com.demcha.compose.document.templates.cv.v2.data;

import java.util.Objects;

/**
 * Section whose body is a single block of free prose — used for
 * Professional Summary, Profile, Objective, and similar one-paragraph
 * headings.
 *
 * <p>Inline markdown markers ({@code **bold**}, {@code *italic*},
 * {@code _italic_}) are honoured by the renderer.</p>
 *
 * @param title non-blank banner heading (e.g. "Professional Summary")
 * @param body  prose; may contain inline markdown
 */
public record ParagraphSection(String title, String body) implements CvSection {

    /** Validates that every field is non-null and that {@code title} is non-blank. */
    public ParagraphSection {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(body, "body");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
    }
}
