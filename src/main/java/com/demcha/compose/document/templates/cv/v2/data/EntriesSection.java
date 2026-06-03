package com.demcha.compose.document.templates.cv.v2.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Section whose body is a list of timeline {@link CvEntry}s.
 *
 * <p>Used for both Education and Professional Experience — they
 * share the same four-field shape and the same canonical render, so
 * we don't pay for a separate type per semantic flavour. If a
 * preset wants to style Education differently from Experience it can
 * branch on the section title at the preset level — the data model
 * stays minimal.</p>
 *
 * @param title   non-blank banner heading
 * @param entries ordered entries (oldest-last by convention)
 */
public record EntriesSection(String title, List<CvEntry> entries)
        implements CvSection {

    /**
     * Validates that {@code title} and {@code entries} are non-null,
     * rejects a blank title, and defensively copies the entry list.
     */
    public EntriesSection {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(entries, "entries");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        entries = List.copyOf(entries);
    }

    /**
     * Fluent builder.
     *
     * @param title non-blank section heading
     * @return new builder
     */
    public static Builder builder(String title) {
        return new Builder(title);
    }

    /**
     * Mutable builder.
     */
    public static final class Builder {
        private final String title;
        private final List<CvEntry> entries = new ArrayList<>();

        private Builder(String title) {
            this.title = title;
        }

        /**
         * Appends one entry built from its four fields.
         *
         * @param title    bold heading (job title, degree)
         * @param subtitle italic subtitle (employer, institution);
         *                 blank collapses the subtitle line
         * @param date     right-aligned date column; blank removes it
         * @param body     full-width prose paragraph; may contain
         *                 inline markdown
         * @return this builder for chaining
         */
        public Builder entry(String title, String subtitle, String date, String body) {
            this.entries.add(new CvEntry(title, subtitle, date, body));
            return this;
        }

        /**
         * Appends one pre-built entry.
         *
         * @param entry the entry to append (non-null)
         * @return this builder for chaining
         */
        public Builder entry(CvEntry entry) {
            this.entries.add(Objects.requireNonNull(entry, "entry"));
            return this;
        }

        /**
         * Builds the immutable {@link EntriesSection}.
         *
         * @return the assembled section
         */
        public EntriesSection build() {
            return new EntriesSection(title, entries);
        }
    }
}
