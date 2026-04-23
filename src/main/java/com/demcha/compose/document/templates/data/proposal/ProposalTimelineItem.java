package com.demcha.compose.document.templates.data.proposal;

import java.util.Objects;

/**
 * Display-oriented timeline item for proposals.
 */
public record ProposalTimelineItem(
        String phase,
        String duration,
        String details) {

    public ProposalTimelineItem {
        phase = Objects.requireNonNullElse(phase, "");
        duration = Objects.requireNonNullElse(duration, "");
        details = Objects.requireNonNullElse(details, "");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String phase;
        private String duration;
        private String details;

        private Builder() {
        }

        public Builder phase(String phase) {
            this.phase = phase;
            return this;
        }

        public Builder duration(String duration) {
            this.duration = duration;
            return this;
        }

        public Builder details(String details) {
            this.details = details;
            return this;
        }

        public ProposalTimelineItem build() {
            return new ProposalTimelineItem(phase, duration, details);
        }
    }
}
