package com.demcha.compose.document.templates.data.proposal;

import java.util.List;
import java.util.Objects;

/**
 * The project-goals band of a structured proposal: an icon-badged heading
 * and a horizontal run of icon-beside-text goal cells.
 *
 * @param heading the section heading
 * @param icon    the icon token of the section badge
 * @param items   the goal cells, in order
 */
public record ProposalGoals(String heading, String icon, List<Goal> items) {

    /**
     * Normalizes optional fields and freezes the goal list.
     */
    public ProposalGoals {
        heading = Objects.requireNonNullElse(heading, "");
        icon = Objects.requireNonNullElse(icon, "");
        items = List.copyOf(Objects.requireNonNullElse(items, List.of()));
    }

    /**
     * One goal cell.
     *
     * @param icon the icon token of the goal
     * @param text the goal statement
     */
    public record Goal(String icon, String text) {

        /**
         * Normalizes optional fields to empty strings.
         */
        public Goal {
            icon = Objects.requireNonNullElse(icon, "");
            text = Objects.requireNonNullElse(text, "");
        }
    }
}
