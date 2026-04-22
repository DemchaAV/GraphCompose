package com.demcha.compose.document.templates.data.invoice;

import java.util.Objects;

/**
 * Display-oriented invoice line item.
 */
public record InvoiceLineItem(
        String description,
        String details,
        String quantity,
        String unitPrice,
        String amount) {

    public InvoiceLineItem {
        description = Objects.requireNonNullElse(description, "");
        details = Objects.requireNonNullElse(details, "");
        quantity = Objects.requireNonNullElse(quantity, "");
        unitPrice = Objects.requireNonNullElse(unitPrice, "");
        amount = Objects.requireNonNullElse(amount, "");
    }
}
