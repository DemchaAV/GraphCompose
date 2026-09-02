package com.demcha.compose.document.templates.data.proposal;

import java.util.List;
import java.util.Objects;

/**
 * The executive-summary block of a structured proposal: an icon-badged
 * heading and its body paragraphs.
 *
 * <p>The narrative model's {@link ProposalData#executiveSummary()} is one
 * paragraph with a preset-owned heading; this block owns its heading, its
 * icon token, and as many paragraphs as the author wrote.</p>
 *
 * @param heading    the section heading, as authored
 * @param icon       the icon token of the section badge
 * @param paragraphs the summary paragraphs, in order
 */
public record ProposalSummaryBlock(String heading, String icon, List<String> paragraphs) {

    /**
     * Normalizes optional fields and freezes the paragraph list.
     */
    public ProposalSummaryBlock {
        heading = Objects.requireNonNullElse(heading, "");
        icon = Objects.requireNonNullElse(icon, "");
        paragraphs = List.copyOf(Objects.requireNonNullElse(paragraphs, List.of()));
    }
}
