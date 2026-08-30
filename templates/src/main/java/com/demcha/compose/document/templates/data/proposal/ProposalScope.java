package com.demcha.compose.document.templates.data.proposal;

import java.util.List;
import java.util.Objects;

/**
 * The numbered scope-of-work list of a structured proposal.
 *
 * <p>Distinct from the narrative model's {@link ProposalSection}: a scope
 * item is a numbered title-plus-description row, not a titled run of
 * paragraphs.</p>
 *
 * @param heading the section heading
 * @param icon    the icon token of the section badge
 * @param items   the numbered scope rows, in order
 */
public record ProposalScope(String heading, String icon, List<Item> items) {

    /**
     * Normalizes optional fields and freezes the item list.
     */
    public ProposalScope {
        heading = Objects.requireNonNullElse(heading, "");
        icon = Objects.requireNonNullElse(icon, "");
        items = List.copyOf(Objects.requireNonNullElse(items, List.of()));
    }

    /**
     * One numbered scope row.
     *
     * @param number      the step number, as authored (e.g. {@code "01"})
     * @param title       the step title
     * @param description the step description
     */
    public record Item(String number, String title, String description) {

        /**
         * Normalizes optional fields to empty strings.
         */
        public Item {
            number = Objects.requireNonNullElse(number, "");
            title = Objects.requireNonNullElse(title, "");
            description = Objects.requireNonNullElse(description, "");
        }
    }
}
