package com.demcha.compose.document.templates.data.proposal;

import java.util.List;
import java.util.Objects;

/**
 * The at-a-glance fact card of a structured proposal.
 *
 * @param heading the card heading
 * @param facts   the fact rows, in order
 */
public record ProposalGlance(String heading, List<Fact> facts) {

    /**
     * Normalizes optional fields and freezes the fact list.
     */
    public ProposalGlance {
        heading = Objects.requireNonNullElse(heading, "");
        facts = List.copyOf(Objects.requireNonNullElse(facts, List.of()));
    }

    /**
     * One fact row: an icon, a quiet label, the value line, and an optional
     * parenthetical note.
     *
     * @param icon  the icon token of the fact
     * @param label the quiet label above the value
     * @param value the emphasized value line
     * @param note  the optional second value line; empty when the row has none
     */
    public record Fact(String icon, String label, String value, String note) {

        /**
         * Normalizes optional fields to empty strings.
         */
        public Fact {
            icon = Objects.requireNonNullElse(icon, "");
            label = Objects.requireNonNullElse(label, "");
            value = Objects.requireNonNullElse(value, "");
            note = Objects.requireNonNullElse(note, "");
        }
    }
}
