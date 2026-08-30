package com.demcha.compose.document.templates.data.proposal;

import java.util.List;
import java.util.Objects;

/**
 * The signing card that closes a structured proposal: an icon-badged
 * heading, the acceptance statement, and the labelled signature fields.
 *
 * @param heading   the section heading
 * @param icon      the icon token of the section badge
 * @param statement the acceptance statement paragraph
 * @param fields    the signature field labels, in order (e.g. name, date,
 *                  signature)
 */
public record ProposalAcceptance(
        String heading,
        String icon,
        String statement,
        List<String> fields) {

    /**
     * Normalizes optional fields and freezes the field list.
     */
    public ProposalAcceptance {
        heading = Objects.requireNonNullElse(heading, "");
        icon = Objects.requireNonNullElse(icon, "");
        statement = Objects.requireNonNullElse(statement, "");
        fields = List.copyOf(Objects.requireNonNullElse(fields, List.of()));
    }
}
