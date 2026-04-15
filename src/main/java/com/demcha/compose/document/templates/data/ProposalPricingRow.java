package com.demcha.compose.document.templates.data;

import java.util.Objects;

/**
 * Display-oriented proposal pricing row.
 */
public record ProposalPricingRow(
        String label,
        String description,
        String amount,
        boolean emphasized) {

    public ProposalPricingRow {
        label = Objects.requireNonNullElse(label, "");
        description = Objects.requireNonNullElse(description, "");
        amount = Objects.requireNonNullElse(amount, "");
    }
}
