package com.demcha.compose.document.templates.data.proposal;

import java.util.Objects;

/**
 * The display title of a structured proposal, as up to three authored lines.
 *
 * <p>The lines are separate fields rather than one string with newlines
 * because presets are expected to set the lead line apart from the other
 * two typographically — the split carries the emphasis and is part of the
 * content, not of the layout.</p>
 *
 * @param lead   the first title line
 * @param second the second title line
 * @param third  the third title line
 */
public record ProposalTitleLines(String lead, String second, String third) {

    /**
     * Normalizes optional lines to empty strings.
     */
    public ProposalTitleLines {
        lead = Objects.requireNonNullElse(lead, "");
        second = Objects.requireNonNullElse(second, "");
        third = Objects.requireNonNullElse(third, "");
    }
}
