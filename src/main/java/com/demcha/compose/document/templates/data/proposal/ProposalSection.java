package com.demcha.compose.document.templates.data.proposal;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;

/**
 * Display-oriented proposal section with paragraph content.
 */
public record ProposalSection(
        String title,
        List<String> paragraphs) {

    public ProposalSection {
        title = Objects.requireNonNullElse(title, "");
        paragraphs = List.copyOf(Objects.requireNonNullElse(paragraphs, List.of()));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String title;
        private final List<String> paragraphs = new ArrayList<>();

        private Builder() {
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder paragraphs(List<String> paragraphs) {
            this.paragraphs.clear();
            if (paragraphs != null) {
                this.paragraphs.addAll(paragraphs);
            }
            return this;
        }

        public Builder paragraphs(String... paragraphs) {
            this.paragraphs.clear();
            if (paragraphs != null) {
                this.paragraphs.addAll(List.of(paragraphs));
            }
            return this;
        }

        public Builder addParagraph(String paragraph) {
            this.paragraphs.add(paragraph);
            return this;
        }

        public ProposalSection build() {
            return new ProposalSection(title, paragraphs);
        }
    }
}
