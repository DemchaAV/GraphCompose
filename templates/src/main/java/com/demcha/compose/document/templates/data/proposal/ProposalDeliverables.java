package com.demcha.compose.document.templates.data.proposal;

import java.util.List;
import java.util.Objects;

/**
 * The deliverables band of a structured proposal: an icon-badged heading and
 * two authored bullet columns.
 *
 * <p>The left/right split is authored rather than computed so it can be
 * tuned per proposal — moving one long bullet between columns is a data
 * revision, not a layout change.</p>
 *
 * @param heading     the section heading
 * @param icon        the icon token of the section badge
 * @param leftColumn  the bullets of the left column, in order
 * @param rightColumn the bullets of the right column, in order
 */
public record ProposalDeliverables(
        String heading,
        String icon,
        List<String> leftColumn,
        List<String> rightColumn) {

    /**
     * Normalizes optional fields and freezes both columns.
     */
    public ProposalDeliverables {
        heading = Objects.requireNonNullElse(heading, "");
        icon = Objects.requireNonNullElse(icon, "");
        leftColumn = List.copyOf(Objects.requireNonNullElse(leftColumn, List.of()));
        rightColumn = List.copyOf(Objects.requireNonNullElse(rightColumn, List.of()));
    }
}
