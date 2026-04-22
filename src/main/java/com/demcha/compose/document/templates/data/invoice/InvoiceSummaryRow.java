package com.demcha.compose.document.templates.data.invoice;

import java.util.Objects;

/**
 * Display-oriented invoice summary row.
 */
public record InvoiceSummaryRow(
        String label,
        String value,
        boolean emphasized) {

    public InvoiceSummaryRow {
        label = Objects.requireNonNullElse(label, "");
        value = Objects.requireNonNullElse(value, "");
    }
}
