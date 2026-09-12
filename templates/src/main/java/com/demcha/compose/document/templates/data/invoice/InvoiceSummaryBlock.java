package com.demcha.compose.document.templates.data.invoice;

import java.util.Objects;

/**
 * The summary block of a structured invoice: what the invoice covers and
 * the period it covers it for.
 *
 * @param heading       the block heading (e.g. {@code "INVOICE SUMMARY"})
 * @param intro         the prose sentence introducing the work billed
 * @param servicePeriod the billed period, set apart from the intro
 */
public record InvoiceSummaryBlock(String heading, String intro, String servicePeriod) {

    /**
     * Normalizes optional fields to empty strings.
     */
    public InvoiceSummaryBlock {
        heading = Objects.requireNonNullElse(heading, "");
        intro = Objects.requireNonNullElse(intro, "");
        servicePeriod = Objects.requireNonNullElse(servicePeriod, "");
    }
}
