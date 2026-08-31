package com.demcha.compose.document.templates.cv.data;

import java.util.List;
import java.util.Objects;

/**
 * Timeline-style entry used inside an {@link EntriesSection}. Covers
 * Education, Professional Experience, Projects and anything else a preset
 * lays out as a dated block — they share the same fields so authors don't
 * have to learn a record type per section.
 *
 * <p>Blank fields are honoured: a blank {@code date} omits the date
 * column, a blank {@code subtitle} drops the italic line, a blank
 * {@code body} drops the description paragraph, a blank {@code place}
 * drops the location, and a blank {@code icon} leaves the entry unmarked.
 * A preset draws only what it has somewhere to put.</p>
 *
 * @param title    bold heading on the left (job title, degree)
 * @param subtitle italic subtitle on the line below (employer,
 *                 institution); blank collapses the subtitle line
 * @param date     right-aligned date column next to title
 *                 (e.g. {@code "2024-Present"}, {@code "2021"});
 *                 blank removes the date column
 * @param body     full-width prose paragraph beneath the subtitle;
 *                 may contain inline markdown
 * @param place    where this happened — a city, a campus, "Remote". The
 *                 designs that show it set it beside the employer or the
 *                 years rather than inside them, which is why it is its own
 *                 field; blank when absent
 * @param icon     token naming the mark a preset draws for this entry. The
 *                 vocabulary is preset-scoped — each preset packages its own
 *                 set — so a token means something only to the preset that
 *                 declares it, and the presets that draw no marks ignore it;
 *                 blank when absent
 */
public record CvEntry(String title, String subtitle, String date, String body,
                      String place, String icon) {

    /**
     * Validates that the four original fields are non-null and that
     * {@code title} is non-blank, treating a null {@code place} or
     * {@code icon} as absent.
     */
    public CvEntry {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(subtitle, "subtitle");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(body, "body");
        place = place == null ? "" : place;
        icon = icon == null ? "" : icon;
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
    }

    /**
     * Backward-compatible constructor for callers that predate the location
     * and the mark. The entry simply carries neither.
     *
     * @param title    bold heading on the left
     * @param subtitle subtitle on the line below; blank collapses it
     * @param date     date column next to the title; blank removes it
     * @param body     prose paragraph beneath the subtitle
     */
    public CvEntry(String title, String subtitle, String date, String body) {
        this(title, subtitle, date, body, "", "");
    }

    /**
     * Creates a fluent builder, which is the readable way to reach the
     * optional fields without counting positions.
     *
     * @param title the entry's heading (required, non-blank)
     * @return new fluent builder
     * @since 2.2.3
     */
    public static Builder builder(String title) {
        return new Builder(title);
    }

    /**
     * Mutable builder for {@link CvEntry}. Every field but the title starts
     * blank, so an entry names only what it has.
     *
     * @since 2.2.3
     */
    public static final class Builder {
        private final String title;
        private String subtitle = "";
        private String date = "";
        private String body = "";
        private String place = "";
        private String icon = "";

        private Builder(String title) {
            this.title = title;
        }

        /**
         * Sets the subtitle — the employer, the institution, the stack.
         *
         * @param value the subtitle; null becomes blank
         * @return this builder for chaining
         */
        public Builder subtitle(String value) {
            this.subtitle = value == null ? "" : value;
            return this;
        }

        /**
         * Sets the date column.
         *
         * @param value the date text; null becomes blank
         * @return this builder for chaining
         */
        public Builder date(String value) {
            this.date = value == null ? "" : value;
            return this;
        }

        /**
         * Sets the prose body.
         *
         * @param value the body; null becomes blank
         * @return this builder for chaining
         */
        public Builder body(String value) {
            this.body = value == null ? "" : value;
            return this;
        }

        /**
         * Sets the prose body from one line per element — the shape the
         * presets that draw a bulleted entry read.
         *
         * @param lines the lines, in order; null becomes blank
         * @return this builder for chaining
         */
        public Builder body(List<String> lines) {
            this.body = lines == null ? "" : String.join("\n", lines);
            return this;
        }

        /**
         * Sets where this happened.
         *
         * @param value the location; null becomes blank
         * @return this builder for chaining
         */
        public Builder place(String value) {
            this.place = value == null ? "" : value;
            return this;
        }

        /**
         * Sets the mark this entry asks for, in the vocabulary of the preset
         * that will draw it.
         *
         * @param value the icon token; null becomes blank
         * @return this builder for chaining
         */
        public Builder icon(String value) {
            this.icon = value == null ? "" : value;
            return this;
        }

        /**
         * Builds the immutable {@link CvEntry}.
         *
         * @return the assembled entry
         */
        public CvEntry build() {
            return new CvEntry(title, subtitle, date, body, place, icon);
        }
    }
}
