package com.demcha.compose.document.templates.data.proposal;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;

/**
 * Display-oriented proposal section with paragraph content.
 *
 * @param title section title
 * @param paragraphs section paragraphs in display order
 */
public record ProposalSection(
        String title,
        List<String> paragraphs) {

    /**
     * Normalizes null section fields and freezes paragraph order.
     */
    public ProposalSection {
        title = Objects.requireNonNullElse(title, "");
        paragraphs = List.copyOf(Objects.requireNonNullElse(paragraphs, List.of()));
    }

    /**
     * Starts a fluent proposal section builder.
     *
     * @return section builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for proposal sections.
     */
    public static final class Builder {
        private String title;
        private final List<String> paragraphs = new ArrayList<>();

        private Builder() {
        }

        /**
         * Sets the section title.
         *
         * @param title section title
         * @return this builder
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Replaces all section paragraphs.
         *
         * @param paragraphs paragraphs in display order
         * @return this builder
         */
        public Builder paragraphs(List<String> paragraphs) {
            this.paragraphs.clear();
            if (paragraphs != null) {
                this.paragraphs.addAll(paragraphs);
            }
            return this;
        }

        /**
         * Replaces all section paragraphs.
         *
         * @param paragraphs paragraphs in display order
         * @return this builder
         */
        public Builder paragraphs(String... paragraphs) {
            this.paragraphs.clear();
            if (paragraphs != null) {
                this.paragraphs.addAll(List.of(paragraphs));
            }
            return this;
        }

        /**
         * Appends one section paragraph.
         *
         * @param paragraph paragraph text
         * @return this builder
         */
        public Builder addParagraph(String paragraph) {
            this.paragraphs.add(paragraph);
            return this;
        }

        /**
         * Builds immutable proposal section data.
         *
         * @return proposal section
         */
        public ProposalSection build() {
            return new ProposalSection(title, paragraphs);
        }
    }
}
