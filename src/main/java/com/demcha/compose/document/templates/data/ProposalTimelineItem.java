package com.demcha.compose.document.templates.data;

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
}
