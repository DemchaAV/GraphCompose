package com.demcha.compose.document.templates.data.proposal;

import java.util.List;
import java.util.Objects;

/**
 * The bulleted terms block of a structured proposal.
 *
 * <p>Distinct from the narrative model's {@link ProposalData#acceptanceTerms()}:
 * this block owns its heading and icon and is its own section, not part of
 * the acceptance card.</p>
 *
 * @param heading the section heading
 * @param icon    the icon token of the section badge
 * @param items   the term bullets, in order
 */
public record ProposalTermsBlock(String heading, String icon, List<String> items) {

    /**
     * Normalizes optional fields and freezes the item list.
     */
    public ProposalTermsBlock {
        heading = Objects.requireNonNullElse(heading, "");
        icon = Objects.requireNonNullElse(icon, "");
        items = List.copyOf(Objects.requireNonNullElse(items, List.of()));
    }
}
