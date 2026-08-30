package com.demcha.compose.document.templates.data.proposal;

import java.util.Objects;

/**
 * The prepared-for / prepared-by / date line under a structured proposal's
 * title.
 *
 * @param preparedFor the client the proposal is prepared for
 * @param preparedBy  the sender the proposal is prepared by
 * @param date        the proposal date text
 */
public record ProposalMetaLine(String preparedFor, String preparedBy, String date) {

    /**
     * Normalizes optional fields to empty strings.
     */
    public ProposalMetaLine {
        preparedFor = Objects.requireNonNullElse(preparedFor, "");
        preparedBy = Objects.requireNonNullElse(preparedBy, "");
        date = Objects.requireNonNullElse(date, "");
    }
}
