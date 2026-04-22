package com.demcha.compose.document.templates.data.invoice;

import java.util.List;
import java.util.Objects;

/**
 * Display-oriented invoice party details.
 */
public record InvoiceParty(
        String name,
        List<String> addressLines,
        String email,
        String phone,
        String taxId) {

    public InvoiceParty {
        name = Objects.requireNonNullElse(name, "");
        addressLines = List.copyOf(Objects.requireNonNullElse(addressLines, List.of()));
        email = Objects.requireNonNullElse(email, "");
        phone = Objects.requireNonNullElse(phone, "");
        taxId = Objects.requireNonNullElse(taxId, "");
    }
}
