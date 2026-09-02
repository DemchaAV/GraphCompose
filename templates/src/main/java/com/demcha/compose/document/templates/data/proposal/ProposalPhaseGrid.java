package com.demcha.compose.document.templates.data.proposal;

import java.util.List;
import java.util.Objects;

/**
 * The phase grid of a structured proposal: an icon-badged heading, the
 * authored column headers, and one row per phase.
 *
 * <p>Distinct from the narrative model's {@link ProposalTimelineItem}: a
 * phase carries a number, a focus column and an output column, and the grid
 * owns its header labels — the narrative timeline is a phase/duration/details
 * triple with preset-owned headers.</p>
 *
 * <p>Unlike {@link ProposalInvestment}, whose two columns are fixed by the
 * concept and therefore named fields, the phase grid's column set belongs
 * to the preset that draws it — so the headers stay an authored list in
 * column order. A preset validates that the list carries one label per
 * column it renders; the data layer does not fix the count.</p>
 *
 * @param heading       the section heading
 * @param icon          the icon token of the section badge
 * @param columnHeaders the grid's header labels, one per rendered column,
 *                      in column order
 * @param phases        the phase rows, in order
 */
public record ProposalPhaseGrid(
        String heading,
        String icon,
        List<String> columnHeaders,
        List<Phase> phases) {

    /**
     * Normalizes optional fields and freezes both lists.
     */
    public ProposalPhaseGrid {
        heading = Objects.requireNonNullElse(heading, "");
        icon = Objects.requireNonNullElse(icon, "");
        columnHeaders = List.copyOf(Objects.requireNonNullElse(columnHeaders, List.of()));
        phases = List.copyOf(Objects.requireNonNullElse(phases, List.of()));
    }

    /**
     * One phase row of the grid.
     *
     * @param number   the phase number, as authored (e.g. {@code "1"})
     * @param name     the phase name
     * @param focus    what the phase concentrates on
     * @param duration the phase duration text
     * @param output   what the phase delivers
     */
    public record Phase(
            String number,
            String name,
            String focus,
            String duration,
            String output) {

        /**
         * Normalizes optional fields to empty strings.
         */
        public Phase {
            number = Objects.requireNonNullElse(number, "");
            name = Objects.requireNonNullElse(name, "");
            focus = Objects.requireNonNullElse(focus, "");
            duration = Objects.requireNonNullElse(duration, "");
            output = Objects.requireNonNullElse(output, "");
        }
    }
}
