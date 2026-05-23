package com.demcha.compose.document.templates.cv.v2.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Root of the v2 CV data model — required {@link CvIdentity} block
 * plus an ordered list of {@link CvSection} entries that render in
 * source order beneath the header.
 *
 * <p>This record carries no styling or rendering decision. Pair it
 * with a
 * {@link com.demcha.compose.document.templates.cv.v2.theme.CvTheme}
 * and a preset from
 * {@code com.demcha.compose.document.templates.cv.v2.presets} to
 * produce a PDF.</p>
 *
 * @param identity required identity / contact block
 * @param sections ordered sections; rendered in source order
 */
public record CvDocument(CvIdentity identity, List<CvSection> sections) {

    public CvDocument {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(sections, "sections");
        sections = List.copyOf(sections);
    }

    /**
     * @return new fluent builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Mutable builder. Section order in the builder is preserved 1:1
     * in the built document.
     */
    public static final class Builder {
        private CvIdentity identity;
        private final List<CvSection> sections = new ArrayList<>();

        private Builder() {
        }

        public Builder identity(CvIdentity value) {
            this.identity = value;
            return this;
        }

        public Builder section(CvSection section) {
            this.sections.add(Objects.requireNonNull(section, "section"));
            return this;
        }

        /**
         * Varargs convenience for the common case where every section
         * is constructed up-front. Equivalent to chained
         * {@link #section(CvSection)} calls.
         *
         * @param values one or more sections in render order
         * @return this builder
         */
        public Builder sections(CvSection... values) {
            Objects.requireNonNull(values, "values");
            for (CvSection s : values) {
                section(s);
            }
            return this;
        }

        public Builder sections(List<CvSection> values) {
            Objects.requireNonNull(values, "values");
            for (CvSection s : values) {
                section(s);
            }
            return this;
        }

        public CvDocument build() {
            return new CvDocument(identity, sections);
        }
    }
}
