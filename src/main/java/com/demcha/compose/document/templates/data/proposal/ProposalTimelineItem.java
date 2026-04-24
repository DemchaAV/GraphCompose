package com.demcha.compose.document.templates.data.proposal;

import java.util.Objects;

/**
 * Display-oriented timeline item for proposals.
 *
 * @param phase timeline phase label
 * @param duration display duration
 * @param details supporting details
 */
public record ProposalTimelineItem(
        String phase,
        String duration,
        String details) {

    /**
     * Normalizes null timeline fields to empty strings.
     */
    public ProposalTimelineItem {
        phase = Objects.requireNonNullElse(phase, "");
        duration = Objects.requireNonNullElse(duration, "");
        details = Objects.requireNonNullElse(details, "");
    }

    /**
     * Starts a fluent proposal timeline item builder.
     *
     * @return timeline item builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for proposal timeline items.
     */
    public static final class Builder {
        private String phase;
        private String duration;
        private String details;

        private Builder() {
        }

        /**
         * Sets the phase label.
         *
         * @param phase phase label
         * @return this builder
         */
        public Builder phase(String phase) {
            this.phase = phase;
            return this;
        }

        /**
         * Sets the duration text.
         *
         * @param duration duration text
         * @return this builder
         */
        public Builder duration(String duration) {
            this.duration = duration;
            return this;
        }

        /**
         * Sets supporting timeline details.
         *
         * @param details timeline details
         * @return this builder
         */
        public Builder details(String details) {
            this.details = details;
            return this;
        }

        /**
         * Builds immutable timeline item data.
         *
         * @return proposal timeline item
         */
        public ProposalTimelineItem build() {
            return new ProposalTimelineItem(phase, duration, details);
        }
    }
}
